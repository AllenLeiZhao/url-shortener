# Architecture Decision Records

## ADR-001: Stack — Spring Boot 3, Java 21, Maven, H2 (file mode)

**Context.** 2–3 day prototype that must be runnable end-to-end on a clean machine, while
demonstrating production-grade structure.

**Decision.** Spring Boot 3.5 / Java 21 LTS / Maven. Persistence: H2 in file mode behind
Spring Data JPA.

**Rationale.**
- Java/Spring is the primary enterprise stack of the target organization; decisions are
  defensible in that context.
- H2 file mode gives zero-setup persistence across restarts; JPA + a repository interface
  keeps the storage engine swappable (a planned brownfield exercise is exactly this swap).
- Maven over Gradle: simpler for reviewers to run; no wrapper bootstrap surprises.

**Trade-offs.** H2 is not a production database (no HA, weak concurrency at scale). Accepted
for prototype scope; flagged in risks. JPA adds overhead vs plain JDBC — accepted for
development speed and schema management.

## ADR-002: Short-code generation — random Base62, length 7, collision retry

**Context.** Codes must be short, URL-safe, unique, and should not leak information.

**Options considered.**
1. **Sequential ID → Base62 encode.** Shortest possible codes, no collisions; but codes are
   enumerable (competitor/abuse can walk the sequence and scrape all URLs) and leak volume.
2. **Truncated hash (e.g. first 7 chars of SHA-256 of URL).** Deterministic/idempotent, but
   truncation collides across *different* URLs and needs the same retry machinery, with a
   false air of safety. (AI initially proposed this; rejected — see ai-log AI-3.)
3. **Random Base62, length 7, retry on collision.** 62⁷ ≈ 3.5 × 10¹² space; collision
   probability stays negligible for prototype volumes; not enumerable; horizontally scalable
   without ID coordination.

**Decision.** Option 3, generated with `SecureRandom`, unique constraint in DB as the source
of truth, bounded retry (5 attempts) on constraint violation.

**Trade-offs.** Same long URL shortened twice yields two codes (no idempotency). Accepted:
deduplication is a product choice, listed as a possible enhancement.

## ADR-003: Redirect status — 302 Found (not 301)

**Context.** 301 (permanent) lets browsers/CDNs cache the hop, reducing load — but cached
redirects bypass the server, which would blind the Part-2 analytics feature and make future
remapping/disabling of a code unreliable.

**Decision.** `302 Found`. Every hit reaches the service, keeping analytics accurate and
links revocable.

**Trade-offs.** Slightly higher server load per click; acceptable, and mitigable later with
short-TTL caching if needed.

## ADR-004: URL validation policy

**Decision.** Accept only absolute `http`/`https` URLs, max 2048 chars, parseable by
`java.net.URI` with a host present.

**Rationale.** A shortener is an open-redirect amplifier by design; the minimum bar is
refusing dangerous schemes (`javascript:`, `data:`, `file:`) that turn a redirect into an
XSS/local-access vector. Length cap bounds storage abuse and matches common browser limits.

**Trade-offs.** No blocklist of malicious *destinations* (phishing domains) — out of scope,
recorded in risks; a production deployment would integrate a reputation feed.

## ADR-005: Click events reference the code by value, captured asynchronously

**Context (brownfield, Part 2).** Analytics must not degrade the redirect hot path.

**Decision.** `click_events.code` stores the short code as a value (no FK to `short_urls`),
and capture runs fire-and-forget on a bounded async executor; failures and executor
saturation drop the event with a log line.

**Rationale.** A FK would couple every click write to the mappings table (lock/consistency
coupling on the hottest path) for integrity we don't need — a click for a deleted mapping is
harmless. Async capture keeps redirect latency independent of analytics load.

**Trade-offs.** Best-effort analytics: events in flight are lost on crash; exactly-once
would require an outbox/queue (out of prototype scope, listed in risks). Orphaned events
possible if mappings are ever deleted — acceptable.

## ADR-006: In-memory fixed-window rate limiter (no external dependency)

**Context (ambiguous scenario, Part 2).** The anonymous create API needs abuse protection
(reliability R2). AI proposed the bucket4j library.

**Decision.** Hand-rolled per-IP fixed-window counter (~60 lines, `ConcurrentHashMap`),
socket address only (X-Forwarded-For deliberately not trusted — no trusted proxy exists in
this deployment). Limit configurable; over-limit → 429 + `Retry-After`.

**Trade-offs.** Fixed windows allow up to 2× burst at window boundaries — irrelevant at
prototype scale. State is per-instance and resets on restart; multi-instance deployment
would move this to Redis (documented in risks). Stale-window pruning iterates the map on
every request — O(distinct client IPs per minute), fine at prototype scale; under real load
this becomes amortized/scheduled cleanup (or disappears entirely into Redis TTLs). Rejecting
the dependency keeps the build lean and the mechanism fully transparent/testable.
