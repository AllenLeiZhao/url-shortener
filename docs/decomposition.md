# Task Decomposition & Sequencing

Tasks are ordered by dependency. Status is updated as work proceeds (iterative delivery
in three parts). Each task is executed with AI assistance under engineer review; per-task
traceability lives in `docs/ai-log.md`.

## Part 1 — Greenfield core

| ID | Task | Depends on | Acceptance criteria | Status |
|----|------|-----------|---------------------|--------|
| T1 | Project skeleton: Spring Boot 3 / Java 21 / Maven, layered packages, H2 file DB | — | `mvn test` runs green on a clean checkout | ✅ done |
| T2 | Requirements analysis & this decomposition doc | — | Ambiguities enumerated with resolutions (`requirements.md`) | ✅ done |
| T3 | Domain model + repository (`ShortUrl` entity, Spring Data repository) | T1 | Mapping persisted & queryable by code; unique index on code | ✅ done |
| T4 | Short-code generation strategy | T3 | Base62, collision-safe (retry), no enumerable sequence; ADR-002 records trade-offs | ✅ done |
| T5 | Create-URL API (`POST /api/urls`) with validation | T3, T4 | 201 + body on success; 400 with structured error for bad scheme/malformed/oversized URL | ✅ done |
| T6 | Redirect (`GET /{code}`) + lookup (`GET /api/urls/{code}`) | T3 | 302 with `Location` for known code; 404 JSON for unknown; ADR-003 records 301 vs 302 decision | ✅ done |
| T7 | Tests: service unit tests + HTTP integration tests | T5, T6 | Happy paths + validation failures + 404 covered; `mvn test` green | ✅ done |
| T8 | API documentation (OpenAPI/Swagger via springdoc) + README setup instructions | T5, T6 | `/swagger-ui.html` renders; README quick-start works on clean machine | ✅ done |

## Part 2 — Brownfield & ambiguous scenarios

| ID | Task | Depends on | Acceptance criteria | Status |
|----|------|-----------|---------------------|--------|
| T9 | Brownfield impact analysis: adding click analytics to the existing codebase | T1–T8 |  `docs/scenarios/brownfield-analytics.md` lists impacted modules/APIs/data flows before code changes | ✅ done |
| T10 | Analytics implementation: click event capture on redirect + stats endpoint | T9 | Redirect latency unaffected (async capture); `GET /api/urls/{code}/stats` returns counts | ✅ done |
| T11 | Ambiguity normalization: "reliability features" | T1–T8 | `docs/scenarios/ambiguous-reliability.md` — ambiguity analysis → normalized scope | ✅ done |
| T12 | Reliability implementation (scope decided in T11; candidates: rate limiting, health probes, graceful 503) | T11 | Per normalized acceptance criteria in T11 | ✅ done |

## Part 3 — Hardening & final summary

| ID | Task | Depends on | Acceptance criteria | Status |
|----|------|-----------|---------------------|--------|
| T13 | Test-gap review & additional edge-case tests | T10, T12 | Concurrency (code collision), duplicate URL, storage failure paths covered | ✅ done |
| T14 | Risk register & guardrails doc | all | `docs/risks.md`: failure scenarios, trade-offs, mitigations | ✅ done |
| T15 | Final engineering summary | all | Plan/rationale, artifacts, risks, assumptions, limitations in one doc | ✅ done |
| T16 | Clean-environment verification of setup instructions | all | Fresh copy → test → run → exercise endpoints per README; persistence across restart | ✅ done |
| T17 | Greenfield scenario write-up (retrospective of Part 1) | T1–T8 | `docs/scenarios/greenfield-core.md` completes the three-scenario deliverable | ✅ done |
| T18 | Peer-review fixes: quality gates (Spotless + CI), error-contract completeness, doc corrections | T1–T17 | Review items dispositioned in ai-log AI-14; `mvn verify` green | ✅ done |
| T19 | Bonus demo page: static single-file UI over the API at `/` | T5, T10, T12 | Page served without shadowing `/{code}` routes (tested); zero backend changes | ✅ done |
