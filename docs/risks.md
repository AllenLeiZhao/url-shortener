# Risk Register & Guardrails

Risks are grouped by category. Each entry records the failure scenario, the guardrail in
place for the prototype, and the production path where the prototype answer is not the
production answer. "Accepted" means consciously accepted for this scope, not overlooked.

## Security & abuse

| Risk | Scenario | Guardrail (prototype) | Production path |
|------|----------|----------------------|-----------------|
| Open-redirect abuse via dangerous schemes | `javascript:`/`data:`/`file:` URL stored, executes on redirect | Scheme allowlist (http/https), host required, enforced at the service boundary (ADR-004); tested | Same, plus CSP on any UI |
| Redirect to malicious-but-valid destinations | Phishing/malware URL behind a trusted-looking short link | **Accepted** — no destination reputation checking | Reputation feed (e.g. Safe Browsing) at create time + async re-scan of existing links |
| Storage exhaustion via anonymous create API | Bot floods `POST /api/urls` | Per-IP rate limit 20/min (429 + Retry-After); 2048-char URL cap | AuthN/API keys, quota per principal, distributed limiter |
| Rate-limit bypass by IP spoofing headers | Client sets `X-Forwarded-For` | Header deliberately ignored; socket address used (ADR-006) | Behind a trusted proxy: use the proxy-verified client IP only |
| Short-code enumeration | Walking the code space to scrape stored URLs | Random codes over 62⁷ space, `SecureRandom` (ADR-002) — not sequential | Same; plus anomaly detection on 404 rates |
| SQL injection / malformed input | Hostile payloads in URL or code path segments | JPA parameter binding throughout; redirect path constrained to `[0-9A-Za-z]{1,16}` at the route | Same |

## Reliability & data

| Risk | Scenario | Guardrail (prototype) | Production path |
|------|----------|----------------------|-----------------|
| DB unavailable | H2 file locked/corrupted | Clean 503 with structured body (tested); health probe exposes DB state | External HA database; retries/circuit breaker become meaningful (deferred R4) |
| Click events lost | Crash or executor saturation between redirect and async write | **Accepted** — analytics is best-effort by design (ADR-005); bounded queue drops with log | Outbox table or message queue for at-least-once capture |
| Collision-retry exhaustion | Random code collides 5× (probability ~0 until table ≈ code space) | Clean 500 with retry message (tested); monitorable log line | Raise code length / switch strategy well before saturation |
| Unbounded growth: `click_events`, `short_urls` | Disk fills over time | **Accepted** — no retention/expiry (requirements A5) | TTL/archival policy; partitioning for click events |
| Rate-limit state lost on restart | Limiter is in-memory | **Accepted** — one free window after restart | Redis-backed limiter (multi-instance anyway) |
| Schema drift / unsafe DDL | `ddl-auto: update` lets Hibernate mutate the schema implicitly | **Accepted** for prototype speed; entities are the single schema source | Versioned migrations (Flyway/Liquibase) with `ddl-auto: validate` |
| Single instance | Process death = outage | Health probes make it orchestratable (restart/replace) | Horizontal scaling: stateless app + external DB — ADR-002 code strategy already needs no coordination |

## Engineering-process risks (AI-assisted delivery)

| Risk | Guardrail |
|------|-----------|
| AI-generated code accepted without understanding | Every AI output dispositioned in `ai-log.md` (adopted/edited/rejected + why); rejections recorded with reasoning (AI-3, AI-7, AI-9) |
| Plausible-but-wrong AI designs | Design decisions made through ADRs with options and trade-offs, not taken from first AI proposal (ADR-002, ADR-006) |
| Silent scope creep from AI suggestions | Scope normalized in writing before implementation (requirements.md, scenario docs); exclusions recorded with rationale |
| Regression while enhancing (brownfield) | Impact analysis before code change; acceptance criterion "Part-1 tests pass unmodified"; full suite green after each part |

## Known limitations (assignment scope)

- No authentication / multi-tenancy — API is anonymous (A6).
- No link expiry, editing, or deletion (A5).
- No custom aliases (A3) — would require reserved-word filtering and abuse controls.
- H2 file mode is a prototype store, not a production database (ADR-001).
- CI (`.github/workflows/ci.yml`) runs `mvn verify` — build, tests, and format check
  (Spotless). No dependency scanning or deeper static analysis yet; that gap is documented
  in `final-summary.md` §Quality gates rather than hidden.
