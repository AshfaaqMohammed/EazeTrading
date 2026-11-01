// loadtest/baseline.js
// EazeTrading baseline load test (k6).
// Mixed workload: ~70% reads, ~30% writes (buy), against the local app treated as "one server".
// Re-run this UNCHANGED after each hardening task to quantify the improvement.
//
// Run:  k6 run loadtest/baseline.js
// Override base URL:  k6 run -e BASE_URL=http://localhost:5455 loadtest/baseline.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:5455';

// Test user (must exist, no 2FA, wallet funded).
const EMAIL = __ENV.EMAIL || 'loadtest@gmail.com';
const PASSWORD = __ENV.PASSWORD || 'test1234';

// A coinId that exists in the `coin` table (DB read + buy payload).
const COIN_ID = __ENV.COIN_ID || 'aster-2';
const BUY_QTY = __ENV.BUY_QTY || 0.001;   // tiny, so the wallet never drains

// Custom metrics so we see read vs write latency separately.
const readLatency = new Trend('read_latency', true);
const writeLatency = new Trend('write_latency', true);
const buyErrors = new Rate('buy_errors');

// Ramping profile: find the "knee" where latency/errors spike.
export const options = {
    scenarios: {
        mixed: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },   // warm up
                { duration: '1m',  target: 50 },
                { duration: '1m',  target: 100 },
                { duration: '1m',  target: 200 },   // push toward the ceiling
                { duration: '30s', target: 0 },     // ramp down
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        // These are observation markers, not hard pass/fail — tune as you learn the app.
        http_req_duration: ['p(95)<2000', 'p(99)<5000'],
        buy_errors: ['rate<0.5'],
    },
};

// Log in ONCE per VU at startup, reuse the JWT for that VU's iterations.
export function setup() {
    const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({ email: EMAIL, password: PASSWORD }), {
        headers: { 'Content-Type': 'application/json' },
    });
    check(res, { 'login 200': (r) => r.status === 200 });
    const jwt = res.json('jwt');
    if (!jwt) {
        throw new Error(`Login failed, no jwt. status=${res.status} body=${res.body}`);
    }
    return { jwt };
}

export default function (data) {
    const authHeaders = {
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${data.jwt}` },
    };

    // Weighted mix: 0-69 => reads (70%), 70-99 => write (30%).
    const roll = Math.floor(Math.random() * 100);

    if (roll < 40) {
        // READ 1: public DB read of a coin (no auth needed, but harmless to send header).
        const r = http.get(`${BASE_URL}/coins/${COIN_ID}`, authHeaders);
        readLatency.add(r.timings.duration);
        check(r, { 'coin read ok': (res) => res.status === 200 || res.status === 202 });

    } else if (roll < 70) {
        // READ 2: authenticated wallet read (DB read).
        const r = http.get(`${BASE_URL}/api/wallet`, authHeaders);
        readLatency.add(r.timings.duration);
        check(r, { 'wallet read ok': (res) => res.status === 200 || res.status === 202 });

    } else {
        // WRITE: buy — the locked, transactional path.
        const payload = JSON.stringify({ coinId: COIN_ID, quantity: BUY_QTY, orderType: 'BUY' });
        const r = http.post(`${BASE_URL}/api/orders/pay`, payload, authHeaders);
        writeLatency.add(r.timings.duration);
        const ok = check(r, { 'buy ok': (res) => res.status === 200 });
        buyErrors.add(!ok);
    }

    sleep(0.1); // small think-time so VUs don't spin at 100% CPU unrealistically
}