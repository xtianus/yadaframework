# YadaTests Implementation Plan

> **For agentic workers:** Use `executing-plans` to implement this plan task by task, or `subagent-driven-development` if the user selects delegated execution. Track progress with the checkboxes below. This document authorizes no implementation by itself.

**Goal:** Create `YadaTests`, a framework project in the repository-root Gradle build, as the home for all automated unit, integration, and browser e2e tests across the Yada framework workspace.

**Architecture:** `YadaTests` is the eighth project of the repository-root `yadaframework` build (see `2026-10-11-repository-gradle-root.md`). It consumes the other framework projects as project dependencies, so tests exercise the checkout, and it owns their test suites. A small Spring MVC application supplies realistic framework pages and persistence for integration and browser tests. Separate source sets isolate fast unit tests, embedded-database tests, browser tests, and Gradle tooling tests.

**Build layout baseline:** The root `settings.gradle` names the build `yadaframework` and includes `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools` and `YadaDocs`. The root has no `build.gradle`. There are exactly two wrappers, at the repository root and in `YadaExamples`. `shared.gradle.properties`, `loadSharedProperties.gradle` and `eclipseSetup.gradle` live at the repository root and are applied with `../loadSharedProperties.gradle` and `../eclipseSetup.gradle`. The root plan's pending relocation of these three files into `gradle/` is out of scope here; if it happens later, `YadaTests/build.gradle` changes its two `apply from` lines like every other module.

**Tech Stack:** Java 21 with `-parameters`, Gradle 8.14.3, Spring MVC 6, manual Hibernate/JPA, Thymeleaf, JUnit Jupiter, Mockito, MariaDB4j, embedded Tomcat, Playwright Java, JaCoCo, Gradle TestKit. No Spring Boot or Spring Data.

**Spec:** The design and constraints in this document implement the user's request. The authoritative database configuration reference is `YadaDocs/src/docs/asciidoc/en/database/embedded.adoc`.

## Delivery stages

- **Stage 1 (Tasks 1–3):** central build, embedded MariaDB lifecycle and the minimal application. It is a coherent increment that can be merged and verified on its own, and it contains the only production change (Task 2).
- **Stage 2 (Tasks 4–6):** relocation of existing tests, new regression and browser coverage, the Tools suite, reporting, docs and CI. Start it only after Stage 1 passes its review checkpoints, because every Stage 2 suite depends on the Stage 1 harness.

## Decisions

1. **Attachment identity guard in `YadaMariaDBServer` (production behavior change).** At present, when a port answers a JDBC probe, the component attaches silently, even if that server uses a different data directory. After Task 2 such a mismatch fails at startup. Silent attachment can create schemas in, or later stop, someone else's database. Document the change in the embedded database guide and release notes.
2. **YadaExamples is out of scope.** YadaTests tests the framework's reusable code. YadaExamples is a collection of usage patterns that is not meant to be included in another project, so it is neither part of the YadaTests build nor tested by it. It is only a reference to copy from: its Spring configuration classes, initializer and `dbSchema` task.
3. **Stage 1 is delivered first**, as described above.
4. **YadaTests is part of the root build.** It is included by the root `settings.gradle` and has no settings file or wrapper of its own. This keeps one framework wrapper and one Buildship owner per module directory. YadaExamples remains the only independent build.
5. **Root lifecycle tasks stay fast and environment-free.** Root `./gradlew test`, `check` and `build` run only the YadaTests unit suite in addition to the other modules' tasks. The MariaDB, browser, tooling and documentation suites run only through explicit tasks (`:YadaTests:integrationTest`, `:YadaTests:e2eTest`, `:YadaTests:toolTest`, `:YadaTests:verifyDocs`, `:YadaTests:frameworkCheck`). YadaTests applies no publishing plugin, so root `publish` still publishes exactly six libraries.

## Global constraints

- Name the project `YadaTests`, acronym `yts`, application package `net.yadaframework.tests`.
- All framework test implementations and test-only fixtures ultimately live in `YadaTests`. YadaExamples is excluded (Decision 2): it is never included in the build, never modified by this work, and gets no test suite.
- Production projects must not depend on YadaTests.
- Reference framework projects with `project(':YadaWeb')`-style dependencies of the root build, so tests exercise the checkout rather than published jars. Do not add `projectDir` remappings.
- `YadaTests/build.gradle` applies `../loadSharedProperties.gradle` and `../eclipseSetup.gradle` like the other Java modules, and keeps its plugin and dependency configuration in its own script (no convention plugins or version catalog).
- Every database-backed suite uses MariaDB4j's native embedded MariaDB through Yada's existing configuration and startup mechanism. No H2, external MySQL setup, or database containers.
- Pure unit tests start neither MariaDB nor Tomcat. The test application disables real email, cloud calls, payment calls, and scheduled background jobs, and substitutes controlled local fakes.
- Preserve existing production behavior except for the narrowly scoped lifecycle API, failed-launch cleanup, and attachment identity guard in Task 2 (see Decision 1). Do not upgrade the framework dependency stack as part of this work.
- Use tabs in code, English comments and Javadoc, existing jQuery and framework JavaScript, and SCSS if styling is needed. Browser tests use Playwright's Java binding, so no frontend build pipeline is needed.
- Use current shared Java/framework properties and pinned compatible test dependencies. For each new dependency, verify the latest stable compatible version on Maven Central, record it centrally and lock resolution. Do not copy the documentation's dynamic `3.+` dependency.
- Keep runtime files, database data, logs, screenshots, traces, and generated reports under ignored `YadaTests/build/`. Never read `/srv/yex*`, developer credentials, or personal overrides.
- Database and application suites initially run serially, with one fork and JUnit parallel execution disabled. Independent invocations use distinct run directories and ports. The embedded MariaDB port is never the default 3306 but a free port picked at run time (Task 2), so tests do not conflict with running applications.

