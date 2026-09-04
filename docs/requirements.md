# Requirement Understanding

## 1. Stated requirement (verbatim from assignment)

> Build a URL shortener service from scratch with core APIs, analytics, and reliability features. Complete and improve it over 2–3 days using AI assistance while demonstrating engineering judgment.

## 2. Interpreted intent

The deliverable is not just a URL shortener — it is a demonstration of **engineer-led,
AI-accelerated software delivery**. The service is the vehicle; the evaluated substance is
requirement analysis, decomposition, disciplined AI usage, validation, and risk control.

Functionally, the service must:

1. Accept a long URL and return a short code / short URL (core API).
2. Redirect visitors of the short URL to the original URL (core API).
3. Record and expose usage analytics (scope intentionally deferred — see ambiguities).
4. Provide "reliability features" (scope intentionally deferred — see ambiguities).

## 3. Identified ambiguities and how they are resolved

| # | Ambiguity | Options | Resolution / assumption |
|---|-----------|---------|-------------------------|
| A1 | "Analytics" — what granularity? | total click count / per-click event log / referrer + user-agent breakdown | Treated as the **brownfield scenario** (Part 2): start with per-click event capture so aggregates can be derived; no PII beyond coarse metadata. |
| A2 | "Reliability features" — undefined | rate limiting, health checks, graceful degradation, retries, idempotency | Treated as the **ambiguous-requirement scenario** (Part 2/3): ambiguity is analyzed and normalized in `docs/scenarios/ambiguous-reliability.md` before implementation. |
| A3 | Custom short codes (aliases)? | not mentioned in requirements | Out of scope for Part 1. Candidate brownfield extension; requires reserved-word and abuse controls if added. |
| A4 | Short code uniqueness scale | single node vs distributed | Assume single-node prototype; code-generation strategy chosen so it does not preclude horizontal scaling (see ADR-002). |
| A5 | Expiration / deletion of links | not mentioned | Out of scope; noted as limitation in final summary. |
| A6 | Authentication / multi-tenant | not mentioned | Out of scope; API is anonymous. Noted as a production gap in risks. |

## 4. Normalized engineering problem (Part 1 scope)

Build a Spring Boot HTTP service that:

- `POST /api/urls` — accepts `{ "url": "<absolute http/https URL>" }`, validates it,
  persists a mapping, returns `201` with the short code and short URL.
  Invalid input → `400` with a structured error body.
- `GET /{code}` — issues a `302` redirect to the original URL; unknown code → `404`.
- `GET /api/urls/{code}` — returns mapping metadata (no redirect), for clients and later analytics.
- Persistence in an embedded relational DB (H2, file mode) behind a repository
  abstraction so the storage engine can be swapped (planned brownfield exercise).
- Unit tests for the service layer, integration tests for the HTTP contract.

## 5. Non-functional constraints (self-imposed, production-minded)

- Only `http`/`https` URLs accepted (rejects `javascript:`, `data:`, `file:` schemes — open-redirect/XSS vector).
- URL length capped at 2048 characters.
- Layered architecture (web / service / repository) — modular and testable.
- All AI-generated output is reviewed and owned by the engineer; see `docs/ai-log.md`.
