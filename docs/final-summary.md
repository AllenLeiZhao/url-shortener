# Final Engineering Summary

## 1. What was built

A URL shortener service (Java 21, Spring Boot 3.5, Maven, H2 file-mode) delivered in three
iterative parts, each treating AI as an accelerator inside engineer-defined tasks:

- **Part 1 — greenfield core:** create (`POST /api/urls`), redirect (`GET /{code}`, 302),
  lookup (`GET /api/urls/{code}`); layered architecture; validation as a security boundary;
  OpenAPI docs.
- **Part 2 — brownfield scenario:** click analytics added to the existing codebase after a
  written impact analysis — async per-click capture off the redirect path, stats endpoint.
- **Part 2 — ambiguous scenario:** "reliability features" normalized from six candidate
  interpretations into three implemented ones (health probes, per-IP create rate limiting,
  graceful 503 degradation) with recorded exclusions.
- **Part 3 — hardening:** concurrency/duplicate/storage-failure/malformed-input tests, risk
  register, clean-environment verification.

Final state: **34 tests green** (13 unit, 21 integration across 6 HTTP test classes),
runnable end-to-end with `mvn spring-boot:run` — including a bonus single-file demo page
at `/` (a static shell over the API; no backend changes).

## 2. Plan & rationale

The assignment's differentiator is disciplined AI-assisted execution, so the plan optimized
for **traceable process over feature count**:

1. Normalize the requirement in writing first (`requirements.md`) — six ambiguities
   identified and dispositioned, not guessed at.
2. Decompose into tasks with dependencies and acceptance criteria (`decomposition.md`,
   T1–T19), executed in dependency order across three parts.
3. For every AI-assisted task, record intent → output → adopted/edited/rejected with
   rationale (`ai-log.md`, AI-1…AI-16).
4. Make design decisions through ADRs with options and trade-offs (`decisions.md`,
   ADR-001…006), not by accepting the first AI proposal — two proposals were rejected
   outright (truncated-hash codes, AI-3; bucket4j dependency, AI-9) and one was cut for
   privacy (client-IP storage, AI-7).
5. Gate each part on: full test suite green + live end-to-end transcript captured under
   `docs/evidence/`.

## 3. Artifacts

| Artifact | Location |
|----------|----------|
| Runnable service + tests | `src/` |
| Setup & API reference | `README.md`, `/swagger-ui.html` at runtime |
| Requirements analysis | `docs/requirements.md` |
| Task decomposition (T1–T19, all ✅) | `docs/decomposition.md` |
| ADRs ×6 | `docs/decisions.md` |
| AI traceability log ×16 entries | `docs/ai-log.md` |
| Scenario write-ups ×3 (greenfield / brownfield / ambiguous) | `docs/scenarios/` |
| Risk register & guardrails | `docs/risks.md` |
| Evidence: test logs, live API transcripts, UI screenshot | `docs/evidence/` |

> Test counts cited in per-part documents reflect the suite as it stood at each stage —
> 15 after Part 1, 25 after Part 2, 31 after Part 3, 34 including the bonus page — and
> grew monotonically; they are snapshots, not inconsistencies.

## 4. Validation summary

- **Unit:** URL validation policy (schemes/host/length), collision retry & exhaustion,
  click capture (truncation, null metadata, failure swallowing), stats aggregation.
- **Integration:** full HTTP contract (201/400/302/404/429/503), async clicks reflected in
  stats, rate limiting isolated in its own context, health probe, 30-way concurrent create
  uniqueness, storage-outage degradation via mocked repository.
- **Live verification:** per-part curl transcripts with timestamps, including the
  201→429 rate-limit flip and sub-5ms redirects during async capture; clean-environment
  run in `docs/evidence/part3-clean-run.log` including persistence across restart.

### Quality gates

`mvn verify` is the gate: compilation, all tests, and Spotless format check
(Palantir Java Format); `.github/workflows/ci.yml` runs it on every push/PR.
Deliberately **not** included, with rationale: OWASP dependency-check (needs an NVD API
key and long feeds download — listed as the first addition for a real repo) and static
analysis beyond format (SpotBugs/ErrorProne — value at this codebase size didn't justify
the setup inside the time-box; the gap is recorded rather than hidden). A peer review by a
second AI session was run against the assignment rubric and dispositioned item-by-item
(ai-log AI-14).

## 5. Key trade-offs (full register in `docs/risks.md`)

- Random Base62 codes: not enumerable and coordination-free, at the cost of no idempotent
  create (same URL → two codes) — a product choice, documented and tested.
- 302 over 301: server sees every hit (analytics accuracy, revocability) at the cost of
  redirect load.
- Best-effort analytics: clicks can be lost on crash/saturation; redirects never block on
  analytics. Exactly-once would need an outbox/queue.
- Hand-rolled in-memory rate limiter: transparent, dependency-free, per-instance; a
  multi-instance deployment moves it to Redis.
- H2 file mode: zero-setup reviewer experience over production realism; storage is behind
  a repository seam specifically so this swaps.

## 6. Assumptions

Single-node deployment; anonymous API (no authN/tenancy); no link expiry or aliases;
no trusted proxy in front (hence socket-address rate limiting); prototype traffic volumes
(collision probability, table growth, and fixed-window burst are all assessed at that scale).

## 7. Limitations & next steps

No destination-reputation checking, no dependency/static-analysis scanning in CI, no
retention policy, no distributed deployment story beyond the seams left for it. First
production steps, in order: external database + versioned migrations (Flyway), dependency
scanning in CI, authenticated create API with per-key quotas, reputation checking at create
time, Redis-backed rate limiting.

## 8. Ownership statement

AI (Claude, via Claude Code) drafted boilerplate, first-pass implementations, tests, and
document drafts throughout. Every design decision, scope call, rejection, and merged line
was reviewed and is owned by the engineer; the traceability log exists so that claim is
auditable rather than asserted.
