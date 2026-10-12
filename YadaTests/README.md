# YadaTests

YadaTests owns every automated test of the Yada Framework. It is the eighth project of the repository-root build and uses the other framework projects as project dependencies, so the tests always exercise the checkout. It is never published and produces no archive. YadaExamples is not tested here.

A small Spring MVC application (`net.yadaframework.tests`, in `src/main`) provides real framework pages and persistence for the integration and browser suites.

## Suites

| Task | Source set | What it runs | Needs |
|---|---|---|---|
| `:YadaTests:test` | `src/test` | Pure unit tests of all modules | Nothing: no server, no database |
| `:YadaTests:integrationTest` | `src/integrationTest` | Embedded MariaDB lifecycle, the test application over HTTP, persistence, the upload regression tests | Native MariaDB binaries (bundled) |
| `:YadaTests:e2eTest` | `src/e2eTest` | Playwright browser tests against the test application | Chromium, installed once |
| `:YadaTests:toolTest` | `src/toolTest` | YadaTools unit tests and Gradle TestKit builds | Nothing |
| `:YadaTests:verifyDocs` | | Renders the documentation and checks its links | Nothing |
| `:YadaTests:frameworkCheck` | | All of the above, in that order | As above |
| `:YadaTests:frameworkCoverage` | | JaCoCo reports per framework module | Run after the suites |

Root `./gradlew test`, `check` and `build` run only the unit suite. The stateful suites run serially in one JVM, and each of them fails when it discovers no test.

## Commands

From the repository root (on Windows use `gradlew.bat`):

- `./gradlew test` or `./gradlew :YadaTests:test` for the unit tests;
- `./gradlew :YadaTests:integrationTest` for the database and HTTP tests;
- `./gradlew :YadaTests:installBrowsers` once, then `./gradlew :YadaTests:e2eTest` for the browser tests;
- `./gradlew :YadaTests:frameworkCheck :YadaTests:frameworkCoverage` for the full verification.

From the `YadaTests` folder, `../gradlew integrationTest` selects the same task. Select single tests with `--tests`, for example `./gradlew :YadaTests:integrationTest --tests '*EmbeddedDatabaseLifecycleTest'`; the zero-tests check is skipped for filtered runs.

`:YadaTests:installBrowsersCi` also installs the Chromium system packages and needs root: it is meant for CI.

Each production module still builds on its own, for example `./gradlew :YadaWeb:build`: no production module depends on YadaTests.

## Prerequisites

- Java 21, through the root wrapper.
- The embedded MariaDB (MariaDB4j 3.2.0, MariaDB 11.4) runs as the current user and needs no installation. On Linux it needs the usual system libraries (`libcrypt1`, `libsystemd0`), present on standard Ubuntu installations.
- On Linux and macOS the checkout path must not contain whitespace: MariaDB4j can't start MariaDB in such folders, and Yada refuses them with a clear message. `EmbeddedDatabaseWhitespacePathTest` fails as soon as a MariaDB4j release fixes this, with instructions to remove the check.
- The browser tests download Chromium with `installBrowsers`; they need no network access afterwards, since jQuery and Bootstrap are served from `src/main/webapp/static`.

## Reports and artifacts

All files stay in the ignored `YadaTests/build` folder:

- `reports/tests/<task>` and `test-results/<task>`: JUnit HTML and XML reports, including the application and database output of each test class;
- `e2e-artifacts/<class>-<test>`: screenshot and Playwright trace of each failed browser test (open the trace with `npx playwright show-trace`);
- `reports/frameworkCoverage/<module>`: coverage of each production module, in HTML and XML;
- `docs-verification`: the staged and rendered documentation;
- `schema/yts.sql` and `schema/V1__yadatest.sql`: the output of `:YadaTests:dbSchema` and `:YadaTests:webTestDbSchema`.

## Run isolation and cleanup

Each test run gets its own folder `build/runs/<run-id>` with the MariaDB binaries, data and temporary files, a free database port (never 3306 or 63306) and its own schema `yts_<run-id>`. The harness writes a dedicated Yada configuration for the run, so no developer configuration or credential is read. Two invocations at the same time don't share anything.

The harness stops only the processes it started, through `YadaMariaDBServer.stopOwnedDatabase()`, and deletes a run folder only after its database port is free. If a run is killed, its folder stays in `build/runs`: check that no `mariadbd` process uses it any more, then delete it or run `./gradlew :YadaTests:clean`. Never kill database processes that don't belong to a YadaTests run folder.

## The test application

- Configuration: `TestRun` writes `configuration.xml` and `conf.run.xml` in the run folder; the application settings are in `src/main/resources/conf.webapp.yts.xml`.
- Startup: `TestApplication` starts embedded Tomcat on the compiled application with exactly the application and security initializers, and parent-first class loading, so tests can call application beans directly.
- Schema: Flyway runs `src/main/resources/database/V001__baseline.sql`, and the application refuses to start without it. Generate the DDL of the entities with `./gradlew :YadaTests:dbSchema` and add a new migration for every change.
- Data: `DatabaseFixtures.reset()` empties every table except the Flyway history and reseeds the default users and articles through `TestDataService`. Browser tests reset before every test.
- The application sets no `<dialect>`: `YadaJpaConfig` chooses `MariaDBDialect` because the embedded database is enabled, and `PersistenceIntegrationTest` checks it.

`coverage-inventory.md` maps every original test to its suite, lists the covered behavior by module and the backlog.
