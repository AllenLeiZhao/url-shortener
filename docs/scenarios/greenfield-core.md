# Scenario 1 — Greenfield: Core URL Shortener from Scratch

*Requirement:* "Build a URL shortener service from scratch with core APIs."
Executed as Part 1. This write-up records how the greenfield scenario demonstrated
decomposition → execution → validation.

## 1. Decomposition

The high-level requirement was normalized in `requirements.md` (§4) and decomposed into
T1–T8 (`decomposition.md`) with explicit dependencies: skeleton → domain model →
code-generation strategy → APIs → tests → docs. Ambiguities (analytics granularity,
reliability scope, aliases, scale) were identified up front and deliberately deferred to
Parts 2–3 rather than guessed at.

## 2. Execution

- Layered Spring Boot service (web / service / repository) with H2 file-mode persistence
  behind a repository interface — the seam later exercised by the brownfield scenario.
- Contract: `POST /api/urls` (201/400), `GET /{code}` (302/404), `GET /api/urls/{code}`
  (200/404), structured error bodies, OpenAPI via springdoc.
- Key decisions captured as ADRs at decision time, not retrofitted: stack (ADR-001),
  random Base62 + DB-constraint collision retry (ADR-002 — AI's truncated-hash proposal
  rejected, ai-log AI-3), 302 over 301 (ADR-003), validation-as-security-boundary (ADR-004).
- AI generated boilerplate and first drafts throughout; engineer set constraints, reviewed,
  and edited — full trail in `ai-log.md` AI-1…AI-5.

## 3. Validation

- 15 tests (8 unit + 7 integration) green — validation policy, collision retry/exhaustion,
  full HTTP contract, cross-request code uniqueness.
- Live end-to-end transcript (`evidence/part1-api-demo.log`): 201, 302 with correct
  `Location`, 404, three distinct 400 paths.
- Swagger UI rendering verified (`evidence/part1-swagger-ui.jpg`).
