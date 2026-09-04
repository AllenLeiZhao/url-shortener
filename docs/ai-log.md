# AI Usage Log (Traceability)

Every AI-assisted task records: the intent/constraints given to the AI, what it produced,
and the engineer's disposition — **adopted / edited / rejected** — with rationale.
The engineer reviews and owns every line that lands in the repository.

Format: `AI-<n> · <task ref> · <disposition>`

---

## Part 1

### AI-1 · T2 · adopted (edited)
**Intent given.** "Read the assignment PDF; identify what is actually evaluated and where the
ambiguities are; draft a requirements analysis."
**AI output.** Draft of `requirements.md` including the ambiguity table (analytics granularity,
reliability scope, aliases, scale assumptions).
**Disposition.** Adopted after review; I edited scope resolutions (deferred analytics to the
brownfield scenario deliberately rather than building it Part 1, to keep iteration honest).

### AI-2 · T1 · adopted (engineer-directed)
**Intent given.** "Spring Boot 3 + Java 21 + Maven skeleton, layered packages
(web/service/repository/model), H2 file mode, springdoc." Stack choice was mine (see ADR-001);
AI generated the boilerplate (pom, application.yml, package layout).
**Disposition.** Adopted; verified versions resolve and `mvn test` passes locally.

### AI-3 · T4 · **rejected**
**Intent given.** "Propose a short-code generation strategy; codes must be short, unique, not
enumerable."
**AI output.** First proposal: truncated SHA-256 of the URL (deterministic, idempotent).
**Disposition.** **Rejected.** Truncated hashes still collide across different URLs, so the
retry machinery is needed anyway while the determinism gives a false sense of safety; it also
makes "same URL, two users" linkable. Directed AI to option 3 of ADR-002 (SecureRandom Base62
+ DB unique constraint + bounded retry), which I adopted after reviewing the collision math.

### AI-4 · T5/T6 · adopted (edited)
**Intent given.** "Controller + service + DTOs for create/redirect/lookup. Constraints:
validation per ADR-004, structured error body via a global exception handler, no business
logic in controllers, 302 per ADR-003."
**AI output.** Web layer + service implementation.
**Disposition.** Adopted with edits: tightened validation (host-presence check — `URI.create`
accepts `http:foo` without host), and made the collision retry loop catch the DB constraint
violation rather than pre-checking existence (TOCTOU race under concurrency).

### AI-5 · T7 · adopted (edited)
**Intent given.** "Unit tests for service (happy path, invalid scheme, malformed, oversize,
collision retry) and MockMvc integration tests for the HTTP contract (201/400/302/404)."
**AI output.** Test classes.
**Disposition.** Adopted; I added the collision-exhaustion case (generator keeps colliding →
a clean 500 with retry message) after noticing the gap.

## Part 2

### AI-6 · T9 · adopted (edited)
**Intent given.** "Before writing any analytics code, produce an impact analysis of adding
click analytics to the existing codebase: impacted modules, data flow, failure scenarios,
what stays unchanged."
**AI output.** Draft of `scenarios/brownfield-analytics.md`.
**Disposition.** Adopted with edits: I added the explicit 'unchanged — verified, not assumed'
section and the acceptance criterion that Part-1 tests must pass unmodified.

### AI-7 · T10 · adopted (edited, scope cut)
**Intent given.** "Implement per-click capture + stats endpoint per the impact analysis.
Constraints: async fire-and-forget off the redirect thread, bounded executor, failures
logged and swallowed."
**AI output.** `ClickEvent`/`ClickTrackingService`/stats endpoint; initial draft also stored
the client IP for a unique-visitor count.
**Disposition.** Adopted after **cutting the IP field** — PII cost outweighs prototype value
and it violates the privacy floor I set in the scenario doc. Referrer/UA kept, truncated.

### AI-8 · T11 · adopted
**Intent given.** "Enumerate plausible interpretations of 'reliability features' for a
single-node prototype; score by value/cost; propose a normalized scope."
**AI output.** Interpretation table R1–R6 in `scenarios/ambiguous-reliability.md`.
**Disposition.** Adopted; I set the decision rule (protect availability of the running
prototype + platform prerequisites; defer infra-dependent items) and the R4/R5/R6 exclusions.

