/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  PERFORMANCE / LOAD TESTS — k6                                     │
 * │                                                                     │
 * │  Tool: k6 (https://k6.io) — free, open-source load testing tool.  │
 * │                                                                     │
 * │  HOW TO INSTALL k6:                                                │
 * │  • Windows:  winget install k6 --source winget                     │
 * │  • macOS:    brew install k6                                        │
 * │  • Linux:    sudo snap install k6                                   │
 * │  • Docker:   docker run -i grafana/k6 run - < k6-load-test.js     │
 * │                                                                     │
 * │  HOW TO RUN (against your Render backend):                         │
 * │  k6 run k6-load-test.js                                            │
 * │                                                                     │
 * │  HOW TO RUN (against local dev server):                            │
 * │  BASE_URL=http://localhost:9090 k6 run k6-load-test.js            │
 * │                                                                     │
 * │  TEST STAGES:                                                       │
 * │  • Ramp up   : 0 → 10 VUs over 30 seconds                         │
 * │  • Sustained : 10 VUs for 1 minute (normal load)                  │
 * │  • Spike     : 10 → 50 VUs over 30 seconds (peak load)            │
 * │  • Recover   : 50 → 5 VUs over 30 seconds                         │
 * │  • Cool down : 5 VUs for 30 seconds                                │
 * │                                                                     │
 * │  THRESHOLDS (SLA):                                                  │
 * │  • 95% of all requests complete within 2 seconds                   │
 * │  • 99% of login requests complete within 3 seconds (BCrypt)        │
 * │  • Error rate < 1%                                                  │
 * └─────────────────────────────────────────────────────────────────────┘
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── Custom metrics ────────────────────────────────────────────────────────────
const loginDuration   = new Trend('login_duration_ms',   true);
const projectsDuration = new Trend('projects_duration_ms', true);
const errorRate       = new Rate('error_rate');

// ── Configuration ─────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'https://spms-backend-hbad.onrender.com';

// ── Test stages (ramping VUs) ─────────────────────────────────────────────────
export const options = {
    stages: [
        { duration: '30s', target: 10  },  // ramp-up to 10 users
        { duration: '60s', target: 10  },  // stay at 10 users (normal load)
        { duration: '30s', target: 50  },  // spike to 50 users
        { duration: '30s', target: 5   },  // recover
        { duration: '30s', target: 5   },  // cool-down
    ],
    thresholds: {
        // 95th percentile of ALL requests must be under 2 seconds
        'http_req_duration':    ['p(95)<2000'],
        // login specifically: BCrypt is slow, allow 3 seconds
        'login_duration_ms':    ['p(99)<3000'],
        // project listing should be fast
        'projects_duration_ms': ['p(95)<1500'],
        // Less than 1% of requests should fail
        'error_rate':           ['rate<0.01'],
        // HTTP request failures < 1%
        'http_req_failed':      ['rate<0.01'],
    },
};

// ── JSON helper ───────────────────────────────────────────────────────────────
const jsonHeaders = { 'Content-Type': 'application/json' };

function post(path, body) {
    return http.post(`${BASE_URL}${path}`, JSON.stringify(body), { headers: jsonHeaders });
}

function get(path) {
    return http.get(`${BASE_URL}${path}`);
}

// ── Test users (must exist in your database; use your seed data credentials) ──
const TEST_USERS = [
    { username: 'preethi',  password: 'password', role: 'employee' },
    { username: 'manager',  password: 'password', role: 'manager'  },
    { username: 'hr',       password: 'password', role: 'hr'       },
    { username: 'admin',    password: 'password', role: 'admin'    },
];

// ── Main virtual user scenario ────────────────────────────────────────────────
export default function () {
    // Pick a random test user for this VU iteration
    const user = TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO A: Login
    // ══════════════════════════════════════════════════════════════════════════
    let userId = null;

    group('Login', () => {
        const res = post('/api/auth/login', {
            username: user.username,
            password: user.password,
            role: user.role,
        });

        loginDuration.add(res.timings.duration);
        errorRate.add(res.status !== 200);

        const ok = check(res, {
            '✅ Login status 200':        r => r.status === 200,
            '✅ Login success=true':      r => JSON.parse(r.body).success === true,
            '✅ Login returns user data': r => JSON.parse(r.body).data !== null,
            '✅ Login under 3 seconds':   r => r.timings.duration < 3000,
        });

        if (ok) {
            try {
                const body = JSON.parse(res.body);
                userId = body.data?.id;
            } catch (_) {}
        }
    });

    sleep(0.5); // simulate brief "page load" think time

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO B: List all projects
    // ══════════════════════════════════════════════════════════════════════════
    group('Browse Projects', () => {
        const res = get('/api/projects');

        projectsDuration.add(res.timings.duration);
        errorRate.add(res.status !== 200);

        check(res, {
            '✅ Projects status 200':       r => r.status === 200,
            '✅ Projects returns array':    r => {
                try { return Array.isArray(JSON.parse(r.body).data); }
                catch (_) { return false; }
            },
            '✅ Projects under 2 seconds':  r => r.timings.duration < 2000,
        });
    });

    sleep(0.3);

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO C: Fetch user profile (if login succeeded)
    // ══════════════════════════════════════════════════════════════════════════
    if (userId) {
        group('User Profile', () => {
            const res = get(`/api/users/${userId}`);

            errorRate.add(res.status !== 200);

            check(res, {
                '✅ User profile status 200':     r => r.status === 200,
                '✅ User profile has user obj':   r => {
                    try { return JSON.parse(r.body).data?.user !== undefined; }
                    catch (_) { return false; }
                },
                '✅ Profile under 1.5 seconds':   r => r.timings.duration < 1500,
            });
        });
    }

    sleep(0.3);

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO D: Fetch notifications
    // ══════════════════════════════════════════════════════════════════════════
    if (userId) {
        group('Notifications', () => {
            const res = get(`/api/notifications?userId=${userId}`);

            errorRate.add(res.status !== 200);

            check(res, {
                '✅ Notifications status 200':    r => r.status === 200,
                '✅ Has notifications array':     r => {
                    try { return Array.isArray(JSON.parse(r.body).data?.notifications); }
                    catch (_) { return false; }
                },
            });
        });
    }

    sleep(0.3);

    // ══════════════════════════════════════════════════════════════════════════
    // SCENARIO E: Dashboard stats
    // ══════════════════════════════════════════════════════════════════════════
    if (userId) {
        group('Dashboard Stats', () => {
            const res = get(`/api/dashboard/stats?userId=${userId}&role=${user.role}`);

            errorRate.add(res.status !== 200);

            check(res, {
                '✅ Dashboard stats 200':  r => r.status === 200,
                '✅ Stats under 2 seconds': r => r.timings.duration < 2000,
            });
        });
    }

    sleep(0.5); // user "reads" the page
}

// ── Summary handler — printed after test finishes ────────────────────────────
export function handleSummary(data) {
    const passed = data.metrics['http_req_failed']?.values?.rate < 0.01;
    const p95    = data.metrics['http_req_duration']?.values['p(95)'];
    const p99Login = data.metrics['login_duration_ms']?.values['p(99)'];
    const errRate  = (data.metrics['error_rate']?.values?.rate * 100).toFixed(2);

    return {
        stdout: `
╔══════════════════════════════════════════════════════╗
║           SPMS LOAD TEST SUMMARY                     ║
╠══════════════════════════════════════════════════════╣
║  Overall result:  ${passed ? '✅ PASSED' : '❌ FAILED'}                          ║
║  p(95) all req:   ${p95?.toFixed(0)} ms (threshold: 2000 ms)          ║
║  p(99) login:     ${p99Login?.toFixed(0)} ms (threshold: 3000 ms)          ║
║  Error rate:      ${errRate}% (threshold: 1%)                  ║
╚══════════════════════════════════════════════════════╝
`,
    };
}