## Repository findings

Each fact below is stated once; tasks refer to it rather than restating it.

- **F1 Build roots.** The repository-root build described in the build layout baseline owns all framework modules, including `YadaDocs`, which is a normal root project rather than an independent build. Running `../gradlew <task>` from a module directory selects that module's task; `./gradlew test` at the root runs every module's `test`. Eclipse imports modules under the root build with Buildship `connection.project.dir=..`, and one checkout cannot let both the root and YadaExamples own a module. Dependabot scans `/` and follows root settings includes, so it discovers `YadaTests/build.gradle` without configuration changes.
- **F2 Existing tests.** Root `./gradlew test` runs 114 tests, all passing: 100 Web, 7 Security, 1 Commerce and 6 Ai. Web has 11 test classes, all Jupiter plus Mockito. One of them, `YadaSavedPropertyDaoTest`, is a DAO test that mocks `EntityManager` and is still a pure unit test. Security has `YadaEmbeddedTomcatTestServerTest`, which is a pure path-resolution unit test. It also has an `uploadtest` package with two Tomcat-based integration tests and their initializer, configurations and controller. Commerce has `TestYadaMoney` on JUnit 4.12 (`org.junit.Test`). Ai has `YadaAiRequestJsonTest` on JUnit BOM 5.13.4, while Web and Security use BOM 6.0.3. The central baseline is 6.0.3, and the money test converts to Jupiter.
- **F3 Stale test dependency.** Web declares `testImplementation 'javax.servlet:javax.servlet-api:4.0.1'`, but no Web main or test source imports `javax.*`. Do not carry it into YadaTests unless compilation proves a need. It is the only javax servlet type in the test stack, so dropping it removes the javax/jakarta conflict risk.
- **F4 Fixtures.** `YadaEmbeddedTomcatTestServer` and `YadaTestConfigurationScope` live in `YadaWeb/src/testFixtures` (package `net.yadaframework.core`) and are consumed by Security through `testImplementation(testFixtures(project(':YadaWeb')))`. `YadaWeb` applies `java-test-fixtures` only for them, and `aiknowledge/workspace_build_notes.md` documents both the fixture dependency and the Eclipse test-code exposure it needs. `YadaTestConfigurationScope` resets `YadaAppConfig.CONFIG`, selects `yada.configurationFile`, and stops configuration reload threads. Relocation keeps their package so that package-private members such as `resolveClasspathDirectories` stay accessible.
- **F5 MariaDB startup.** `YadaMariaDBServer` is `@Component("yadaMariaDBServer")` and does all of its work in the constructor (lines 37–100):
  - If `baseDir` is empty, it installs with `DB.newEmbeddedDB` and calls `start()`.
  - Otherwise it builds the DB through `YadaMariaDB.openEmbeddedDB`, a `DB` subclass whose only job is to reopen an existing data folder without installing.
  - It then probes with `openConnection()`. If the probe succeeds, it attaches to the running process. If the probe fails, it calls `start()`.
  - Finally it calls `ensureSchema()` and, for a newly created schema, `loadDatabase()`. The latter reads `sourceSql` and may rename that file to `.loaded`; failures there are only logged.

  The component keeps `DB db` private, exposes only `getPort()`, and has no shutdown API.
- **F6 JPA wiring.** `YadaJpaConfig` (around line 150) looks up `yadaMariaDBServer` before building the pool and rewrites the JDBC host/port to localhost plus `getPort()`. If MariaDB4j is missing from the classpath, it logs an error and silently continues with the configured URL. The harness must therefore assert the MariaDB4j class is present and `getPort()` is non-null; otherwise a misconfigured classpath could fall back to an external database. It scans framework entity packages, so the application adds its own entity package.
- **F7 Web test schema.** `YadaWeb:testDbSchema` (`YadaWeb/build.gradle` around lines 177–202) runs `YadaCreateDbSchemaTask` with `hibernate-tools` 5.6.15. Its `hibtools` configuration mounts `classes/java/test`, and it temporarily copies `resources/test/META-INF/persistence.test.xml` to `classes/java/test/META-INF/persistence.xml` to produce `V1__yadatest.sql`. Moving `src/test` out of Web breaks all of these inputs.
- **F8 Tools.** `YadaTools` uses `compileOnly gradleApi(), localGroovy()` so that Gradle's embedded XML API classes do not leak into consumers. It does not apply `java-gradle-plugin`, so TestKit has no automatic plugin-under-test metadata. Web (and YadaExamples) apply the published `net.yadaframework:yadatools:0.7.7.R4` through `buildscript`; a project in the same root build cannot supply another project's buildscript classes.
- **F9 Security configuration types.** `YadaWebSecurityConfig extends YadaWebConfig`, and `YadaSecurityConfig` is a standalone configuration class. The dictionary has no dedicated entries for them, so their source and the Examples subclasses are the reference.
- **F10 Non-code modules.** `YadaBones` contains only `.gitignore` and `currently empty.txt` and is not part of the root build. `YadaDocs` owns documentation; its `copyDocInfoHeader` task writes into its source tree and its publication tasks write into the repository's `docs/`. Both appear in the coverage inventory without manufactured Java suites.

