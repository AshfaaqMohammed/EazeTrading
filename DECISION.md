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