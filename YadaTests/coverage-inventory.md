# YadaTests coverage inventory

This file maps every automated test of the Yada Framework workspace to the central suite that owns it, records discovered counts, and lists the behavior that is not yet covered.

## Baseline before centralization

Root `./gradlew test` before YadaTests existed ran 114 tests, all passing, with no failure or skip.

| Module | Test class | Tests | Engine | Notes |
|---|---|---|---|---|
| YadaWeb | `net.yadaframework.components.YadaUtilTest` (with nested `CompareNaturalTests`, `JsonTests`, `NaturalSortOrderLocaleTests`) | 8 + 10 + 15 + 3 | Jupiter | Same-package access to `YadaUtil` |
| YadaWeb | `net.yadaframework.core.YadaConfigurationBootstrapViewTest` | 5 | Jupiter | In-memory XML configuration |
| YadaWeb | `net.yadaframework.core.YadaConfigurationSmtpSelectionTest` | 8 | Jupiter | In-memory XML configuration |
| YadaWeb | `net.yadaframework.persistence.YadaSavedPropertyCodecsTest` | 11 | Jupiter | |
| YadaWeb | `net.yadaframework.persistence.YadaSavedPropertyKeyTest` | 8 | Jupiter | |
| YadaWeb | `net.yadaframework.persistence.entity.YadaSavedPropertyTest` | 5 | Jupiter | |
| YadaWeb | `net.yadaframework.persistence.repository.YadaSavedPropertyDaoTest` | 14 | Jupiter + Mockito | Mocked `EntityManager`, pure unit test |
| YadaWeb | `net.yadaframework.web.YadaControllerTest` | 2 | Jupiter + Mockito | |
| YadaWeb | `net.yadaframework.web.YadaCropQueueTest` | 2 | Jupiter | |
| YadaWeb | `net.yadaframework.web.YadaPageRequestSortTest` | 4 | Jupiter | Three cases added centrally for the split sort parameter |
| YadaWeb | `net.yadaframework.web.YadaUserAgentTest` | 5 | Jupiter | |
| YadaWebSecurity | `net.yadaframework.core.YadaEmbeddedTomcatTestServerTest` | 3 | Jupiter | Package-private `resolveClasspathDirectories` of the fixture |
| YadaWebSecurity | `net.yadaframework.security.uploadtest.core.OversizedMultipartUploadIntegrationTest` | 3 | Jupiter | Embedded Tomcat, database disabled |
| YadaWebSecurity | `net.yadaframework.security.uploadtest.core.OversizedMultipartUploadLifecycleIntegrationTest` | 1 | Jupiter | Embedded Tomcat, reload-thread leak check |
| YadaWebCommerce | `net.yadaframework.commerce.persistence.entity.TestYadaMoney` | 1 | JUnit 4.12 | Converted to Jupiter centrally |
| YadaAi | `net.yadaframework.ai.components.bedrock.YadaAiRequestJsonTest` | 6 | Jupiter (BOM 5.13.4) | Uses `@TempDir`, no AWS credentials |

Totals: Web 100, Security 7, Commerce 1, Ai 6.

Fixtures: `YadaEmbeddedTomcatTestServer` and `YadaTestConfigurationScope` (package `net.yadaframework.core`) were published by `YadaWeb` through `java-test-fixtures` and consumed by Security.

Test resources: `YadaWeb/src/test/resources` (`configuration.xml`, `conf.webapp.test.xml`, `build.properties`, `META-INF/persistence.test.xml`) was read by no test; `persistence.test.xml` was the input of `YadaWeb:testDbSchema`. `YadaWebSecurity/src/test/resources` holds the upload application configuration and its `itest-webapp`.

A search for further tests (`test`, `collaudo`, `verifica`) found none outside these trees. `YadaWebCMS`, `YadaTools`, `YadaDocs` and `YadaBones` had no tests.

## Central ownership

Every test listed in the baseline lives in YadaTests, in its original package, and runs exactly once. Module `test` tasks have no sources (`NO-SOURCE`).