Three approaches were considered:

- Leaving tests distributed and adding a reporting aggregator does not satisfy central ownership.
- Copying all of YadaExamples would bring along unrelated pages, credentials and migrations.
- A central suite with a minimal application reuses the existing architecture and keeps fixtures small.

The plan chooses the third, as a project of the root build (Decision 4) rather than a third independent build with its own wrapper.

## Build and suite contract

The root `settings.gradle` gains `include 'YadaTests'`. `YadaTests/build.gradle` declares project dependencies on `:YadaWeb`, `:YadaWebSecurity`, `:YadaWebCMS`, `:YadaWebCommerce`, `:YadaAi` and `:YadaTools` per source set, and wires `:YadaDocs:verifyAsciidoc` for documentation verification. `YadaBones` has no build yet. YadaTests uses the `java` plugin, not `war`: Tomcat mounts its compiled outputs and `src/main/webapp` directly, so root `build` produces no application archive.

| Task / source set | Responsibility | Dependency boundary |
|---|---|---|
| `:YadaTests:test` / `src/test` | All pure Java unit tests | Framework libraries, Ai, Jupiter, Mockito, Spring test, harness classes (not started); no main output |
| `:YadaTests:integrationTest` / `src/integrationTest` | Database, HTTP/container and lifecycle regressions | Main + harness output; native MariaDB and Tomcat |
| `:YadaTests:e2eTest` / `src/e2eTest` | Browser behavior against the YadaTests application | Main + harness output, Playwright Java and Chromium |
| `:YadaTests:toolTest` / `src/toolTest` | YadaTools unit and Gradle functional tests | Tools outputs and runtime closure, Gradle API/Groovy, TestKit; isolated JVM |
| `:YadaTests:dbSchema` | Generate application DDL for migration review | Main entity graph, Jakarta persistence descriptor, `YadaSchemaGenerator` |
| `:YadaTests:verifyDocs` | Documentation build/link verification | `:YadaDocs:verifyAsciidoc`; no app runtime |
| `:YadaTests:frameworkCheck` | All central suites plus documentation | Explicit ordering: unit, then integration, then e2e |
| `:YadaTests:frameworkCoverage` | XML/HTML Java coverage by owning production module | Execution data from central Test tasks plus sibling production outputs |

Configuration rules:

- `main` consumes Web, Security, CMS and Commerce only. `harness` consumes Web plus Tomcat and configuration dependencies and has no dependency on main output. Its classes live in a custom `src/harness` source set that is never packaged into the application WAR or into production artifacts.
- Ai and Tools never enter a runtime configuration shared with the general suites. This keeps the AWS SDK and Gradle's fat API jar away from them. Add AWS SDK runtime types only if the migrated Ai test needs them.
- Jupiter/Mockito version constraints are shared through one small common configuration, not by inheriting `testRuntimeClasspath`.
- Each Test task sets `classpath` and `testClassesDirs` from its own source set, and consuming another source set's output depends on that producer's `classes` task.

Tools:

- Including Tools as a source project does not replace the published `yadatools:0.7.7.R4` buildscript plugin (F8). That plugin stays in place, and source-based Tools behavior is verified only through TestKit.
- Do not claim build-plugin substitution unless it is explicitly implemented and tested.

Running tasks:

- From the repository root, use project-qualified paths such as `./gradlew :YadaTests:integrationTest`. An unqualified `./gradlew test` at the root runs every module's `test`, which after Task 4 means the YadaTests unit suite plus any module that still has tests. From the `YadaTests` directory, `../gradlew integrationTest` selects only YadaTests tasks.
- `:YadaTests:check` keeps Gradle's default meaning (unit suite only), per Decision 5. `:YadaTests:frameworkCheck` is the canonical full verification. Each Test task writes its own JUnit XML/HTML report.
- `:YadaTests:installBrowsers` and `:YadaTests:installBrowsersCi` are JavaExec tasks that invoke `com.microsoft.playwright.CLI` with `install chromium` and `install --with-deps chromium`, respectively. Only the explicit CI task installs browser system packages.

Initial scope:

- Use Chromium only. Add Firefox/WebKit only after the central suite is stable.
- Add no coverage percentage gate until a baseline exists.
- Browser JavaScript coverage is outside JaCoCo's scope.

## File map

Create:

