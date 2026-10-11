# Security Scan Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` to execute this plan. Each independent problem must be fixed on a separate branch by a dedicated subagent. Similar or overlapping problems must be fixed on the same branch by the same subagent, using the ownership groups below. Do not implement fixes on the coordinating agent's checkout.

**Goal:** Resolve the real risks in `~/yadaframework-security-scan-2026-10-09.md`, preserve the existing fixes, and prevent bearer tokens from being usable after a database or log disclosure.

**Architecture:** Keep the current Spring MVC, Hibernate DAO, and Yada controller structure. Make token issuance and consumption authoritative on the server, derive DataTables permissions from server configuration, and use secure defaults at request/response boundaries. A foundations branch delivers shared prerequisites; six branch groups then own related changes end to end.

**Tech stack:** Java 21, Spring MVC/Security, manual JPA/Hibernate, Thymeleaf, Gradle, JUnit Jupiter; existing jQuery/Yada browser helpers.

**Spec / evidence:** The supplied security scan plus source review of branch `0.7.9`, commit `8d20e54c572a7cef8a2cf63c6cc0996160369bff`. This is a planning deliverable only; no application changes, exploit reproduction, or runtime tests were performed for this review. Source-confirmed defects and conditional exploit claims are distinguished below.

## Global constraints

- Preserve the current `SecureRandom` changes and server-created password-reset entity. Hashing supplements them; it does not solve predictable generation, malicious binding, or replay by itself.
- Use existing Yada APIs after verifying their implementations against `aiknowledge`. `YadaTokenHandler` supplies link construction/parsing, not cryptographic signatures despite dictionary wording. `YadaWebUtil.redirectString`, `enhanceUrl`, and `sanitizeUrl` are not safe-local-redirect validators. `getClientAddress` also trusts forwarded headers and cannot be reused unchanged for audit trust decisions.
- Use a separate git worktree per branch. Start from the reviewed baseline or the integration revision explicitly supplied by the coordinator. No unrelated refactoring, branch deletion, automatic publishing, or merging as part of this planning request.
- Each owner writes focused regression tests, demonstrates failure where the defect remains, implements the change, runs the relevant tests/build, and commits only its owned changes. Tests for the user's existing fixes are expected to pass on the baseline; do not require them to fail artificially.
- Entity changes require a Flyway migration and schema verification using `xtian-schema-update` / `xtian-dbschema` at execution time. `YadaExamples/src/main/resources/database/V001__baseline.sql` must not be edited to upgrade existing deployments. Allocate a new version after checking the execution branch and generate the actual filename then; document upgrades for downstream apps inheriting these entities.
- Preserve registration-request subclasses and legitimate configurable roles, locales, custom table endpoints, and upload-limit behavior. Document intentional API and link compatibility breaks explicitly.
- Tabs for Java, English comments, no new Spring Boot or Spring Data layer. Add test dependencies only where needed and compatible with the existing stack.

## Triage of all findings

The report's five dismissed entries are duplicates, not evidence that their underlying risks are absent. The final section maps them to their owners.