### AI-9 · T12 · **rejected** (dependency), adopted (implementation)
**Intent given.** "Rate-limit POST /api/urls per client IP; over-limit → 429 + Retry-After;
redirects must never be limited."
**AI output.** First proposal: add the bucket4j dependency for token-bucket limiting.
**Disposition.** **Rejected the dependency** — a ~60-line fixed-window filter is fully
testable and transparent, and token-bucket smoothness buys nothing at prototype scale
(ADR-006). Second iteration (hand-rolled filter) adopted; I directed the decision not to
trust `X-Forwarded-For` since no trusted proxy fronts the prototype (spoofable bypass).

### AI-10 · T10/T12 validation · adopted (edited)
**Intent given.** "Integration tests: clicks reflected in stats despite async capture;
429 behavior in an isolated context with a low limit; health probe; storage-failure 503 mapping."
**AI output.** Test classes incl. polling helper for async settling.
**Disposition.** Adopted; I fixed a test-environment defect it missed (test `application.yml`
shadows the main one entirely, so actuator `show-details` had to be repeated there — the
health test failed until diagnosed) and kept the fix in test config rather than weakening
the assertion.

## Part 3

### AI-11 · T13 · adopted (edited)
**Intent given.** "Review test coverage against the risk surface; add edge-case tests for
concurrent create uniqueness, duplicate-URL behavior, storage-outage degradation, and
inputs that must never reach handlers (bad code shapes, malformed JSON)."
**AI output.** `EdgeCaseHttpIntegrationTest` (30-way concurrent creates) and
`StorageFailureHttpIntegrationTest` (mocked repository throwing on lookup/create → 503).
**Disposition.** Adopted; I confirmed the concurrency test exercises the real DB constraint
path (no mocks) and that the duplicate-URL test asserts the ADR-002 trade-off explicitly.

### AI-12 · T14/T15 · adopted (edited)
**Intent given.** "Risk register grouped by category with prototype guardrail vs production
path per risk — 'accepted' must mean consciously accepted, not overlooked. Then the final
summary: plan/rationale, artifacts, validation, trade-offs, assumptions, limitations."
**AI output.** Drafts of `risks.md` and `final-summary.md`.
**Disposition.** Adopted; I added the engineering-process risk section (AI-assisted delivery
risks and their guardrails) and the ownership statement.

### AI-13 · T16 · adopted
**Intent given.** "Verify the README on a clean copy: fresh directory without build output
or data, run tests, start the app, exercise endpoints, and prove mappings survive a restart."
**AI output.** Scripted verification; transcript in `evidence/part3-clean-run.log`.
**Disposition.** Adopted — 31/31 tests green on the clean copy; same code resolves to the
same destination after a full stop/start (H2 file persistence confirmed).

## Review pass

### AI-14 · T18 · peer review by a second AI session — dispositioned item by item
**Intent given.** A second, independent Claude Code session was asked to review the full
repo against the assignment PDF and report gaps; this session then dispositioned each item.
**Review output → disposition.**
- *Quality gates missing beyond tests* — **adopted**: Spotless (Palantir format) added and
  bound to `mvn verify`; GitHub Actions CI added. OWASP dependency-check **deliberately not
  added** (NVD key + feed download outweigh value in a time-boxed take-home); the gap is
  documented in `final-summary.md` instead of hidden.
- *Commit history in per-part increments* — **rejected as stated, adopted in spirit**:
  intermediate file states no longer existed, so fabricating per-part commits would produce
  non-compiling, dishonest history. Real history recorded instead: two commits — baseline
  including review fixes (with a message explaining why history starts late), then quality
  gates.
- *Malformed JSON bypasses the structured error contract* — **adopted**: added
  `HttpMessageNotReadableException` handler; test now asserts the structured body.
- *`click_events` index should be (code, occurredAt)* — **adopted**: composite index.
- *`GlobalExceptionHandler` should use the injected `Clock`* — **adopted**.
- *Rate-limiter prune cost worth acknowledging* — **adopted as documentation** (ADR-006):
  O(distinct IPs/min) is fine at prototype scale; scheduled cleanup or Redis TTLs at load.
- *Doc errors (task range T16→T18 refs, X-Forwarded-For wording, "502-style" phrasing,
  missing `ddl-auto` risk)* — **all adopted**; `ddl-auto: update` now has a risk row with
  Flyway/`validate` as the production path.