- `YadaTests/{build.gradle,gradle.lockfile,README.md}`: project dependencies, suite wiring, dependency pins and usage. No settings file, wrapper or `gradle.properties` (Decision 4); test-only dependency versions are pinned in `YadaTests/build.gradle`.
- `YadaTests/src/main/java/net/yadaframework/tests/core/{AppConfig,WebConfig,JpaConfig,SecurityConfig,WebApplicationInitializer}.java`: thin application configuration adapted from Examples.
- `YadaTests/src/main/java/net/yadaframework/tests/components/TestDataService.java`: explicit application fixtures and reset contract.
- `YadaTests/src/main/java/net/yadaframework/tests/persistence/entity/UserProfile.java`: minimal concrete security profile following Examples' entity relationships.
- `YadaTests/src/main/java/net/yadaframework/tests/web/ScenarioController.java`: small pages/actions exercising actual framework APIs.
- `YadaTests/src/main/resources/{configuration.xml,conf.webapp.test.xml,logback.xml,META-INF/persistence.xml,database/V001__baseline.sql}`: safe test-only configuration and full application schema.
- `YadaTests/src/main/webapp/WEB-INF/{web.xml,views/test/*}`: minimal Thymeleaf scenarios using real Yada resources.
- `YadaTests/src/harness/java/net/yadaframework/tests/support/{TestRun,TestApplication,DatabaseFixtures}.java`: run configuration, application ownership, fixture reset.
- `YadaTests/src/harness/java/net/yadaframework/core/{YadaEmbeddedTomcatTestServer,YadaTestConfigurationScope}.java`: relocated fixtures in their original package (F4).
- Central test classes/resources under the five source sets above, in their original packages where package-private access requires it.
- `YadaTests/coverage-inventory.md`: module/scenario ownership, migrated tests, discovered counts and untested behavior.
- `.github/workflows/yada-tests.yml`: source-based verification and retained reports.

Modify:

- Root `settings.gradle`: add `include 'YadaTests'`.
- `YadaWeb/src/main/java/net/yadaframework/components/YadaMariaDBServer.java` (and `YadaMariaDB.java` only if needed): owned-process shutdown, constructor-failure cleanup, attachment identity guard.
- Build files that declare migrated test dependencies, fixtures or schema inputs: Web (including its `java-test-fixtures` plugin and `testDbSchema`), Security (its `testFixtures` dependency), Commerce and Ai, plus any other module the inventory finds.
- `YadaWeb/src/testFixtures/*` and original test trees: move only after the central replacements pass.
- `YadaDocs/build.gradle`: a separate staged `verifyAsciidoc` task that writes no tracked files.
- `README.md`, `yadaDeveloper2.code-workspace`, `aiknowledge/workspace_build_notes.md` and the embedded database documentation: root-qualified test commands, the eighth root project, IDE entry and lifecycle instructions. `.github/dependabot.yml` needs no change (F1).

Template fixture resources for migrated upload tests belong in `src/integrationTest/resources` at their existing classpath paths. Do not merge the colliding `configuration.xml`/`build.properties` files. Each suite selects its configuration explicitly.

## Review focus

1. Failed startup or failed tests release only their owned MariaDB process and ports (Tasks 2 and 3).
2. Two independent runs and paths containing spaces never share state or attach to someone else's database (Task 2).
3. Static configuration, Spring contexts and initializer discovery do not contaminate another suite (Tasks 3 and 4).
4. Relocated tests that need package-private access, and the converted JUnit 4 assertions, still compile and execute (Tasks 1 and 4).
5. Database resets remove committed browser writes and dependent rows while retaining Flyway metadata (Tasks 3 and 5).

## Task 1: Establish central build and migration inventory

**Files:** build/wrapper files, `coverage-inventory.md`, `src/test`. Inspect all sibling `src/test` and `src/testFixtures` trees.

**Interfaces:** Implements the suite contract above. Unit suites do not depend on harness startup, and sibling dependencies are production outputs, never sibling test sources.

- [x] Record test classes, methods (including parameterized invocations), engine requirements, resources, package-private access, fixtures, and actual baseline results for every module. Start from F2, then search for more with English and Italian keywords (`test`, `collaudo`, `verifica`). Record existing failures explicitly so they are not counted as migration losses later.
- [x] Add `include 'YadaTests'` to the root settings and create `YadaTests/build.gradle` with shared property loading and Eclipse setup (F1). Declare project dependencies and source sets, keeping Tools isolated. As the first executable proof, copy one pure unit test, preserving its package and assertions.
- [x] Run `./gradlew projects` and `./gradlew :YadaTests:test --tests 'net.yadaframework.persistence.YadaSavedPropertyKeyTest'` at the root. Expected: eight framework projects, checkout sources compile, the named tests run, and no native process or server starts. The `test` configuration must not borrow every sibling's test dependencies.
- [x] Run `./gradlew build --dry-run` and `./gradlew publish --dry-run -PrepoPath=<scratch>` at the root. Expected: no YadaTests integration, e2e, tool or docs task, no YadaTests archive or publication, and still exactly six library publications (Decision 5). YadaExamples' `projects` output is unchanged.
- [x] Configure the task graph, per-suite classpaths, JUnit engine/launcher alignment, one fork for stateful suites, and dependency locking. Once a suite has its first tests, it fails when it discovers zero. Suites that are intentionally empty during bootstrap are recorded as such.
- [x] Check runtime classpaths for duplicate framework versions, Gradle API leakage, and any javax servlet type (F3 expects none). `:test` must not load Gradle's fat API jar. Verify the root wrapper works on native Ubuntu and Windows/WSL without privileged application directories.
- [x] Review checkpoint: the build graph resolves without cycles, and the inventory identifies every existing test before anything is deleted.