| Open # | Current evidence and assessment | Owner |
|---|---|---|
| 1 | Original PRNG weakness fixed: `YadaRegistrationRequest.setDefaults()` uses `SecureRandom.nextLong() & Long.MAX_VALUE` (63 random bits). Plaintext storage, missing redemption-time expiry/purpose checks, and non-atomic consumption still deserve remediation. Sequential IDs are acceptable selectors when the secret is strong. | A |
| 2 | Reported password-reset mass assignment fixed: `yadaPasswordResetPost` constructs a fresh entity and copies only email. It still binds a full incoming entity unnecessarily; `handleRegistrationRequest` still saves a caller-supplied entity, and password redemption accepts any matching request type. Protect registration too. | A |
| 3 | Original autologin PRNG weakness also fixed by the same commit. Tokens remain plaintext, reusable, and allowed to have null expiration. | A |
| 4 | Source-confirmed unsafe query construction and arbitrary field selection: client column paths and order direction enter HQL; client `extraJsonAttributes` enters reflection-based output. `securityAsPath` controls table access, not which fields may be disclosed. Full injection payload impact needs a Hibernate-backed reproduction; arbitrary field disclosure independently warrants a fix. | B |
| 5 | Source-confirmed unchecked autologin redirect on success and already-logged-in rejection. Attacker-owned bearer links can also log a visitor into the attacker's account. Safe redirects and single use reduce distinct risks; neither alone prevents login CSRF. | A |
| 6 | Example controller binds profile/credential identities, authorizes roles using the profile identity, then cascade-merges credentials. This is an unsafe identity boundary. The exact ADMIN overwrite described depends on merge, version, and unique-FK behavior and is not runtime-proven. Fix the boundary even if that particular exploit rolls back. | F |
| 7 | Source-confirmed: `AuditFilter` replaces the logged address with arbitrary `X-Forwarded-For`. Exploit depends on ingress behavior. `YadaWebUtil.getClientAddress/getClientIp` share the trust problem. | C |
| 8 | Source-confirmed: `YadaSecurityConfig` disables all header writers and exposes username-not-found exceptions. The default failure handler produces distinct flags and carries the submitted password in the unknown-user case. Effective missing headers depend on upstream configuration; framework defaults remain unsafe. | D |
| 9 | Source-confirmed raw exception text is written after response commit. A real reflected-XSS trigger was not demonstrated by the scan; unsafe output and exception disclosure are sufficient reasons for a small fix. | E |
| 10 | URL leakage pattern confirmed. The cited `YadaActionUploadAttrProcessor` is marked unused, but active `YadaMultipartFormTagProcessor` also appends CSRF to multipart actions. Leakage requires log access or applicable referrer behavior; third-party referrer leakage is not universal under browser defaults. | D |
| 11 | Source-confirmed autologin secrets in success/expiry/not-found logs and no consumption. Registration logs, entity `toString()`, parser logs, and audit request paths expose related secrets too. | A |
| 12 | Example impersonation endpoints accept GET, save unvalidated `currentLocation`, and replay it through a redirect helper that accepts external URLs. Requires an authorized victim and an impersonatable target. | F |

## Branch ownership and execution order

| Group | Branch | Dedicated implementing subagent | Findings |
|---|---|---|---|
| 0 | `security/foundations` | `authentication_security` | Shared prerequisites: `safeLocalRedirect`, MySQL test fixture, YadaExamples test setup |
| A | `security/authentication-boundaries` | `authentication_security` | 1, 2, 3, 5, 11 |
| F | `security/example-app-boundaries` | `example_security` | 6, 12 |
| B | `security/datatables-server-policy` | `datatable_security` | 4 and its three dismissed duplicates |
| C | `security/audit-proxy-trust` | `audit_security` | 7 |
| D | `security/headers-and-upload-csrf` | `http_security` | 8, 10 |
| E | `security/error-response-safety` | `error_security` | 9 |

Group A keeps both token families together because they share parsing/link/email code and a single token contract. The shared redirect validator and the database test fixture are split into Group 0 so that other groups do not wait for all of A. The example-app identity and impersonation fixes (F) touch only `YadaExamples` plus the Group 0 redirect helper, so they are a separate branch. Group D keeps header/referrer configuration together with multipart CSRF transport.

- [ ] Coordinator records the clean baseline, opens isolated worktrees, and assigns each group to its named subagent with this plan and the scan. Keep one implementing owner for all tasks within a group; separate review agents may review its work.
- [ ] Group 0 runs first and is integrated before A, B, and F write Hibernate-backed tests or use `safeLocalRedirect`. A, B, and F branch from the integrated Group 0 revision.
- [ ] Prioritize A and B. C may investigate in parallel, but apply its `AuditFilter` edits after A's redaction changes are integrated. E may investigate in parallel but integrate after B because both touch `YadaController`. B's example-endpoint regression tests run after F, because F changes the same `UserProfileController`.
- [ ] D can proceed independently; any authentication-handler conflict with A must be resolved by preserving both generic failure behavior and token redaction.

## Group 0: shared foundations

Owned by `authentication_security`; delivered as its own branch and integrated before dependent work starts.

