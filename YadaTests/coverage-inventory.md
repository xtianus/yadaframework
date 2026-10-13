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

Totals: 110 unit tests in `:YadaTests:test` and 4 upload tests in `:YadaTests:integrationTest`, matching the 114 of the baseline class by class.

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
| `integrationTest` | `TestApplicationSmokeTest` | V001 migration, rendering, CSRF, persisted fixtures, reset, schema after restart |
| `integrationTest` | `TestApplicationStartupFailureTest` | Missing and broken schema fail startup and release the database |
| `integrationTest` | `YadaEmbeddedTomcatTestServerIntegrationTest` | Initializer allowlist, parent-first class sharing, failed start |
| `integrationTest` | `ConfigurationIsolationTest` | Start-close-start across two configurations: static configuration, application context and reload threads |
| `integrationTest` | `PersistenceIntegrationTest` | DAO insert/read/update/delete and rollback, foreign keys, saved-property uniqueness, persistent enums, YadaPageRequest queries, configuration, framework resources |
| `integrationTest` | `DataTableSecurityIntegrationTest` | DataTables query policy over HTTP on `/yadaDataTableData` and a custom endpoint: tampered columns, order directions and indexes, search flags and extra attributes rejected before any SQL, no credential output, page access |
| `test` | `YadaDataTableSecurityTest` | `YadaDataTableQueryPolicy` path validation, rejection before any query, HQL built only from policy paths, bound map keys, policy-only output |
| `test` | `YadaAiRequestBuilderTest` | Claude and Nova request building: turns, tools, optional fields, parameter ranges |
| `test` | `YadaConfigurationDialectTest` | Dialect chosen by `YadaJpaConfig`: none by default, `MariaDBDialect` for the embedded database, configured `<database><dialect>` first |
| `e2eTest` | `AuthenticationE2eTest` | Login, failed login, logout, anonymous redirect, user and admin areas, CSRF acceptance and rejection |
| `e2eTest` | `AjaxE2eTest` | yada.ajax.js fragment replacement without reload, Ajax notification modal |
| `e2eTest` | `FormE2eTest` | Bean Validation failure and success, notification after a normal request |
| `e2eTest` | `UploadE2eTest` | Upload within and above `maxFileUploadSizeBytes` |
| `e2eTest` | `ArticlesE2eTest` | Localized article names, pagination, sorting, editing with a committed write |
| `e2eTest` | `ShopE2eTest` | YadaMoney prices, cart, order with a persistent-enum status |
| `e2eTest` | `CleanupAfterFailureE2eTest` | A failing browser assertion leaves a screenshot and a trace and releases browser, application and database |
| `toolTest` | `YadaToolsPluginTest` | Plugin application rules, task registration, classpath file copy utilities |
| `toolTest` | `YadaToolsFunctionalTest` | TestKit: `yadaInit` skeleton generation, rerun without overwriting, missing properties, missing `war` plugin |

Discovered tests per suite: `test` 122 (110 relocated + 12 new), `integrationTest` 38 (4 relocated + 34 new), `e2eTest` 21, `toolTest` 10.

## Coverage by module

| Module | Central coverage | Backlog (scenario owner) |
|---|---|---|
| YadaWeb | Unit tests of utilities, configuration, DataTables query policy, saved properties, crop queue, page sort, user agent; embedded MariaDB lifecycle; persistence, persistent enums, YadaSql paging; resource handlers; Ajax, notifications, error page, YadaMoney | Datatables rendering in a browser (the scenario page doesn't render `<yada:datatable>`: its template needs a `userSession` bean), `YadaFileManager` and attachments (`UploadE2eTest`), jobs (`PersistenceIntegrationTest`), e-mail with a local fake (new mail scenario), `YadaLocalePathVariableFilter` with locale paths (new locale scenario) |
| YadaWebSecurity | Login, logout, failure flags, role-based access, CSRF, multipart before security (upload tests), user creation through `YadaUserCredentialsDao` | Registration and password reset (new registration scenario with a local mail fake), autologin links, lockout after failed attempts (`AuthenticationE2eTest`), impersonation |
| YadaWebCMS | `YadaArticle` localized names, editing, paging and sorting | `YadaProduct` galleries and `YadaProductDao`, `YadaWebCmsDao.swapAttributes` (`ArticlesE2eTest`), image cropper views |
| YadaWebCommerce | `TestYadaMoney`, cart, order and order items, `YadaOrderDao.save`, `YadaOrderStatus` persistent enum | `YadaTransactionDao` and payments (with a local fake, `ShopE2eTest`), `YadaOrderDao.cleanup` |
| YadaAi | JSON shape and request building of Claude and Nova requests, response text extraction | Calls through the AWS client are never made; a local HTTP fake would be needed (`YadaAiRequestBuilderTest`) |
| YadaTools | Plugin rules, file utilities, `yadaInit` generation | `yadaDbSchema` execution: the deprecated task downloads hibernate-tools 5.6 into the test build and needs `javax.persistence` entities (`YadaToolsFunctionalTest`); deployment and SSH tasks are out of scope by design |
| YadaDocs | Not a code module: verified by `:YadaTests:verifyDocs` | Link checks across documents |
| YadaBones | Contains only `.gitignore` and `currently empty.txt`; it is not part of the root build | Nothing to test |

## Known framework issues found by the suites

- With MariaDB4j 3.2.0 (MariaDB 11.4), MySQL Connector/J 9.4 and Hibernate 7.1, Hibernate couldn't pick a dialect: Connector/J queries `INFORMATION_SCHEMA.KEYWORDS.RESERVED`, which MariaDB lacks. Fixed: `YadaJpaConfig` uses `MariaDBDialect` for the embedded database unless `<database><dialect>` says otherwise (`YadaConfigurationDialectTest`, `PersistenceIntegrationTest`).
- A single request parameter `sort=sku,desc` bound to `YadaPageRequest` was split at the comma by Spring and rejected by `YadaPageSort` ("No sort column specified in 'desc'"). Fixed in `YadaPageRequest.getPageSort()` (`YadaPageRequestSortTest`, `ArticlesE2eTest.singleSortParameterWithDirection`).
- The `entityManager` bean of `YadaJpaConfig` is created with `createEntityManager()` and is not bound to Spring transactions. Tests use a shared EntityManager instead.

## Threads and processes left by libraries

- MariaDB4j keeps `CommonsExec*` threads of a stopped process until the JVM exits; Tomcat reports them as possible leaks when a failed application stops. They are owned by MariaDB4j, not by the test code.
- `YadaJobScheduler` creates a thread pool that is not shut down when the context closes.
