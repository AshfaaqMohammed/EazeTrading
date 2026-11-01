# EazeTrading — Engineering Decisions Log

A running record of the production-hardening decisions made while scaling
EazeTrading from a working starter into a system that is safe under
concurrency and fit for horizontal scale. Each entry captures the problem,
the option chosen, the alternatives rejected, and the behavior under
load/failure — written to be usable as interview talking points.
  
---

## Task 1 — Foundation: Flyway migrations + externalized secrets

### Problem
- Schema was managed by `spring.jpa.hibernate.ddl-auto=update`. Hibernate
  silently altered the live schema on every boot: no version history, no
  review, no rollback, and drift between environments. `update` also never
  drops/《fixes》 columns, so schemas quietly diverge over time.
- Secrets (mail password, Razorpay key/secret, CoinGecko key, JWT secret)
  were hardcoded in `application.properties` and committed to git.

### Decision
- Introduced **Flyway** as the single source of truth for schema. Captured the
  existing schema as `V1__baseline.sql` and switched Hibernate to
  `ddl-auto=validate` (checks entity/schema agreement on boot, never mutates).
- Used `baseline-on-migrate=true` + `baseline-version=1` so Flyway **adopts**
  the pre-existing populated database at version 1 instead of trying to build
  it from scratch (which would collide with existing tables). On a fresh DB
  (e.g. Testcontainers), there is nothing to baseline, so `V1` runs in full.
- Externalized all config via `${ENV}` placeholders (12-factor). Real secrets
  have **no committed fallback** — the app fails fast if they are missing.
  Low-risk local config (DB host/port, mail host, CoinGecko URLs, dev JWT key)
  keeps dev-only fallbacks for a frictionless local run. Local secrets live in
  a gitignored `.env` (loaded via `spring-dotenv`); `.env.example` documents
  the required vars.

### Alternatives considered / rejected
- **Liquibase** instead of Flyway: equally valid; Flyway chosen for plain-SQL
  migrations (closer to the SQL you'd run by hand — better for learning) and
  simpler mental model. Liquibase's XML/YAML changelog abstraction wasn't worth
  it here.
- **Keep `ddl-auto=update`**: rejected — unreviewed, unversioned schema changes
  are unacceptable for a money system and make multi-environment deploys unsafe.
- **`ddl-auto=none`** vs `validate`: chose `validate` so entity/schema drift is
  caught loudly at startup rather than surfacing as runtime SQL errors.
- **Regenerate baseline from entities** instead of dumping the live DB:
  rejected in favor of a faithful `mysqldump --no-data` so the baseline exactly
  matches production reality (including the Hibernate `*_seq` id tables).

### Behavior / consequences
- First boot against the existing DB: Flyway writes `flyway_schema_history`,
  records a baseline at v1, runs nothing destructive; live data untouched.
- Missing a required secret now fails fast at startup (intended) rather than
  silently running with a committed credential.
- Note: id generation uses Hibernate **table sequences** (`*_seq`) because
  entities use `@GeneratedValue(AUTO)`. These tables are part of the baseline.

### Follow-ups / debt
- **Rotate the previously-committed secrets** (Gmail app password, Razorpay test
  keys, CoinGecko key) — they remain in git history. Low risk (test/demo creds)
  but must be rotated before any public exposure.

### Interview talking points
- Migrations vs `ddl-auto`: why schema-as-code (versioned, reviewed, reversible)
  matters, especially for money systems.
- The "adopt an existing DB" problem and how `baseline-on-migrate` solves it.
- 12-factor config: config in the environment, fail-fast on missing secrets,
  secrets out of source control, and why old git history still needs rotation.
- `validate` vs `update` vs `none` and what each does at startup.


## Task 3 — Concurrency control (pessimistic wallet lock + optimistic asset @Version + CHECK backstop)

### Problem
Wallet balance updates are read-modify-write. Under concurrency, N simultaneous
buys/transfers/withdrawals each read the same balance, all pass the funds check, and
all debit — a LOST UPDATE that lets a user overdraw / double-spend. Atomicity
(@Transactional) does NOT prevent this; at READ_COMMITTED both writers may read the
same committed value and both write.

### Decision (layered)
- PESSIMISTIC lock (SELECT ... FOR UPDATE) on the hot, contended WALLET row for every
  debit path (buy, transfer, withdrawal). Correctness > throughput on money; serialize
  rather than retry-storm.
- OPTIMISTIC lock (@Version) on the lower-contention ASSET row; rare conflicts handled
  by a bounded retry loop (OrderServiceImpl loop -> OrderTransactionalService.processOnce
  through the proxy = fresh tx per attempt).
- DB CHECK (balance >= 0) as a backstop that cannot be bypassed by app code (V2).
- Transfers lock BOTH wallets in ascending-id order to remove the circular-wait
  condition -> no deadlock.

### The bug that cost us (IMPORTANT interview point)
The pessimistic lock silently did NOTHING at first: a prior UNLOCKED read
(getUserWallet) loaded the Wallet into Hibernate's first-level cache (persistence
context). The subsequent findByUserIdForUpdate() returned the CACHED instance and
never issued SELECT ... FOR UPDATE. Result: 10/10 concurrent buys "succeeded",
balance logic saw 100-100=0 each time (never negative, so CHECK never tripped),
money created.
FIX: make the LOCKED read the FIRST access to the entity in the transaction — do not
read it unlocked beforehand. Lesson: an unlocked read can poison a later lock via the
first-level cache.