- [ ] **Database test fixture.** No Hibernate/database test infrastructure exists today (existing DAO tests are Mockito-only; `YadaWeb` test fixtures provide only `YadaEmbeddedTomcatTestServer`). Add a reusable MySQL-backed JPA fixture to `YadaWeb/src/testFixtures`. Use MySQL, not H2, because the tests must reproduce MySQL row locking, unique constraints, and Flyway MySQL syntax. Prefer Testcontainers when Docker is available; otherwise read a disposable database URL/credentials from a system property or environment variable. When neither is available, the database tests must report a visible verification gap (assumption failure with a clear message), never a silent pass.
- [ ] **Safe local redirect.** Add `String safeLocalRedirect(String candidate)` to `YadaWebUtil` with the test matrix in A3, in `YadaWeb/src/test/java/net/yadaframework/components/YadaWebUtilRedirectTest.java`. It returns a validated local path or `/`. Keep `redirectString` as a formatting helper after validation, because it has legitimate external-URL callers elsewhere. Existing `sanitizeUrl` intentionally permits external URLs and must not substitute for this method.
- [ ] **YadaExamples tests.** `YadaExamples` has no `src/test` folder and no test configuration. Configure JUnit Jupiter and `useJUnitPlatform()` in `YadaExamples/build.gradle` following the `YadaWeb` module setup, with the fixture above available, and add one smoke test so later groups' tests demonstrably execute.
- [ ] **Build hygiene check.** `YadaWebSecurity/settings.gradle` also includes leftover Gradle-init projects `shared`, `api`, and `services:webservice` that do not exist. Verify `bash YadaWebSecurity/gradlew -p YadaWebSecurity test :YadaWeb:test` runs under the pinned Gradle 8.14.3; if the leftovers cause errors or deprecation failures, remove those three lines in this branch.
- [ ] Each branch returns commits, changed APIs/configuration, reproduction evidence, exact test results, and deployment requirements. Coordinator reviews diffs and re-runs affected checks on the combined result before declaring scan items resolved. Integration/push happens only when separately requested.

## Group A: authentication boundaries

### Files and interfaces

Modify under `YadaWebSecurity/src/main/java/net/yadaframework/security/`:

- `persistence/entity/YadaRegistrationRequest.java`, `YadaAutoLoginToken.java`.
- `persistence/repository/YadaRegistrationRequestDao.java`, `YadaAutoLoginTokenDao.java`; credential DAO methods only as needed for atomic consumption and password update.
- `components/YadaTokenHandler.java`, `YadaSecurityUtil.java`, `YadaSecurityEmailService.java`.
- `web/YadaRegistrationController.java`, `YadaLoginController.java`, and `AuditFilter.java` for token-path redaction only.
- The autologin confirmation view under the YadaWebSecurity views, plus its message-bundle entries.

Add `YadaSecurityTokenCodec` under `security/components` with `String generateSecret()`, `String digest(String purpose, String secret)`, `ParsedSecurityToken parse(String link)`, and a `ParsedSecurityToken(long id, String secret)` record. Generate 32 random bytes, encode as 64 lowercase hex characters, and format links as `<positive-id>-<secret>`. Digest with SHA-256 over a fixed domain prefix, purpose, and secret; retain only the 64-character digest in persisted entities. These are proposed new APIs, not existing Yada APIs.

Create token/binding/redirect tests in `YadaWebSecurity/src/test/java/net/yadaframework/security/`, using the Group 0 MySQL fixture for identity, locking, and concurrent-redemption tests; Mockito-only tests cannot establish merge or transaction correctness.

### A1. Token storage, validity, and consumption

**Tests first**

- [ ] Assert database rows and entity string representations contain no raw bearer secret.
- [ ] Assert freshly issued links work while reloaded entities cannot regenerate their secrets.
- [ ] Assert malformed IDs, extra separators, truncated/oversized secrets, wrong purposes, and nonexistent IDs return the existing invalid-link outcome without a stack trace or secret logging.
- [ ] Run the focused tests before implementing. Preserve passing tests for the current `SecureRandom` fix and clean password-reset entity.