| Original location | Central suite | Discovered tests |
|---|---|---|
| YadaWeb `src/test` (11 classes) | `:YadaTests:test` | 100 |
| YadaWebSecurity `YadaEmbeddedTomcatTestServerTest` | `:YadaTests:test` (fixture in `src/harness`, same package) | 3 |
| YadaWebCommerce `TestYadaMoney` | `:YadaTests:test`, converted from JUnit 4 to Jupiter with unchanged assertions | 1 |
| YadaAi `YadaAiRequestJsonTest` | `:YadaTests:test`, without AWS SDK runtime types | 6 |
| YadaWebSecurity `uploadtest` package (2 test classes) | `:YadaTests:integrationTest` | 4 |

At the time of centralization the totals were 110 unit tests in `:YadaTests:test` and 4 upload tests in `:YadaTests:integrationTest`, matching the 114 of the baseline class by class. Later work extended several of these classes (`YadaControllerTest`, `YadaPageRequestSortTest`, the upload tests) and added the suites below; the counts in the baseline table are the original ones.

Relocated resources and fixtures:

- The upload application resources have per-scenario names in `src/integrationTest/resources`: `uploadtest-configuration.xml`, `conf.webapp.uploadtest.xml`, `uploadtest-build.properties` and `uploadtest-webapp/`. The YadaTests application uses `conf.webapp.yts.xml`, so no two suites share a classpath resource name.
- `YadaEmbeddedTomcatTestServer` and `YadaTestConfigurationScope` live in `src/harness/java/net/yadaframework/core`. YadaWeb no longer applies `java-test-fixtures`, and YadaWebSecurity no longer depends on YadaWeb test code.
- `YadaWeb/src/test/resources` (`configuration.xml`, `conf.webapp.test.xml`, `build.properties`) was read by no test and is not carried over.
- `persistence.test.xml` is replaced by `src/webTestDbSchema/META-INF/persistence.xml`, used by `:YadaTests:webTestDbSchema` (formerly `YadaWeb:testDbSchema`, which now fails with a pointer to the new task). Owner of the historical `YadaWeb/schema/V1__yadatest.sql`: `:YadaTests:webTestDbSchema`. The file is kept as it was: regenerating it with Hibernate 7 adds a unique constraint on `YadaJob.jobStateObject_id`.

## Suites added by YadaTests