## Task 2: Own embedded MariaDB lifecycle and run isolation

**Files:** `TestRun.java`, `TestApplication.java`, application config XML, `YadaMariaDBServer.java`. Tests: `src/integrationTest/java/net/yadaframework/tests/support/EmbeddedDatabaseLifecycleTest.java` and `EmbeddedDatabaseSourceSqlTest.java`.

**Interfaces:**

- `TestRun.create(Path buildDirectory)` returns an AutoCloseable run with `Path directory()`, `Path configurationFile()` and `int databasePort()`.
- `TestApplication.start(TestRun run)` returns an AutoCloseable application with `URI baseUri()`.
- New production API `YadaMariaDBServer.stopOwnedDatabase()` stops a process that this instance started. It is idempotent and reports shutdown failure as `YadaSystemException`. An instance that attached to an already-running process does not own it.

Tests first:

- [x] Write lifecycle tests covering these cases:
  - a fresh directory starts native MariaDB, and JDBC `SELECT VERSION()` succeeds;
  - restarting after a graceful stop reopens the existing data through the `YadaMariaDB` path;
  - closing twice is harmless;
  - a failed Spring startup still stops an owned database;
  - an attached instance cannot stop the owner's process.

  The full application only exists after Task 3. For the failed-startup case, use a minimal root context of `YadaAppConfig` and `YadaJpaConfig` that is deliberately broken (for example, an invalid Vibur setting), so the refresh fails after MariaDB has started. This keeps the ownership contract fully tested within Stage 1.
- [x] The stop and ownership cases cannot compile against the current API. Add `stopOwnedDatabase()` as a stub, confirm those cases fail, and only then implement it. The fresh-start and reopen cases may already pass; they guard against regressions.

Ownership and shutdown (F5):

- [x] Track ownership as "this instance called `start()`", recorded before the call, and track attachment separately. The attach path never calls `start()`, so it never owns the process. `stopOwnedDatabase()` calls `DB.stop()` only for owned instances, and that works the same for the `DB.newEmbeddedDB` and `YadaMariaDB.openEmbeddedDB` paths.
- [x] Handle a failure after launch but before the bean is registered (for example in `ensureSchema()` or `loadDatabase()`): attempt owned cleanup in the constructor's catch block, keep the original error as the primary exception, and attach cleanup errors as suppressed. Verify the pinned MariaDB4j behavior for stop-after-partial-start.
- [x] Add no automatic shutdown to production contexts, because developers rely on the documented workflow of keeping the database process running. Do not use reflection or a global process-kill command. If the stop contract needs more access, extend `YadaMariaDB`, which is already a `DB` subclass under framework control.

Run directory and schema naming:

- [x] Generate absolute per-run paths `build/runs/<unique-id>/embeddedDB/{MariaDB,data,tmp}` and a word-character schema `yts_<run-id>`. Unique directories are what keep concurrent invocations and repeated runs from sharing state. The tests must include a path containing spaces.

Port allocation:

- [x] Never use the default MariaDB/MySQL port 3306, or any embedded port configured by a developer application. That way, running tests never conflicts with an application or database already running on the machine. Instead, each run picks a free, unprivileged port from the available ones and passes it in the configuration. Do not use port zero: Yada rewrites the JDBC URL using the configured port, so zero would produce an unusable URL. If startup loses the allocation race, retry with a fresh run directory and port.

Attachment identity guard (Decision 1):

- [x] In `YadaMariaDBServer`, immediately after a successful attachment probe and before `ensureSchema()`, compare the normalized `SELECT @@datadir` with the configured data directory. On mismatch, abort without deleting files, creating a schema, or stopping that server. The guard must live inside the component, because a post-construction harness check would run after `ensureSchema()` has already touched the foreign server.

Configuration shape:

- [x] Write the run's configuration to its own file and set `yada.configurationFile` before any static configuration access. The configuration has:
  - `<database enabled="true">`;
  - `<embedded enabled="true" port="...">` with `baseDir`, lowercase `datadir`, and `tmpDir`;
  - framework properties;
  - a datasource `jdbc:mysql://localhost:<port>/yts_<run-id>` with every Vibur setting `YadaJpaConfig` reads;
  - test-only root/empty embedded credentials;
  - mail and jobs disabled;
  - no personal or localdev overrides.
- [x] Use the documented MariaDB4j dependency with `spring-core` excluded; the repository baseline is 3.2.0.

Harness ownership:

- [x] Override the scanned `yadaMariaDBServer` bean with a test-only subclass/factory registered under the same name. It captures the instance as soon as it is constructed and keeps it outside the Spring context, so a failed refresh cannot lose the cleanup handle. Assert the MariaDB4j class is present and `getPort()` is non-null, so a misconfigured classpath fails instead of silently using the configured URL (F6).
- [x] Teardown runs in this order: stop HTTP acceptance; close contexts, the EntityManagerFactory and pools; stop owned MariaDB; restore the configuration scope; verify the port is released. Cleanup continues past individual failures and keeps diagnostics. Run data is deleted only after shutdown is confirmed.

`sourceSql`:

- [x] Add `EmbeddedDatabaseSourceSqlTest` using a copied SQL file inside the run directory. Verify that:
  - first creation applies the seed;
  - the `.loaded` rename affects only the copy;
  - reopening does not replay the seed;
  - invalid or unreadable SQL is detected by explicit schema/data assertions, because the component only logs the error.

  Keep this suite separate from normal Flyway migrations.

Verification:

- [x] Run `./gradlew :YadaTests:integrationTest --tests '*EmbeddedDatabaseLifecycleTest' --tests '*EmbeddedDatabaseSourceSqlTest'`. Verify:
  - fresh, reopen, attach and failure behavior;
  - an occupied port serving a different data directory;
  - two concurrent independent invocations;
  - paths containing spaces;
  - no leftover processes or ports.

  Run on Ubuntu as a normal user. Document native binary dependencies, and verify platform support before enabling each OS CI job.
- [x] Review checkpoint: the lifecycle changes are minimal, and normal application startup behaves as before except for the guard in Decision 1.

## Task 3: Build the minimal application and deterministic schema/fixtures

**Files:** application configuration classes, minimal entity/controller/templates, persistence descriptor, baseline migration, `DatabaseFixtures.java`, `TestDataService.java`. Tests: `src/integrationTest/java/net/yadaframework/tests/support/TestApplicationSmokeTest.java`.

**Interfaces:**

- `DatabaseFixtures.reset()` clears scenario data and reseeds deterministic users and content through transactional application DAOs.
- `TestDataService` provides fixture creation to Java tests.
- `TestApplication.baseUri()` comes from the existing Tomcat fixture's `getBaseUri()`.

Smoke tests first:

- [x] Write smoke tests asserting:
  - a real rendered page;
  - a usable CSRF token;
  - a user fixture that is persisted and read back;
  - an unchanged schema after restart;
  - that the SQL `V001` migration actually succeeded, by querying Flyway history. An automatic baseline row does not prove the migration ran.
  - that an application with a missing or broken schema fails startup visibly rather than serving an empty homepage.

Application wiring:

- [x] Adapt Examples' root/servlet separation and initializer using `YadaAppConfig`, `YadaJpaConfig`, `YadaWebSecurityConfig` and `YadaSecurityConfig` (F9). Keep component scans narrow and configure the concrete security profile. Load the real framework scripts, Thymeleaf dialects, security filters and resources so that observed behavior is genuine.

Tomcat and configuration harness (F4):

- [x] Reuse the relocated fixtures and mount the application's compiled classes and resources explicitly. Extend `YadaEmbeddedTomcatTestServer` with an explicit initializer-class allowlist, and verify that exactly one initializer runs even when several share a mounted directory. Keep the overloads that the migrated tests need, and test the classpath/resource mounting instead of assuming `src/main/webapp` is enough.
- [x] Add:
  - run-directory injection for Tomcat files;
  - `getRootApplicationContext()` for service access;
  - context-health checks;
  - cleanup after a partial `tomcat.start()` failure.

  Before calling `TestDataService` in-process, confirm that test code and the mounted application share compatible defining classloaders.
- [x] Snapshot and restore `YadaUtil.applicationContext` along with `YadaAppConfig.CONFIG` and the system property, so that a closed context never remains globally reachable.

Schema generation:

- [x] Define the complete persistence descriptor and `:YadaTests:dbSchema` as a JavaExec running `net.yadaframework.tools.YadaSchemaGenerator` for persistence unit `yadaPersistenceUnit`. It writes `build/schema/yts.sql`, uses script-only generation, and disables every database action, so no running DB is needed. `YadaSchemaGenerator` lives in YadaWeb's main sources, so it is already on the main runtime classpath, and the published `yadatools` jar is not needed. Follow the Examples `dbSchema` task, but write under `build/` and verify the setup against the current Hibernate. Keep the generator's classpath and resources separate from app startup.
- [x] Run `./gradlew :YadaTests:dbSchema` and inspect the complete DDL, covering core, Security, CMS, Commerce and test entities. Commit a new `V001__baseline.sql`. Do not copy the outdated Examples baseline or edit existing module migrations.

Startup ordering:

- [x] Normal startup uses Flyway through the existing `YadaAppConfig.init()` migration, never `sourceSql` plus duplicate DDL. Hibernate may build metadata early, but schema generation and validation stay disabled. No constructor or init callback may query tables before migration. Fixtures are accessed only after a successful root refresh and a verified V001. Test this ordering explicitly.

Fixture reset:

- [x] Reset with an explicit foreign-key dependency order, or with application DAO deletions in committed transactions, and preserve migration history. Browser requests commit their own transactions, so test-thread rollback cannot undo them. Reset tests start from committed cart, article and user data and assert a clean state plus deterministic reseeding. Expose no reset endpoint: Java browser fixtures call the service in-process.

Verification:

- [x] Run `./gradlew :YadaTests:integrationTest --tests '*TestApplicationSmokeTest'`. Expected: native MariaDB, migrations, rendering, security and persistence pass twice from clean directories, with cleanup verified.
- [x] Review checkpoint: each scenario exercises real framework behavior, and the app has no external-service or example-business dependency. **End of Stage 1.**

## Task 4: Relocate every existing automated test and fixture

**Files:** all inventoried test files and resources, central test trees, affected module build files, Web's `testDbSchema`.