**Storage and API**

- [ ] Persist `tokenHash` rather than `long token` in both entities.
- [ ] Raw-secret hand-off: add a `@Transient` raw-secret field that is populated only on an entity freshly issued by `makeAutoLoginToken` / the registration-request factory, and is never loaded from the database. `makeAutologinLink(...)`, `makeLink(...)`, and email creation read it and throw `YadaInvalidUsageException` when it is null (for example on a reloaded entity). This keeps the existing `makeAutologinLink(entity, ...)` call shape; callers that persist a token and build its link later must issue a new token instead, which is documented as a compatibility break.
- [ ] Migrate all `parseLink`, `makeLink`, `getToken`, and numeric DAO consumers together, including invitation emails and subclass registration flows. Keep old numeric APIs only as deprecated fail-closed adapters if source compatibility requires them; they must not issue or redeem legacy links. Document the new string API for consumers.

**Validity**

- [ ] Implement purpose-aware lookup and consumption in DAO transactions. Recovery must require `PASSWORD_RECOVERY`; registration and email-change confirmation must require their own types. `passwordResetForm` must use the same expiry/purpose validation as final redemption.
- [ ] Lifetimes: configurable 30-minute default for recovery/email-change, 24-hour default for registration, and preserve the configured autologin lifetime with a mandatory finite expiration. Expiry comparison is `now >= expiration` => invalid; null/invalid timestamps fail closed. Cleanup is maintenance, never the validity check.

**Atomic consumption**

- [ ] Make recovery consumption and password update one transaction using a pessimistic row lock (`SELECT … FOR UPDATE`) or an equivalent conditional-consume update. A rollback restores the token's availability; authentication occurs after commit. Use the same atomic model for registration/email change.
- [ ] Autologin tokens are single-use (this resolves the existing `TODO delete autologin link after use`). Consume atomically once before creating an authenticated session; a later session-creation failure requires a new link rather than reviving a consumed token. Reject disabled or otherwise ineligible users before consuming/authenticating. Never rely on a synchronized JVM block for cross-instance single use.
- [ ] Test exact expiry, wrong type, repeated use, two simultaneous redemptions in separate transactions (only one succeeds), rollback during password update, replacement requests invalidating previous links, and null-expiration autologin rejection. Test stored digests cannot be presented as secrets to redeem links.

**Migration**

- [ ] Generate and validate a new schema migration in `YadaExamples` only; the framework ships no migrations for its own entities. Provide downstream applications with a documented SQL template in the Yada developer docs.
- [ ] Invalidate all outstanding legacy token rows and require fresh links; do not preserve plaintext or weak historical links as a fallback. Use a maintenance/atomic rollout so old application nodes cannot issue tokens during migration.
- [ ] Identify any subclass foreign keys before deleting requests and preserve user accounts/profile data. Document migration order, expected broken outstanding links, backup/rollback limitations, and downstream schema upgrades.

SHA-256 here protects high-entropy random tokens; password hashing remains the existing configured password encoder. No password encoder change is part of this task. An HMAC or server-side pepper is deliberately not used: with 256-bit random secrets, a stolen digest cannot be brute-forced, so a key would add key-management cost without meaningful protection. Hashing does not protect a secret already captured in a URL or email.

### A2. Registration and reset binding

- [ ] Test malicious reset parameters `id`, `token`, `tokenHash`, `timestamp`, `registrationType`, and `yadaUserCredentials.id`: only the submitted email may influence the fresh recovery request. Verify the attacker's existing row cannot be converted into a victim request.
- [ ] Replace reset entity binding with an email-only DTO. Retain existing controller extension compatibility through a documented deprecated adapter if necessary. Use explicit form allowlists/DTOs for registration; construct new trusted entities and preserve explicitly permitted subclass fields. `handleRegistrationRequest` must reject a non-null supplied identity before cleanup/save, so downstream callers cannot bypass the controller binder.
- [ ] Update the example registration controller/form binding in `YadaExamples` to use the new allowlist/DTO, since it is a direct consumer of this API.

### A3. Autologin redirects and login CSRF

