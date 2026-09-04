# Scenario 2 — Brownfield: Adding Click Analytics to the Existing Service

*Requirement:* "core APIs, **analytics**, and reliability features." This part treats the
Part-1 codebase as an existing production system being enhanced — the impact analysis below
was written **before** any code change (task T9), then the change was implemented (T10).

## 1. Requirement normalization

Ambiguity A1 (`requirements.md`): "analytics" granularity is unspecified. Normalized scope:

- Capture a **per-click event** on every successful redirect (timestamp, referrer,
  user-agent). Per-event capture is chosen over a bare counter because aggregates can always
  be derived from events, never the reverse.
- Expose `GET /api/urls/{code}/stats` returning: total clicks, clicks in the last 24 h,
  last-click time.
- **Privacy floor:** no IP address, no cookies, no fingerprinting. Referrer and user-agent
  are stored truncated. (An IP-based unique-visitor count was considered and rejected — PII
  cost outweighs prototype value; see ai-log AI-7.)

**Acceptance criteria**
1. Redirect latency is not affected by analytics capture (write happens off the request thread).
2. A failed analytics write never breaks a redirect (fire-and-forget with logged error).
3. `GET /api/urls/{code}/stats` → 200 with counts; 404 for unknown code.
4. Existing Part-1 tests continue to pass unmodified (behavioral compatibility).

## 2. Impact analysis (before implementation)

### Impacted modules

| Module | Change | Risk |
|--------|--------|------|
| `web/RedirectController` | After resolving, hand off click capture; must not add blocking work | Latency regression if capture is synchronous → mitigated by `@Async` |
| `service/` (new `ClickTrackingService`) | New: async event recording + stats aggregation | Async executor saturation under click storms → bounded queue, drop-with-log |
| `model/` (new `ClickEvent`) | New entity + table `click_events`, composite index (code, occurredAt) | Table growth unbounded → accepted for prototype, retention noted in risks |
| `repository/` (new `ClickEventRepository`) | New Spring Data interface (count / count-since / last-click queries) | — |
| `web/ShortUrlApiController` | New `GET /api/urls/{code}/stats` endpoint + DTO | — |
| Application config | `@EnableAsync` + named thread pool | Must size pool; defaults documented |

### Unchanged (verified, not assumed)

- `UrlShortenerService.shorten/resolve` — analytics reads codes, never mutates mappings.
- Validation, error contract, short-code generation — untouched.
- DB schema of `short_urls` — untouched; `click_events` references the code value rather
  than a FK to `short_urls.id`, trading referential integrity for zero write coupling on the
  hot redirect path (deliberate; recorded in ADR-005).

### Data flow (new)

```
GET /{code} → resolve (sync, unchanged) → 302 response
                     └─ record(code, referrer, ua) ──→ async executor ──→ click_events
GET /api/urls/{code}/stats → verify code exists → aggregate queries → JSON
```

### Failure scenarios considered

| Scenario | Behavior |
|----------|----------|
| Analytics DB write fails | Redirect already returned 302; error logged; click lost (accepted — analytics is best-effort, mappings are not) |
| Async queue full (click storm) | Executor rejects; drop with log; redirect unaffected |
| Service crash between redirect and async write | In-flight events lost (accepted; would need an outbox/queue for exactly-once — out of scope, noted in risks) |
| Stats queried for unknown code | 404 via existing `ShortUrlNotFoundException` path |

## 3. Execution

Implemented as designed. Notable engineering decisions during execution are in
`docs/ai-log.md` (AI-6…AI-8) and ADR-005.

## 4. Validation

- Unit tests: stats aggregation, capture failure does not propagate.
- Integration tests: clicks via `GET /{code}` are reflected in stats (with async settling),
  404 for unknown code, Part-1 contract tests still green.
- Manual end-to-end transcript in `docs/evidence/part2-api-demo.log`.