| Suite | Class | Scope |
|---|---|---|
| `integrationTest` | `EmbeddedDatabaseLifecycleTest` | Fresh install, reopen, double stop, attach ownership, data-directory guard, failure after launch, failed Spring refresh, concurrent runs |
| `integrationTest` | `EmbeddedDatabaseSourceSqlTest` | `<sourceSql>` seed: applied once, `.loaded` rename of the copy only, invalid and unreadable files |
| `integrationTest` | `EmbeddedDatabaseWhitespacePathTest` | Rejection of folders with whitespace while MariaDB4j can't use them |
| `integrationTest` | `TestApplicationSmokeTest` | Every migration (V001 first) ran as SQL, rendering, CSRF, persisted fixtures, reset, schema after restart |
| `integrationTest` | `AutologinSecurityTest` | Autologin GET confirmation page without login or consumption, CSRF-protected single-use POST, foreign and cross-session POST rejection, unsafe actions redirected locally, other user's session preserved, expired/null/disabled/forged/digest/malformed tokens rejected, no raw secret stored |
| `integrationTest` | `RegistrationTokenSecurityTest` | Forged fields on signup and reset, single-use and expiring registration/recovery/email change links, replacement of previous links, purpose separation, default lifetimes, no raw secret stored, concurrent password changes |
| `integrationTest` | `SecurityTokenConsumptionTest` | Row-locked consumption: one of two concurrent transactions wins (registration and autologin), rollback keeps the token, purpose checks, reloaded tokens can't build links, issue/save refuse existing requests, autologin expiry and local actions |
| `integrationTest` | `TokenSecretRedactionTest` | No token secret in application logs for successful, expired, unknown, malformed and repeated tokens and request cleanup |
| `integrationTest` | `SecurityTokenMigrationRehearsalTest` | V001 to latest upgrade with legacy token rows: tokens deleted, accounts, roles and profiles kept, new columns |
| `integrationTest` | `YadaSecurityDefaultsTest` | Default security headers and `Referrer-Policy` on pages, Ajax responses, redirects and errors; static resource caching kept; unknown user and wrong password give the same page and the same Ajax response; lockout still stops the right password and reports the minutes left |
| `integrationTest` | `OversizedMultipartUploadIntegrationTest`, `OversizedMultipartUploadLifecycleIntegrationTest` | Multipart CSRF without a token in the url (hidden field or header), missing and invalid tokens rejected before the controller, 413 for oversized plain forms, oversized Ajax uploads reaching the controller, malformed multipart bodies, temporary files deleted, no reload-thread leak |
| `integrationTest` | `AuditDirectPeerTest`, `AuditConfiguredProxyTest` | Audit `remoteIp` is the peer address unless the peer is a configured trusted proxy; spoofed, multi-hop, text and control-character `X-Forwarded-For` headers; chain read from the right; `forwardedFor` sanitized |
| `integrationTest` | `ErrorResponseSafetyIntegrationTest` | Pages and Ajax errors carry only the HTTP reason phrase; a committed response never carries exception text |
| `integrationTest` | `SecurityEndpointsSmokeTest` | Login, registration, password recovery and autologin with emailed links, per-run settings, mailbox reset |
| `integrationTest` | `TestRunSettingsTest` | `TestRun.setConfigurationValue`: single and repeated values, attributes, removal, escaping, invalid paths |
| `integrationTest` | `SpelCompilerAcrossApplicationsTest` | Flake fix: Thymeleaf's SpEL compiler loader outlives the first application, so expressions compiled in a later application still load their classes |
| `integrationTest` | `TestApplicationStartupFailureTest` | Missing and broken schema fail startup and release the database |
| `integrationTest` | `YadaEmbeddedTomcatTestServerIntegrationTest` | Initializer allowlist, parent-first class sharing, failed start |
| `integrationTest` | `ConfigurationIsolationTest` | Start-close-start across two configurations: static configuration, application context and reload threads |
| `integrationTest` | `PersistenceIntegrationTest` | DAO insert/read/update/delete and rollback, foreign keys, saved-property uniqueness, persistent enums, YadaPageRequest queries, configuration, framework resources |
| `integrationTest` | `DataTableSecurityIntegrationTest` | DataTables query policy over HTTP on `/yadaDataTableData` and a custom endpoint: tampered columns, order directions and indexes, search flags and extra attributes rejected before any SQL, no credential output, page access, subclass-only columns of a configured table shown as output only (one warning, sort rejected), localized map keys bound with their type |
| `test` | `YadaDataTableSecurityTest` | `YadaDataTableQueryPolicy` path validation (explicit policies fail fast, derived policies make non-persistent columns output only), rejection before any query, HQL built only from policy paths, bound and non-colliding map keys, embeddables never returned, policy-only output |
| `test` | `YadaDataTableErrorSafetyTest` | A database failure while paging returns the generic `yadaError` "Server error" |
| `test` | `YadaAiRequestBuilderTest` | Claude and Nova request building: turns, tools, optional fields, parameter ranges |
| `test` | `YadaSecurityTokenCodecTest`, `YadaLogRedactionTest` | Token format, digest, strict parsing and expiry rule; secret redaction of urls, query strings and email bodies |
| `test` | `YadaClientAddressTest`, `AuditFilterSecurityTest` | Client address resolution from the peer, trusted proxies and `X-Forwarded-For` chains (IPv6 forms, malformed hops); audit MDC fields and token redaction in request lines |
| `test` | `YadaControllerTest`, `YadaGlobalExceptionHandlerTest` | Error dispatch keeps the status and shows the generic message; committed responses get no view and no text; the DataTables endpoint never reaches the DAO on denial and never serializes the policy |
| `test` | `YadaAuthenticationFailureHandlerTest` | Failure attributes for unknown user, wrong password, lockout (with and without a username), disabled user |
| `test` | `YadaActionUploadAttrProcessorTest` | `yada:actionUpload` adds the CSRF field only for absolute local paths; encoded separators, backslashes, control characters, relative and absolute urls get none |
| `test` | `YadaWebUtilRedirectTest` | `safeLocalRedirect` and `isSafeLocalRedirect` accept and reject lists |
| `test` | `YadaConfigurationDialectTest` | Dialect chosen by `YadaJpaConfig`: none by default, `MariaDBDialect` for the embedded database, configured `<database><dialect>` first |
| `e2eTest` | `AuthenticationE2eTest` | Login, failed login, logout, anonymous redirect, user and admin areas, CSRF acceptance and rejection |
| `e2eTest` | `AutologinConfirmationE2eTest` | Autologin confirmation page: no automatic submit or login, explicit confirmation works once and keeps the fragment, another user's session preserved |
| `e2eTest` | `ImpersonationE2eTest` | `yada.impersonate(..., "POST")` sends the local path, and a legacy `yada.dataTableCrud` button with `ajax: false` and `method: "POST"` submits a POST |
| `e2eTest` | `AjaxE2eTest` | yada.ajax.js fragment replacement without reload, Ajax notification modal |
| `e2eTest` | `FormE2eTest` | Bean Validation failure and success, notification after a normal request |
| `e2eTest` | `UploadE2eTest` | Upload within and above `maxFileUploadSizeBytes` |
| `e2eTest` | `ArticlesE2eTest` | Localized article names, pagination, sorting, editing with a committed write |
| `e2eTest` | `ShopE2eTest` | YadaMoney prices, cart, order with a persistent-enum status |
| `e2eTest` | `CleanupAfterFailureE2eTest` | A failing browser assertion leaves a screenshot and a trace and releases browser, application and database |
| `toolTest` | `YadaToolsPluginTest` | Plugin application rules, task registration, classpath file copy utilities |
| `toolTest` | `YadaToolsFunctionalTest` | TestKit: `yadaInit` skeleton generation, rerun without overwriting, missing properties, missing `war` plugin |