- [ ] `safeLocalRedirect` (delivered by Group 0) is tested with `/`, `/dashboard?tab=users`, locale/context paths, absolute external URLs, `//host`, `/\\host`, backslashes, whitespace/control characters, CRLF, percent-encoded slashes/backslashes, and double-encoded variants. Invalid values resolve to `/`; ordinary local query strings/fragments continue working.
- [ ] Remove the extra `URLDecoder.decode` of an MVC-decoded autologin parameter. Validate destinations at link creation and at every redirect exit, including an already-logged-in rejection. If an action is stored in the token record, reject URL action changes rather than treating them as authoritative. Do not decode repeatedly until an attacker-supplied URL becomes acceptable.
- [ ] Preserve email-link usability with a safe GET confirmation page, followed by an explicit POST carrying a session-bound CSRF token. GET must not authenticate or consume the token. Do not auto-submit the confirmation page. Show enough account identity for the visitor to recognize whose account they will enter, without exposing another user's details on invalid links. Test that a foreign-site POST cannot log in and that another user's active session is never silently replaced. Single-use tokens alone do not close login CSRF.
- [ ] The confirmation step and single use are mandatory, not configurable: an opt-out would reopen login CSRF and replay for every application that uses it. The confirmation GET also stops mail-scanner link prefetching from consuming the token. List both in the release notes as intentional compatibility breaks: autologin emails need one extra click, and a link works only once.

### A4. Secret redaction and verification

- [ ] Remove raw secrets from success/failure/expiry messages, `parseLink` diagnostics, registration failures, and entity `toString`. Audit logs must redact secret-bearing path segments as well as query parameters for autologin, confirmation, and password-reset routes; replacing only the controller log is insufficient. Inspect direct raw URI logging and exception messages for these paths.
- [ ] Capture logs for successful, expired, malformed, nonexistent, and repeated tokens, plus cleanup. Assert raw secrets and complete bearer URLs do not occur; log only non-secret IDs/outcomes. Coordinate this narrow `AuditFilter` change with C.
- [ ] Run `bash YadaWebSecurity/gradlew -p YadaWebSecurity test :YadaWeb:test` and `bash YadaExamples/gradlew -p YadaExamples test classes`. All new authentication tests must execute, not report `NO-SOURCE` or skipped database tests. Run migration/schema and database integration checks against disposable data; commit the complete A branch.

## Group F: example application identity and impersonation

**Files:** `YadaExamples/src/main/java/net/yadaframework/example/web/dashboard/UserProfileController.java`, `YadaSession`-based impersonation in the example app, and affected user/profile DAOs. Views under `YadaExamples/src/main/webapp/WEB-INF/views/`: `header.html`, `dashboard/users.html`, `dashboard/usersLegacy.html`, `dashboard/userProfileEditForm.html`. Reuse `yada.impersonate(..., "POST")`, `YadaDataTableButton.dtMethod("POST")`, and `yada.postNavigate` where appropriate. Tests: `YadaExamples/src/test/java/net/yadaframework/example/web/dashboard/UserProfileSecurityTest.java`, using the Group 0 database fixture.

### F1. Profile identity

- [ ] Test a supervisor editing an allowed user while submitting an ADMIN credential ID/version, a different profile ID, and nested credential properties. Assert no ADMIN state changes, no relationship reassignment, and no changes committed before authorization. Include add-user and non-existent-ID paths.
- [ ] Stop binding persistent entities in example profile editing. Load the target profile and its credentials from trusted route/form selector data, authorize its stored roles before copying editable fields, authorize requested new roles, and copy permitted fields onto that same persistent identity in a transaction.
- [ ] IDs, versions, relationships, password hashes, and audit/security timestamps must never be copied. Retain intentional supervisor lockout-reset actions only through an explicitly authorized operation. Invalid requested profile IDs must fail, not silently create users.
- [ ] Reproduce with the actual Hibernate mappings and database constraints. Record whether the scan's exact overwrite succeeds on the baseline; even a blocked overwrite must have a regression test for the unsafe binding boundary.

### F2. Impersonation

