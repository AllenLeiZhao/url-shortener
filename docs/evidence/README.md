# Evidence Index

Validation artifacts captured at the end of each part. Logs are live transcripts
(timestamps preserved); local filesystem paths are redacted to `~/workspace/...`.

## Part 1 — Greenfield core

| File | What it shows |
|------|---------------|
| `part1-mvn-test.log` | Test run: 15/15 green (8 unit + 7 integration) |
| `part1-api-demo.log` | Live curl transcript: 201 create, 302 redirect with `Location`, metadata lookup, 404 unknown code, 400 ×3 (dangerous scheme, blank, bean validation) |
| `part1-swagger-ui.jpg` | Swagger UI rendering the Part-1 API surface |
| `part1-app-startup.log` | Application boot log |

## Part 2 — Analytics (brownfield) + reliability (ambiguous)

| File | What it shows |
|------|---------------|
| `part2-mvn-test.log` | Test run: 25/25 green (incl. async analytics, rate limiting, health) |
| `part2-api-demo.log` | Live transcript: health probe UP (incl. DB), 3 clicks → stats shows 3 (sub-5ms redirects during capture), 22-request burst flipping 201→429 with `Retry-After`, redirect still 302 while create path is limited |
| `part2-app-startup.log` | Application boot log |

## Part 3 — Hardening & final verification

| File | What it shows |
|------|---------------|
| `part3-mvn-test.log` | Final test run: 31/31 green (adds concurrency, duplicate-URL, storage-outage 503, malformed-input tests) |
| `part3-clean-run.log` | Clean-environment verification: fresh copy → 31/31 tests → app run → create/redirect → full restart → same code still resolves (H2 file persistence) |

## Review fixes

| File | What it shows |
|------|---------------|
| `review-fixes-mvn-verify.log` | `mvn verify` after peer-review fixes: 31/31 tests + Spotless format check green |

## Final deliverable state (browser)

| File | What it shows |
|------|---------------|
| `final-swagger-ui.jpg` | Swagger UI with the complete API surface: redirect, create, lookup, stats |
| `final-redirect-in-browser.jpg` | A short link opened in the browser landing on the destination (example.com in the address bar) |
| `final-stats-endpoint.jpg` | Stats endpoint in the browser: `totalClicks: 6` — 5 scripted clicks plus the one real browser redirect above, closing the analytics loop |
| `final-demo-page.jpg` | Bonus demo page at `/` shortening a real URL in the browser |