**Interfaces:** Original Java packages and test behavior are preserved, and the central tasks become the only test implementations. Module production compilation stays independent of YadaTests.

- [x] Move all Web tests (F2, including the mocked `YadaSavedPropertyDaoTest`) and the Ai test into `src/test/java` with their packages unchanged. Move `TestYadaMoney` there as well, convert its JUnit 4 imports and annotations to Jupiter, and preserve the rounding and localization assertions. Add only the dependencies these tests need, and leave out `javax.servlet-api` (F3).
- [x] Move `YadaEmbeddedTomcatTestServerTest` into `src/test`. It only resolves paths, so it needs harness classes on its classpath, but no server starts. Its fixture keeps its package so that `resolveClasspathDirectories` stays accessible. Inventory other same-package access before renaming any test.
- [x] Move Security's `uploadtest` package, including its initializer, configurations, controller, views and two integration tests, into `src/integrationTest`. Keep this upload application isolated from the database-enabled application, with its database explicitly disabled as in the original regression.
- [x] Copy classpath resources under per-scenario names, and select absolute configuration files through configuration scopes. Cover start-close-start across two configurations, and check that no reload-trigger threads remain. Match the original webapp and extra-classpath mounting of the upload tests.
- [x] Run `./gradlew :YadaTests:test :YadaTests:integrationTest` and compare named results and discovered counts with the Task 1 baseline. Investigate every missing, skipped or changed assertion. Remove the original tests, resources and fixture publication only after equivalence is shown. Remove Security's testFixtures dependency before removing Web's fixture source set and `java-test-fixtures` plugin, and drop the now-unused test dependencies from the module build files.
- [x] Root `./gradlew test` then runs the 110 relocated unit tests through `:YadaTests:test` only, and the 4 relocated upload integration tests run through `:YadaTests:integrationTest`. No test runs twice.
- [x] Resolve `YadaWeb:testDbSchema` (F7): move `persistence.test.xml` and the test-schema responsibility to YadaTests and update documentation and consumers. Then either remove the Web task or replace it with a clear message pointing at the YadaTests task. It must not silently mount a `classes/java/test` directory that no longer contains the descriptor. Keep this old test schema separate from the new application baseline.
- [x] Verify with `./gradlew -p <Module> build` and with YadaExamples' `./gradlew build` that each production library still builds without YadaTests in its dependency graph, which shows centralization added no reverse dependency. Run `./gradlew cleanEclipse`, then `./gradlew eclipse`, and check that the Security classpath no longer needs YadaWeb test code. Keep historical schema files if compatibility or documentation needs them, and record their owner.
- [x] Review checkpoint: the inventory maps each original test to exactly one central suite with equivalent execution. Document that module-level `test` tasks no longer own tests.

## Task 5: Add cross-project regression coverage and browser e2e

**Files:** scenario controller/templates, module-specific tests under central trees, `coverage-inventory.md`.

**Interfaces:** Browser instances stay on their owning thread. Each scenario gets a fresh BrowserContext and a deterministic database reset. The harness owns the application and database lifetime even when an assertion fails.

- [x] Add integration cases against embedded MariaDB for:
  - DAO insert/read/update/delete and rollback;
  - foreign-key behavior;
  - saved-property uniqueness;
  - core configuration and resource behavior.

  Assert real persistence constraints rather than using a database mock.
- [x] Add Chromium e2e cases for:
  - login and logout;
  - authorized and forbidden access;
  - CSRF rejection and acceptance;
  - Ajax fragment replacement and notifications;
  - validation failure and success;
  - uploads and oversize errors;
  - pagination and sorting;
  - CMS content rendering and editing;
  - commerce cart, price and order persistence.

  Use public framework APIs after reading their dictionary entries and source, and add only thin scenario endpoints where needed to observe a behavior.
- [x] Use Playwright Java with manual JUnit lifecycle hooks, stable role/label/test-id locators, condition-based waits and fresh browser contexts. On failure, capture a screenshot, a trace, and backend and native DB logs. Close the browser, context and Playwright during cleanup. Verify that a deliberately failing browser assertion still releases native and server resources. Do not use arbitrary sleeps, and do not enable default retries that would hide flaky tests.
- [x] Add Ai serialization and request-building tests that use local fixtures and no AWS credentials.
- [x] Add YadaTools unit tests and TestKit functional cases in temporary project directories. They exercise the generation and schema tasks, never deployment, SSH, publishing or destructive tasks. Build the TestKit plugin classpath explicitly from Tools outputs and runtime dependencies (F8).
- [x] Update the inventory for Web, Security, CMS, Commerce, Ai, Tools, Docs and Bones (F10). Each active code module gets representative high-value coverage, and remaining public behavior goes into a coverage backlog with named scenario owners. Having all tests live centrally does not mean every method is covered.
- [x] Run `./gradlew :YadaTests:installBrowsers :YadaTests:test :YadaTests:integrationTest :YadaTests:e2eTest :YadaTests:toolTest` twice from fresh state. Expected:
  - every active suite discovers tests;
  - assertions are deterministic;
  - there are no cloud calls;
  - no owned process or thread is left behind.
- [ ] Review checkpoint: each new regression first fails on a behavioral assertion and then passes through real framework code. No unrelated production refactoring is introduced.

## Task 6: Reporting, documentation and CI acceptance