- [ ] Convert example impersonation and deimpersonation routes to POST and update every calling button/link, including legacy tables.
- [ ] Validate `currentLocation` with `safeLocalRedirect` before saving and again before replay; preserve authorization via `userCanImpersonate`.
- [ ] Test GET produces no state change, missing/invalid CSRF is rejected, authorized POST works, and malicious saved locations fall back to `/`.
- [ ] Run `bash YadaExamples/gradlew -p YadaExamples test classes`; the new tests must execute. Commit F.

## Group B: DataTables server policy

**Files:** `YadaWeb/src/main/java/net/yadaframework/web/YadaController.java`, `persistence/YadaDataTableDao.java`, `web/YadaDatatablesRequest.java`, `YadaDatatablesColumn.java`, `YadaDatatablesOrder.java`, and `web/datatables/proxy/YadaDataTableProxy.java`; affected table configuration classes, `YadaExamples/.../dashboard/UserProfileController.java` custom table endpoint. Tests: create `YadaWeb/src/test/java/net/yadaframework/persistence/YadaDataTableSecurityTest.java` and extend `web/YadaControllerTest.java`.

**Interface:** Introduce an immutable server-owned `YadaDataTableQueryPolicy` in `net.yadaframework.persistence` and an overload of `getConvertedJsonPage(YadaDatatablesRequest request, Class<T> entityClass, Locale locale, YadaDataTableQueryPolicy policy)`. The policy maps column indexes/output names to exact entity paths plus search/sort permission and declares permitted extra output paths. Default controller obtains it from the configured table; custom/legacy endpoints supply their own policy. Remove the old `getConvertedJsonPage(request, entityClass, locale)` overload instead of making it fail closed at runtime: a runtime failure would compile cleanly in downstream applications and then break every custom endpoint in production, while a removed overload breaks at build time where the upgrade is visible. Document the replacement call in the upgrade notes.

- [ ] Add failing tests for tampered `name`, `data`, `extraJsonAttributes`, searchability/orderability, invalid order indexes, and direction values containing extra HQL. Include `userCredentials.password`, nested related entities, and valid but unconfigured fields. Confirm rejection occurs before querying or reflective conversion.
- [ ] Derive paths and permissions from the server table definition, never client flags. Accept only `asc`/`desc` case-insensitively and enforce upper/lower index bounds. Reject unknown columns and requested extras; syntax validation alone is insufficient because credential paths are valid Java paths. Keep trusted row filters and page-access checks in every default/custom endpoint.
- [ ] Remove query path fallback to `String.class` for unresolvable browser names. Validate configured paths; parameterize map keys rather than placing request text inside quoted HQL. Preserve null joins, localized maps/enums, computed browser renderers, and deterministic pagination without treating JavaScript render functions as entity attributes.
- [ ] Build JSON from policy-permitted fields. Do not return raw association objects that serialize additional properties; deny credential/token secret fields as a second safeguard. Retain trusted `DT_RowId`/`DT_RowClass` behavior and prevent browser requests from authorizing it.
- [ ] Add a Hibernate-backed test (Group 0 MySQL fixture) using a malicious direction/path payload and verify no unauthorized query or data retrieval; document the actual reproduced impact without assuming SQL UNION/RCE capabilities from HQL concatenation. Exercise both `/yadaDataTableData` and `/dashboard/user/userProfileTablePage`.
- [ ] Run `bash YadaWeb/gradlew -p YadaWeb test`. The `/dashboard/user/userProfileTablePage` regression tests run in `YadaExamples` (Group 0 test setup) after F is integrated, because F changes the same controller. Document the mandatory policy upgrade for downstream custom DAOs/endpoints and commit B.

## Group C: audit proxy trust

**Files:** `YadaWebSecurity/.../security/AuditFilter.java`, `YadaWeb/src/main/java/net/yadaframework/components/YadaWebUtil.java`, related `YadaConfiguration` settings and ingress documentation. Tests: create `YadaWebSecurity/src/test/java/net/yadaframework/security/AuditFilterSecurityTest.java` and `YadaWeb/src/test/java/net/yadaframework/components/YadaClientAddressTest.java`.

