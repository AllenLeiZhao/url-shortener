# Scenario 3 — Ambiguous Requirement: "Reliability Features"

*Requirement:* "core APIs, analytics, and **reliability features**" — no definition given.
This scenario demonstrates handling an ambiguous requirement: enumerate interpretations,
decide with explicit criteria, normalize into implementable scope, then build (T11 → T12).

## 1. Interpretation space

For a single-node prototype URL shortener, "reliability" could plausibly mean:

| # | Candidate | Value here | Cost | Verdict |
|---|-----------|-----------|------|---------|
| R1 | Health probes (liveness/readiness) | High — prerequisite for any orchestrated deployment (k8s, LB) | Trivial (Actuator) | ✅ in scope |
| R2 | Rate limiting on the write path | High — an anonymous create API is a storage-exhaustion / abuse vector; protecting it protects availability | Low (in-memory limiter) | ✅ in scope |
| R3 | Graceful degradation on storage failure | High — DB is the single dependency; a clean 503 beats a stack-trace 500 | Low (error mapping) | ✅ in scope |
| R4 | Retries / circuit breaker around DB | Low — embedded H2 in-process; no network partition to survive | Medium (+resilience4j dep) | ❌ deferred; becomes relevant when storage is external |
| R5 | Distributed rate limiting / HA clustering | None at prototype scale | High | ❌ out of scope, noted as production path |
| R6 | Idempotent create (dedupe same URL) | Product choice, not reliability | Low | ❌ tracked separately (ADR-002 trade-off) |

**Decision rule applied:** include what protects availability of the running prototype and
what any deployment platform requires; defer what only pays off with infrastructure this
prototype doesn't have. Each exclusion is recorded, not silently dropped.

## 2. Normalized scope & acceptance criteria

1. **Health probes** — `/actuator/health` (with liveness/readiness groups) returns 200 when
   the service and DB are up; DB health is included in readiness.
2. **Rate limiting** — `POST /api/urls` limited per client IP (default 20/min, configurable);
   over-limit → `429` with the standard error body and `Retry-After` header. Read/redirect
   paths are NOT limited (protecting writes must not break shared links).
3. **Graceful degradation** — storage-layer failures surface as `503` with the structured
   error body, not a 500 with internals; collision-retry exhaustion already returns a clean
   error (Part 1).

## 3. Execution notes

- Rate limiter: fixed-window counter per IP in a `ConcurrentHashMap`, windows pruned on
  access. AI proposed adding the bucket4j dependency; rejected — one more dependency and a
  token-bucket's smoothness buys little at prototype scale, while a ~60-line filter is fully
  testable and transparent (ai-log AI-9). Limitation: per-instance state, resets on restart;
  a multi-instance deployment would move this to Redis — recorded in risks.
- `X-Forwarded-For` is ignored entirely; the socket address (`remoteAddr`) is used — the
  prototype sits behind no trusted proxy, and trusting the header unauthenticated would let
  clients spoof their way around the limit.

## 4. Validation

- Integration tests: over-limit create → 429 + `Retry-After`; under-limit unaffected;
  redirects unaffected by create-limit exhaustion; `/actuator/health` → 200/UP.
- Manual transcript incl. 429 demonstration: `docs/evidence/part2-api-demo.log`.