Discovered tests per suite at the end of the security remediation: `test` 388, `integrationTest` 159, `e2eTest` 26, `toolTest` 10, none skipped.

## Example application tests

`YadaExamples/src/test` is not part of YadaTests (`bash YadaExamples/gradlew -p YadaExamples test`, 44 tests): it checks the contracts of the example application with its own MockMvc and embedded database setup.

| Class | Tests | Scope |
|---|---|---|
| `ExampleTestSetupSmokeTest` | 6 | The setup itself: real login form, latest migration in the Flyway history, captured emails, CSRF post, per-test configuration override, seeded accounts |
| `SecurityTokenMigrationRehearsalTest` | 1 | V001 to V002 with legacy token rows, including the `YexRegistrationRequest` subclass |
| `RegistrationControllerAuthBindingTest` | 3 | Registration binding allowlist |
| `UserProfileIdentitySecurityTest` | 14 | Profile editing binds only allowlisted fields, POST-only writes, unknown ids fail, `failedAttempts` reset only to 0 |
| `UserProfileTableSecurityTest` | 8 | The example tables' query policies |
| `ImpersonationSecurityTest` | 11 | POST-only impersonation and deimpersonation, local saved location, context path round trip |
| `EmbeddedDatabaseStartupRetryTest` | 1 | A new port and folder when the database port is taken |

## Coverage by module