- [ ] Add failing tests for a direct client spoofing `X-Forwarded-For`, multi-hop chains, malformed addresses, newline/control characters, and trusted/untrusted immediate peers. Verify audit MDC never substitutes untrusted header text for the canonical address and contains no injected line breaks.
- [ ] Default to `request.getRemoteAddr()`. Prefer container-level trusted-proxy normalization: deployments run on Tomcat, so document Tomcat's `RemoteIpValve` with an explicit `internalProxies`/`trustedProxies` list matching the ingress, and the ingress overwriting (not appending to) client-supplied forwarded headers; do not install an unconditional forwarded-header filter. If framework-level parsing is necessary, use an explicit trusted-proxy allowlist and traverse the chain from the trusted right-hand end. No trust-all default.
- [ ] Keep the canonical address and any sanitized diagnostic forwarded value in separate fields. Update `getClientAddress/getClientIp` consistently or document that normalized servlet addresses are their sole input. Preserve A's endpoint/token redaction and review IP-dependent downstream callers.
- [ ] Run the focused tests and `bash YadaWebSecurity/gradlew -p YadaWebSecurity test :YadaWeb:test`. Verify direct and configured-proxy requests in an integration fixture, document ingress prerequisites, and commit C.

## Group D: security headers and multipart CSRF

**Files:** `YadaWebSecurity/.../security/YadaSecurityConfig.java`, `components/YadaAuthenticationFailureHandler.java`, `web/YadaMultipartFormTagProcessor.java`, `YadaActionUploadAttrProcessor.java`, security dialect/configuration, existing `security/uploadtest/core/` fixtures and tests. Inspect `YadaWeb/.../YadaCommonsMultipartResolver.java` and initializer/filter ordering before changing upload handling. Add `YadaSecurityDefaultsTest.java` under the security test package.

- [ ] Add failing tests for missing security headers and equal external failure responses for nonexistent users and wrong passwords. Test HTML and Ajax flags, response body/status, redirects, and absence of submitted passwords in request/model output. Preserve authorized password-change/lockout flows without revealing username existence through ordinary login errors.
- [ ] Remove blanket `.headers(...disable())`; restore Spring Security default writers and set `Referrer-Policy: same-origin`. Keep default frame denial unless an actual same-origin framing requirement is demonstrated; then configure that header alone. Verify HSTS only on secure requests. Static-resource caching: `YadaWebConfig.addResourceHandlers` sets long cache periods, and Spring Security's cache-control writer does not override a `Cache-Control` header already set by the response. Rely on that, and add a test proving that versioned resources, `/static`, content files, and `robots.txt` keep their configured `Cache-Control` while dynamic pages receive `no-cache`. If any resource path loses its caching, set its headers explicitly rather than disabling the cache writer globally. No framework template uses `<iframe>`, so keep the default `X-Frame-Options: DENY`.
- [ ] Set `hideUserNotFoundExceptions(true)` and normalize unknown-user/wrong-password failure presentation, including direct custom-handler invocation. Remove the unknown-user password attribute and unknown-user presentation flag. Retain safe internal diagnostics and avoid reintroducing timing differences through custom provider logic.
- [ ] Update active and legacy multipart processors so action URLs contain no CSRF value and existing queries/fragments remain intact. Headers are preferred for Ajax multipart uploads, using the existing Yada CSRF meta tags. Plain forms use a hidden field with multipart parsing before security validation; restrict action destinations to same origin so hidden/header transport cannot send a token elsewhere.
- [ ] Resolve oversized plain-form requests explicitly: when body parsing fails, a hidden CSRF value may be unavailable. Reject before application mutation and return the existing controlled upload-limit response (413), rather than disabling CSRF or putting it back in the URL. Header-authenticated oversized Ajax requests must still reach the appropriate limit handler. Use bounded multipart/temp storage and cleanup when parsing precedes authentication.
- [ ] Extend `OversizedMultipartUploadIntegrationTest` and `OversizedMultipartUploadLifecycleIntegrationTest`: rendered action contains no `_csrf`; valid small body/header submissions succeed; missing/invalid CSRF fails before mutation; valid-header oversized uploads preserve 413; oversized plain forms return a controlled error with no mutation; temp files are cleaned up. Replace the existing test that explicitly expects a CSRF query. Include max-size and malformed multipart cases in the real embedded Tomcat fixture.
- [ ] Run `bash YadaWebSecurity/gradlew -p YadaWebSecurity test :YadaWeb:test`. Verify headers on HTML, Ajax, redirects, and error responses through the actual filter chain. Record any intentionally changed oversized-form status behavior, update multipart documentation, and commit D.