### Also learned
- @Lock cannot be combined with a native @Query ("Illegal attempt to set lock mode for
  a native query") — use JPQL with @Lock, or set the lock another way.
- Pessimistic locking + high concurrency is capped by the connection pool: each
  lock-holding tx ties up a connection for its whole duration. 50 threads vs a 10-conn
  Hikari pool => connection-acquisition timeouts, not a locking failure. Right-size the
  pool and/or bound concurrency (rate limiting/queueing — later tasks).

### Proof
Testcontainers was blocked (Docker Hub auth), so we used a local eaze_test MySQL
(real MySQL 8 — same locking semantics). Integration test OrderConcurrencyIT fires
10 simultaneous buys at a wallet funded for exactly one; asserts exactly 1 success,
9 failures, final balance 0.00, never negative. FAILS without the lock, PASSES with it.

## Task 4 — Idempotency framework (Redis-backed, client-generated key)

### Problem
Non-idempotent writes (buy/sell, transfer, withdrawal, deposit) can execute twice:
- Double-click fires two requests for one intent.
- Network retry: the server processes the request fully (charges the wallet) but the
  RESPONSE is lost; the client cannot distinguish "never arrived" from "succeeded but
  response lost", so it correctly retries — and the server double-charges.
The client cannot avoid retries; therefore the SERVER must absorb duplicates and execute
the side effect exactly once.

### Decision (Design A — client-generated idempotency key, Stripe-style)
- Client sends an `Idempotency-Key` header (one key per submit, REUSED on every retry of
  that submit; a NEW submit uses a NEW key). Server dedupes by that key.
- State stored in REDIS (not in-memory) so it works across horizontally-scaled instances.
  The atomic claim is Redis `SET key val NX PX <ttl>` (setIfAbsent) — a distributed
  test-and-set, the Redis equivalent of `SELECT ... FOR UPDATE`. Among N concurrent
  duplicates exactly one wins the claim.
- Three-way handling per key: DONE -> replay the stored response (header
  `Idempotent-Replayed: true`), IN_PROGRESS -> 409, absent -> claim + process.
- Implemented as a cross-cutting `OncePerRequestFilter` (IdempotencyFilter) guarding the 4
  money endpoints via an allow-list — controllers stay untouched (same pattern as the JWT
  filter). Captures the response via ContentCachingResponseWrapper to store it for replay.
- Caches only 2xx; `release()` (deletes the key) on error/exception so a transient failure
  doesn't permanently block the client's legitimate retry.
- Key scoped by `email:method:path:key` so keys never collide across users/operations.
- LENIENT: a request without the header is processed normally (opt-in). The existing
  frontend is unaffected; it adopts keys later.
- TTL 24h (covers realistic retry windows; Redis auto-expires so the store stays bounded).

### Design A vs Design B (interview point)
- Design A (client key): client decides what is a duplicate. REQUIRED for money ops,
  because two IDENTICAL requests can be two legitimate separate intents (two real $3 buys)
  — a content hash would wrongly collapse them. Used by Stripe/PayPal.
- Design B (content hash / natural key): server derives the key from the request content.
  Right for "process this exact thing once" (webhooks/events with a natural id, ingestion).
  Would wrongly drop a legitimate repeat, so it is unsafe for "money out" actions.
- This app uses BOTH where each fits: Design A as the general request-dedupe layer, and a
  natural key (deposit `payment_id` + PENDING status guard) where the data has a unique id.

### Relationship to Task 2/3
Idempotency stops duplicate REQUESTS; the Task 2/3 transaction + locks + status guards stop
unsafe concurrent/replayed STATE TRANSITIONS at the data layer. Defense in depth — both matter.

### Proof
Manual: same `Idempotency-Key` sent twice for a buy -> first executes (200, order created,
wallet debited once); second returns the SAME body + `Idempotent-Replayed: true` and does
NOT re-run the controller. DB confirmed exactly one order row, one BUY_ASSET ledger row, and
balance dropped by exactly one buy. Redis holds `idemp:...:<key> = {state:DONE,...}`.
(Automated Redis-backed IT deferred.)

### Interview talking points
- Lost-response retries are inevitable and correct on the client; servers must be idempotent.
- `SET NX` = atomic claim = distributed check-and-set; why it must be Redis for multi-instance.
- Client-key vs content-hash idempotency and when each is correct.
- Idempotency as a cross-cutting concern (filter), not per-endpoint code.
- Releasing the key on failure so transient errors don't poison future retries.


## Load testing
## BASELINE LOAD TEST — after Task 3 (raw app), local "one server"

Host: Intel Core Ultra 5 135H, 18 logical CPUs, ~16GB, Windows 11 + WSL2.
App + MySQL + k6 all on the same box. Main-app Hikari pool = 10 (default).
Tool: k6, script loadtest/baseline.js. Workload: ramping 0->200 VUs over 4m,
mixed 70% reads (GET /coins/{id}, GET /api/wallet) + 30% writes (POST /api/orders/pay buy).

Results:
- Throughput: ~40 req/s sustained (http_reqs 39.86/s).
- Latency (ALL requests): p50 5.5ms, p90 88ms, p95 24.6s, max 65s.
- Latency (SUCCESSFUL only): p50 5.3ms, p90 53ms, p95 92ms.  <-- successes stayed fast
- read_latency:  p50 3.98ms, p90 9.83ms, p95 20.98s, max 31.8s.
- write_latency: p50 20ms,  p90 117ms,  p95 27.57s, max 65s.
- Errors: http_req_failed 4.64%; buy_errors 4.99%. Buys timed out first (/api/orders/pay).

Interpretation: fast up to ~p90, then a CLIFF -> classic resource saturation. Bottleneck =
10-connection Hikari pool + wallet row-lock contention on the write path; excess requests
queue tens of seconds and time out. Reads starve too (shared pool). Network not a factor.

This is the reference point. Re-run the SAME script after each task to quantify gains.
Expected movers: Task 5 Redis cache (reads bypass DB/pool), pool sizing / Task 9 queue
(write path), rate limiting (reject fast instead of 25s queue -> graceful degradation).

## Pool-sizing study (after Task 3 baseline) — measured, not guessed

Box: ~18 logical cores, app+MySQL+k6 on one WSL2 host. Same k6 mixed test each run.

| Pool | Throughput | overall p95 | read p95 | write p95 | p99   | errors |
  |------|-----------|-------------|----------|-----------|-------|--------|
| 10   | 40 req/s  | 24.6 s      | 20.98 s  | 27.57 s   | 30.6s | 4.64%  |
| 20   | 86 req/s  | 272 ms      | 7.87 ms  | 320 ms    | 30.4s | 1.38%  |
| 50   | 98 req/s  | 443 ms      | 8.65 ms  | 623 ms    | 30.3s | 0.98%  |

Findings:
- Pool 10 was STARVED on an 18-core box: p95 24s. Raising to 20 cut p95 to 272ms and
  doubled throughput. Instinct "pool too small" was correct.
- 20 -> 50: throughput only +14% but latency got WORSE (p95 272->443ms, write p95
  320->623ms). Diminishing returns + inversion: connections beyond what cores/DB can
  service add context-switching + in-DB queuing, not parallelism. Bigger != better.
- p99 pinned at ~30s regardless of pool size: the pool is NOT the tail bottleneck —
  the wallet-row pessimistic lock (SELECT ... FOR UPDATE) serializes concurrent buys on
  the SAME wallet. (Worst-case in test: all buys hit one wallet; real traffic spreads
  across wallets.) Right fix for the tail = offload reads (cache) + queue/rate-limit writes.
- DECISION: main-app pool = 20 (sweet spot for this box; << MySQL max_connections 151).

Interview line: "optimal pool ≈ cores×~1.5; I measured the curve bend and identified the
next bottleneck (lock serialization) once connections stopped being the limit."