**Files:** central build reporting, workflow, README/workspace entries, embedded DB documentation, coverage inventory.

**Interfaces:**

- `:YadaTests:frameworkCheck` is the canonical full verification command.
- `:YadaTests:frameworkCoverage` reports sibling production classes and excludes the harness and the scenario app.
- `:YadaTests:verifyDocs` depends on `:YadaDocs:verifyAsciidoc` in the same root build.

Coverage:

- [x] Configure JaCoCo on each central Test task with its own execution-data file. Aggregate that data against sibling `sourceSets.main` class and source directories, including Tools' Groovy output, and exclude the harness and the scenario app. Verify that a known Web method and a known Security method exercised only by central tests show as covered. In-process server code is instrumented because the harness runs Tomcat in the test JVM. Tools subprocess executions need explicit instrumentation or a documented exclusion.

Documentation verification:

- [x] Add `YadaDocs:verifyAsciidoc`. It stages the AsciiDoc sources, resources and docinfo under an absolute `YadaTests/build/docs-verification` path supplied by the central build, and renders HTML there.
  - It takes tracked doc sources as inputs.
  - It must not depend on `copyDocInfoHeader`, because that task writes into the source tree (F10), and it must not emit to the repository's `docs/`.
  - Existing publication tasks stay as they are.
- [x] Wire `:YadaTests:verifyDocs` to `:YadaDocs:verifyAsciidoc`. Add reference/link validation where rendering does not catch broken links, and assert that tracked file hashes are unchanged afterwards. Tooling failures are actionable setup errors, not skipped successes.

CI:

- [ ] Create an Ubuntu job with Java 21, the root wrapper, Gradle caching, MariaDB native prerequisites, and explicit Playwright Chromium and system-dependency installation.
  - Run without a database service or container and without production credentials.
  - Use timeouts, always upload test, coverage, trace and native logs, and always attempt owned-process cleanup.
  - Validate cleanup after cancellation and timeout on an isolated runner.
  - Add Windows/WSL jobs only after the embedded binaries and cleanup are verified there.

Developer docs:

- [x] Document, from the repository root:
  - `./gradlew test` or `./gradlew :YadaTests:test` for unit tests;
  - `./gradlew :YadaTests:integrationTest` for database and HTTP tests;
  - `./gradlew :YadaTests:installBrowsers` once for local browser setup;
  - `./gradlew :YadaTests:e2eTest` for browser tests;
  - `./gradlew :YadaTests:frameworkCheck :YadaTests:frameworkCoverage` for full verification.

  Also cover `gradlew.bat` equivalents, `../gradlew <task>` from the `YadaTests` directory, native MariaDB prerequisites, report locations, selective `--tests`, safe cleanup rules, per-module builds and central suite ownership.
- [x] Add YadaTests to the workspace metadata and to `aiknowledge/workspace_build_notes.md` (eight root projects, no fixture dependency between modules). In the embedded DB guide, document run isolation, the ownership/stop API, the attachment guard (Decision 1) and disposable `sourceSql` behavior. Keep the existing development workflow.

Acceptance run:

- [x] Run `./gradlew :YadaTests:clean :YadaTests:frameworkCheck :YadaTests:frameworkCoverage`, then a second fresh run. Validate JUnit XML counts and module coverage attribution. Run `git diff --check` and inspect the changed files for unintended build or deployment changes. Claim success only after the native database and browser suites have actually executed.
- [x] Review checkpoint: every acceptance criterion below is backed by test or report evidence. Commit logical increments only under the execution session's Git instructions.

## Acceptance criteria

- YadaTests is the eighth project of the repository-root build, has no settings file or wrapper of its own, and uses project dependencies for every active Yada framework code project (YadaExamples excluded).
- Root `test`, `build` and `publish` keep their scope: unit tests only, no YadaTests archive, six library publications.
- Every pre-existing automated test is mapped, relocated and executed centrally with its intent preserved. No test is silently lost or run twice, no package-access failure occurs, and no fixture or schema input is left dangling.
- Unit tests run without server or database setup. Integration and e2e suites use native embedded MariaDB with explicit per-run paths and ports and deterministic schema and data.
- Lifecycle tests cover startup failure, assertion failure, repeated runs, attachment ownership and simultaneous independent invocations, and cleanup touches only owned resources.
- Browser tests render real Yada pages and cover authenticated, Ajax, upload, CMS and commerce flows in Chromium, with controlled fakes for external services.
- Every active code module has meaningful central coverage, Docs verification is wired, and the empty Bones module is recorded honestly.
- Full verification is reproducible from YadaTests on the verified platform, with CI reports, useful failure artifacts, and Java coverage attributed to the production modules.

## External implementation references

- [Playwright Java test runners](https://playwright.dev/java/docs/test-runners) and [browser setup](https://playwright.dev/java/docs/browsers) support the explicit JUnit lifecycle and browser installation approach. Use manual lifecycle hooks rather than the experimental JUnit fixture extension.
- [Gradle JaCoCo plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) describes execution-data and source/class inputs for reporting. Check API availability against the root 8.14.3 wrapper during implementation.
- `2026-10-11-repository-gradle-root.md` and its verification file describe the root build, entry points, Eclipse ownership and the pending shared-file relocation.