## Group E: error response safety

**Files:** `YadaWeb/src/main/java/net/yadaframework/web/YadaController.java`, error/Ajax templates only where raw output is found. Extend `YadaWeb/src/test/java/net/yadaframework/web/YadaControllerTest.java`.

- [ ] Add a failing committed-response test with an exception containing attacker-style HTML/script text. Assert the raw value is never appended and response completion does not cause a second forward/error-dispatch loop. Also cover null message and writer/output-stream variants.
- [ ] Stop appending exception-derived text or experimental context-breaking closing tags after commit. Finish the response without rendering a second view; keep detailed diagnostics in server logs and use a generic message through the normal uncommitted error path. Audit Ajax/error templates for escaped output and use `th:text` for untrusted descriptions.
- [ ] Test uncommitted errors retain the original status and configured page, and Ajax errors cannot render executable exception text. Report this as removal of an unsafe sink unless a concrete reflected-XSS route is reproduced; do not claim a confirmed exploit from a mock exception alone.
- [ ] Run `bash YadaWeb/gradlew -p YadaWeb test`, review the combined `YadaController` diff with B, and commit E.

## Review focus and final acceptance

- Concurrent token requests across separate transactions/instances: A must demonstrate exactly one successful consumption, not just sequential replay rejection.
- Existing links, subclass rows, and downstream numeric-token API callers: A must show migration invalidation and documented API upgrades without changing user-account ownership.
- Encoded redirects and legitimate local query strings: Group 0 must test the validator; A and F must apply it at every exit and avoid extra URL decoding.
- Token API hand-off: A must show that reloaded entities cannot produce links and that every in-repo caller was migrated.
- Legacy/custom DataTables endpoints and nested output objects: B must prove they cannot bypass field policy or leak secrets through association serialization.
- Oversized native multipart uploads where the CSRF field cannot be parsed: D must demonstrate a controlled failure with no mutation and retained header-based upload behavior.

- [ ] Run the combined security/core/example tests and migration rehearsal. Check test reports show the new tests actually ran. A unavailable database or build dependency is a reported verification gap, not a passing security review.
- [ ] Review effective framework and example configuration, logs, and email/browser flows. Re-scan the integrated revision and compare by root cause rather than report severity alone.
- [ ] Update affected `aiknowledge` entries and Yada developer docs for changed contracts. Token dictionary entries must describe opaque bearer links rather than signed links. Report per-finding disposition with baseline evidence, implementing commit, regression test, and any deployment-dependent condition.

### Dismissed-finding coverage

| Dismissed entry | Root cause owner |
|---|---|
| JPQL/HQL injection through order direction | B / #4 |
| Arbitrary entity/related field output | B / #4 |
| Column name/data HQL injection | B / #4 |
| Registration/reset/autologin Math.random tokens | A / #1, #3; original weakness already fixed at baseline |
| Autologin action open redirect/login CSRF | A / #5 |

### Not addressed by this plan

These are outside the scan's findings and deliberately out of scope. Track them separately if wanted:

- User enumeration through password-reset and registration responses (D covers only the login form).
- Rate limiting of password-reset, registration, and autologin requests (email flooding, online guessing).
- Content-Security-Policy; D restores Spring Security's default headers and `Referrer-Policy` only.

### Security references

The token lifecycle follows [OWASP password recovery guidance](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html). Header defaults and targeted overrides follow [Spring Security response headers](https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html). Body/header CSRF transport and multipart ordering must be checked against the project's pinned version using [Spring Security CSRF guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html); the current documentation landing page may describe a newer release. These references support the design, not proof of exploitation in this application.