| Module | Central coverage | Backlog (scenario owner) |
|---|---|---|
| YadaWeb | Unit tests of utilities, configuration, DataTables query policy, error handlers, client address, redirect validation, saved properties, crop queue, page sort, user agent; embedded MariaDB lifecycle; persistence, persistent enums, YadaSql paging; resource handlers; Ajax, notifications, error page, YadaMoney | DataTables rendering in a browser (the scenario page doesn't render `<yada:datatable>`: its template needs a `userSession` bean), `YadaFileManager` and attachments (`UploadE2eTest`), jobs (`PersistenceIntegrationTest`), e-mail with a local fake (new mail scenario), `YadaLocalePathVariableFilter` with locale paths (new locale scenario) |
| YadaWebSecurity | Login, logout, generic failure flags, lockout, security headers, role-based access, CSRF, multipart before security (upload tests, 413), client address trust (audit tests), impersonation script behavior in a browser, user creation through `YadaUserCredentialsDao`, registration, password recovery, email change and autologin token flows (`net.yadaframework.security` integration tests) | Server-side impersonation endpoints belong to the application: the example application tests them (see below). Other applications' user-management endpoints are out of scope |
| YadaWebCMS | `YadaArticle` localized names, editing, paging and sorting | `YadaProduct` galleries and `YadaProductDao`, `YadaWebCmsDao.swapAttributes` (`ArticlesE2eTest`), image cropper views |
| YadaWebCommerce | `TestYadaMoney`, cart, order and order items, `YadaOrderDao.save`, `YadaOrderStatus` persistent enum | `YadaTransactionDao` and payments (with a local fake, `ShopE2eTest`), `YadaOrderDao.cleanup` |
| YadaAi | JSON shape and request building of Claude and Nova requests, response text extraction | Calls through the AWS client are never made; a local HTTP fake would be needed (`YadaAiRequestBuilderTest`) |
| YadaTools | Plugin rules, file utilities, `yadaInit` generation | `yadaDbSchema` execution: the deprecated task downloads hibernate-tools 5.6 into the test build and needs `javax.persistence` entities (`YadaToolsFunctionalTest`); deployment and SSH tasks are out of scope by design |
| YadaDocs | Not a code module: verified by `:YadaTests:verifyDocs` | Link checks across documents |
| YadaBones | Contains only `.gitignore` and `currently empty.txt`; it is not part of the root build | Nothing to test |

## Known framework issues found by the suites

- With MariaDB4j 3.2.0 (MariaDB 11.4), MySQL Connector/J 9.4 and Hibernate 7.1, Hibernate couldn't pick a dialect: Connector/J queries `INFORMATION_SCHEMA.KEYWORDS.RESERVED`, which MariaDB lacks. Fixed: `YadaJpaConfig` uses `MariaDBDialect` for the embedded database unless `<database><dialect>` says otherwise (`YadaConfigurationDialectTest`, `PersistenceIntegrationTest`).
- A single request parameter `sort=sku,desc` bound to `YadaPageRequest` was split at the comma by Spring and rejected by `YadaPageSort` ("No sort column specified in 'desc'"). Fixed in `YadaPageRequest.getPageSort()` (`YadaPageRequestSortTest`, `ArticlesE2eTest.singleSortParameterWithDirection`).
- All embedded webapps of a JVM share Thymeleaf under parent-first loading, and its SpEL compiler kept the class loader of the first application that rendered a page. After that application stopped, a later application failed with `NoClassDefFoundError: CompiledExpression` once an expression was compiled (after 100 evaluations). This is a harness property: a deployed webapp loads its own Thymeleaf. `YadaEmbeddedTomcatTestServer` now initializes the Thymeleaf evaluator with a loader that is never stopped (`SpelCompilerAcrossApplicationsTest`).
- The `entityManager` bean of `YadaJpaConfig` is created with `createEntityManager()` and is not bound to Spring transactions. Tests use a shared EntityManager instead.

## Threads and processes left by libraries

- MariaDB4j keeps `CommonsExec*` threads of a stopped process until the JVM exits; Tomcat reports them as possible leaks when a failed application stops. They are owned by MariaDB4j, not by the test code.
- `YadaJobScheduler` creates a thread pool that is not shut down when the context closes.
