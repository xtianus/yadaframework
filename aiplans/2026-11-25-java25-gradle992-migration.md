# Java 25 and Gradle 9.9.2 Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Yada Framework build, test harness, documentation build, standalone example, and application template to Gradle 9.9.2 and Java 25, while replacing the unmaintained Sass plugin and fixing migration warnings.

**Architecture:** The migration is runtime-first, as required by the audit: first replace the build blockers while the Java compilation baseline remains 21, verify Java 21 bytecode on a Java 25 runtime, and only then flip compilation to Java 25. SCSS compilation becomes explicit Gradle build logic: a shared script downloads one exact Dart Sass release for the current operating system/architecture into an OS-specific folder, extracts it there, and invokes it with an `Exec` task named `sassCompile`. Tests, IDE metadata, CI, documentation, and downstream-facing notes are migrated together so that the result is usable by applications rather than merely compilable.

**Tech Stack:** Java 25, Gradle 9.9.2, Groovy DSL, JUnit 6/5, Mockito 5.24.0, Vibur DBCP 26.0, Dart Sass 1.108.2, `de.undercouch.download` 5.7.0, Eclipse Buildship/WTP, GitHub Actions Temurin.

**Spec:** this plan is self-contained and incorporates the authoritative migration audit of Yada Framework 0.7.9 at commit `969ddfbf`, completed on 2026-11-25 at 17:30:53 CET using OpenJDK 25.0.5.1 and Gradle 9.7.1 in temporary snapshots. Additional project conventions are in [aiknowledge/workspace_build_notes.md](../aiknowledge/workspace_build_notes.md) and [aiknowledge/yada_tests_notes.md](../aiknowledge/yada_tests_notes.md).

## Global Constraints

- Do not start implementation before this plan has been reviewed.
- Target exactly Gradle `9.9.2` in both framework wrappers; distribution SHA-256: `971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b`.
- Keep the Java compilation baseline at 21 until the runtime-gate task explicitly flips it to 25.
- Keep `-parameters` in every `JavaCompile` configuration: Spring MVC parameter-name discovery depends on it.
- Target exactly Dart Sass `1.108.2`; do not use the unmaintained `com.meiuwa.gradle.sass` plugin in any form.
- `sassCompile` must keep its existing task name, must download and extract the exact Dart Sass version automatically for the current OS platform into an OS-specific folder, and must keep writing compressed `.min.css` plus `.min.css.map` files into `src/main/webapp/res/css`.
- No `Project.exec`, `ConfigureUtil`, or configuration-time process execution in new build logic; use task types and execution-time behavior.
- `terraforming.sh` is no longer used and is explicitly out of scope. Do not spend migration effort on it or on `terraforming.cfg`.
- Fix migration warnings; do not hide them with broad JVM flags or deprecation silencers. Pre-existing Javadoc errors that are unchanged on Java 21 and Java 25 are outside this migration unless they block a required task. Two groups of warnings stay by decision, both scheduled for removal in Gradle 10: `StartParameter.isConfigurationCacheRequested` from the Asciidoctor Gradle plugin 4.0.5 (the migration to its 5.x model is out of scope), and the Eclipse IDE deprecations (`EclipseJdt.file` and `EclipseWtp.facet` hooks, and the `eclipse`/`eclipseWtp*` generation tasks). The Eclipse configuration and its `cleanEclipse`/`eclipse` workflow stay unchanged until an official replacement is adopted in a separate change.
- Do not downgrade Spring Framework beyond its current 6.2.x line or make unrelated dependency upgrades.
- Do not edit `build2.gradle.v8`; it is an inactive historical reference.
- Update dependency locks whenever a locked dependency changes.
- Search in English and Italian when checking application-facing documentation and launch/configuration references.

## Review Focus

Conditions that can compile but produce the wrong migration result; each is pinned by the task shown in brackets.

1. On Windows, a drive-letter colon in `C:\...` can be misparsed in Dart Sass many-to-many `input:output` arguments. Paths cannot simply be project-root-relative in downstream multi-project builds because framework CSS may live outside the application root: use forward-slash paths relative to the computed common ancestor of inputs and outputs, or fail with a clear message when no such ancestor exists [Task 2].
2. Replacing SCSS `@import` with `@use` can move CSS earlier in the output and change cascade order: preserve ordering deliberately and inspect the generated diff [Tasks 2-3].
3. Vibur 26.0 removes the `Unsafe` warning but can hide the original pool-validation error behind a `NullPointerException` when connection tracking is enabled and startup fails before pool creation: preserve the validation cause [Task 5].
4. Plain locale `ar` no longer implies Arabic-Indic digits on Java 25: applications needing those digits must use an explicit Unicode numbering-system extension such as `ar-u-nu-arab` [Task 8].
5. A build can compile with `sourceCompatibility = 25` while tests or Eclipse still use another JDK: toolchain, CI, Eclipse metadata, and verification must all agree on Java 25 [Task 9].

## Audit evidence and environment caveats

Gradle officially supports Java 25 for its daemon and toolchains from Gradle 9.1. The audit used Gradle 9.7.1; this plan pins the newer exact target Gradle 9.9.2.

The audit validated the runtime-first rollout with these results:

- Java 21 bytecode on Java 25: 423 unit and 180 integration cases passed.
- Java 25 bytecode on Java 25: 423 unit, 180 integration, 11 tooling, and 26 browser cases passed.
- Standalone example on Java 25: the WAR assembled and 48 tests passed, with Sass bypassed in the audit copy because the old plugin could not run on Gradle 9.
- Framework build on Java 25: assembly, Eclipse metadata generation, AsciiDoc verification, and both schema-generation tasks completed.
- Javadoc reported errors at the same source locations under both JDKs; unchanged pre-existing Javadoc errors are not migration regressions.
- The Vibur statement-cache dependency `concurrentlinkedhashmap-lru` called `sun.misc.Unsafe.objectFieldOffset`, and Mockito dynamically self-attached its Java agent. Both mechanisms still worked on Java 25; Tasks 5 and 6 remediate them to remove warnings and future risk, not because either was a fatal Java 25 failure.

Environment caveats from the audit:

- MariaDB4j treats databases below the JVM temporary directory as disposable. When running in temporary snapshots or redirected temporary directories, use a separate JVM temporary directory for integration runs rather than a shared or host temporary directory.
- The concurrent database test compares the data path returned by MariaDB with the configured path. In the audit it passed for both bytecode targets only after rerunning with real directory paths in place of temporary symlinks; use real paths when reproducing that setup.
- Both audited JDKs reject the Java 8 GC logging flag `UseGCLogFileRotation` and accept unified `-Xlog` logging. `terraforming.sh` remains out of scope for this migration, but the final application handoff must mention this deployment issue for installations that still use that script.

Reference documentation: [Oracle JDK migration guide](https://docs.oracle.com/en/java/javase/25/migrate/preparing-migration.html), [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html), [Gradle 9 upgrade guide](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html), [Dart Sass CLI](https://sass-lang.com/documentation/cli/dart-sass/), [JDK 25 locale data](https://www.oracle.com/java/technologies/javase/jdk25-supported-locales.html), [JDK 24 warning guidance](https://docs.oracle.com/en/java/javase/25/migrate/significant-changes-jdk-24.html), [Java launcher documentation](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html), and [Mockito instrumentation setup](https://github.com/mockito/mockito/blob/main/mockito-core/src/main/java/org/mockito/Mockito.java).

---

## File Structure

- Create `YadaTools/scripts/dart-sass.gradle`: shared Dart Sass download/extract/compile build logic used by `YadaExamples`, the application template, and downstream applications.
- Modify wrappers in `gradle/wrapper/` and `YadaExamples/gradle/wrapper/` for Gradle 9.9.2.
- Modify `shared.gradle.properties`, `.github/workflows/yada-tests.yml`, `eclipseSetup.gradle`, and `YadaTools/scripts/template.gradle` for the Java 25 baseline and IDE metadata.
- Modify `YadaExamples/build.gradle` and `YadaExamples/src/main/webapp/res/css/*.scss` for the Sass replacement and Sass module syntax.
- Modify `YadaTests/build.gradle` for Gradle 9 configuration ordering, Mockito agent loading, and dependency locks.
- Modify `YadaWeb/build.gradle`, `YadaWeb/src/main/java/net/yadaframework/core/YadaJpaConfig.java`, and possibly add one small Vibur subclass in the same package for the statement-cache/runtime-warning migration.
- Modify `YadaDocs/build.gradle` for Groovy assignment syntax only; the Asciidoctor plugin stays at 4.0.5.
- Do not change the Eclipse configuration (`eclipseSetup.gradle` Eclipse blocks and the per-module `eclipse {}` blocks) beyond what the Java 25 flip derives from `java_version`.
- Modify tests and documentation named in the tasks below. Do not modify `YadaTools/scripts/terraforming.sh` or `terraforming.cfg`.

### Task 1: Baseline and migration inventory

**Files:**
- Read: `shared.gradle.properties`, both wrapper files, `YadaTests/build.gradle`, `YadaExamples/build.gradle`, `YadaTools/scripts/template.gradle`
- No production files changed by this task

- [ ] **Step 1:** Confirm the working tree is clean except for migration planning documents and that the current branch is `0.8.0`. Record `git rev-parse HEAD`. If it is not the audited commit `969ddfbf` or a deliberate descendant of it, stop and reconcile the audit before continuing.
- [ ] **Step 2:** From the repository root run `./gradlew tasks --warning-mode all` and save the output outside the repository. Expected baseline warnings on Gradle 8.14.3: the `StartParameter.isConfigurationCacheRequested` warning from the Asciidoctor plugin and eight Groovy space-assignment warnings (`outputDir` ×3 in `YadaDocs`, `exceptionFormat` in `YadaTests`, `name` ×4 in `YadaTools` and `YadaWebCommerce`). Gradle 9 additionally reports warnings that Gradle 8.14.3 does not show: IDE file-generation deprecations for `EclipseJdt.file` (in `eclipseSetup.gradle`, `YadaDocs/build.gradle`, and `YadaTools/build.gradle`) and `EclipseWtp.facet` (in `eclipseSetup.gradle`), plus `AbstractTestTask.afterSuite(Closure)` from `failWhenNoTestsDiscovered` in `YadaTests/build.gradle`. A Gradle 9 run of the unchanged build fails during configuration with `Cannot mutate the resolution strategy of configuration ':YadaTests:all'` (fixed in Task 4).
- [ ] **Step 3:** From `YadaExamples` run `./gradlew sassCompile test --warning-mode all` with the existing Gradle 8 build and save the output outside the repository. Copy generated `src/main/webapp/res/css/*.min.css*` to a temporary reference folder for later comparison.
- [ ] **Step 4:** Inventory Sass inputs and expected output names. Required inputs are the application CSS tree plus `YadaWeb/src/main/resources/net/yadaframework/views/yada/css` and `YadaWebCMS/src/main/resources/net/yadaframework/views/yada/css`; inputs include plain `.css` as well as `.scss`, exclude partials beginning with `_` and existing `.min.*`, and flatten outputs into the application `src/main/webapp/res/css` directory.
- [ ] **Step 5:** Commit only the baseline inventory note if the implementer creates one inside the repository; otherwise keep it outside the repository and make no commit.

### Task 2: Shared Dart Sass download and `sassCompile` replacement

**Files:**
- Create: `YadaTools/scripts/dart-sass.gradle`
- Modify: `YadaExamples/build.gradle`
- Modify: `YadaTools/scripts/template.gradle`
- Delete during implementation: `YadaExamples/sass.properties` and `YadaTools/scripts/sass.properties` after confirming no references remain

**Interfaces:**
- Consumes: existing source trees and output layout identified in Task 1.
- Produces: a function `configureDartSass(Map args)` exported as a project extra property (`ext.configureDartSass = { Map args -> ... }`), with required keys `sources` (the complete source tree, including partials) and `outputDir` and optional key `version` defaulting to `1.108.2`. It registers exactly these tasks: `downloadDartSass`, `verifyDartSass`, `unpackDartSass`, and `sassCompile` (`Exec`).

Two script-plugin rules shape this interface; both were confirmed with a scratch build on Gradle 9.7.1. A method declared with `def` in a script applied through `apply from:` is not visible to the applying build script, so the function must be an `ext` closure. Classes from plugins declared in the applying build's `plugins {}` block are not visible inside the applied script (`Could not get unknown property 'de'`), so the script must declare its own `buildscript {}` block with `gradlePluginPortal()` and `classpath 'de.undercouch:gradle-download-task:5.7.0'` and reference `de.undercouch.gradle.tasks.download.Download`/`Verify` from there. With that, the applying build does not need to apply the download plugin.

The audit established why this replacement is required: the `com.meiuwa.gradle.sass` plugin fails when its `download` closure calls `ConfigureUtil`; using the `Action` overload only gets past configuration, after which `sassCompile` fails on `Project.exec`. Gradle 9 removed both APIs. The pinned Dart Sass CLI compiled `yex.scss` successfully in the audit copy.

- [ ] **Step 1:** Implement `YadaTools/scripts/dart-sass.gradle` as shared build logic. It declares its own `buildscript` classpath for `de.undercouch:gradle-download-task:5.7.0` and exports `configureDartSass(Map args)` as an `ext` closure, as described in the interface above. Document this usage contract in a short header comment in the script, because applications read it there. The implementation must normalize Windows/macOS/Linux and `x64`/`arm64`, use the Dart Sass artifact names `dart-sass-<version>-<platform>-<arch>.zip` for Windows and `.tar.gz` otherwise, fail clearly for unsupported platforms, and select the `-musl` Linux artifact only when the host is positively identified as musl-based.
- [ ] **Step 2:** The download location must be OS-specific and version-aware: use `$rootDir/.sass-<platform>-<arch>/` as the per-platform root, store the archive there, and extract into `$rootDir/.sass-<platform>-<arch>/<version>/`. This preserves the current Windows-versus-WSL behavior where the two operating systems do not overwrite each other's Sass binaries. The architecture part of the folder name changes from the raw `os.arch` value used today (for example `.sass-linux-amd64`) to the normalized `x64`/`arm64`, so existing folders become stale and can be deleted. `YadaExamples/.gitignore` already ignores `/.sass*/`; the framework root `.gitignore` has no such rule, which is acceptable only because the script is never applied by the framework root build. Make sure applications generated from the template get an equivalent ignore rule.
- [ ] **Step 3:** Verify downloads with SHA-256. Register `downloadDartSass` as a `Download` task with `overwrite = false`; register `verifyDartSass` as the download plugin's `Verify` task depending on the download, with `algorithm 'SHA-256'`; and make unpacking depend on verification. Pin these checksums for Dart Sass `1.108.2` in the shared script: Windows x64 `8c9f35ac5a82090b36df59d3f411fe4403ce04332dff5abbfc633c91b5e3fe13`; Windows arm64 `dbf45de59a696d6a741f8edf25ec5e55fb49c1db8b24e64a42dd72976337fa9c`; macOS x64 `8822d1f3715d84126a6adfe7053559a64e69687bb6e1b5da0d7f854b0c6ba2fa`; macOS arm64 `95b1bbb13f0b38ba8aacefae45ef23f11271231d24fb57b1593c9e2adf65481c`; Linux x64 glibc `32d2eb5b83cbc180fce9657ff3c57401a9006c59eccfe0bfdde0aa755a66d42f`; Linux arm64 glibc `1c7a587c81467e8f7551d71e5d62845b53270033c5cc3d36aa8fa07330a5d2a9`; Linux x64 musl `a5519557dac62fbe468428d0025043e0cf4344bda2f7f6e3c3cb2fccc1f37b62`; Linux arm64 musl `4931bc8b31dcbf0dbf28ad5e390c275625b430ac8d8c006bbcd6fe58c8a3248b`. They were calculated from the immutable GitHub release assets because Dart Sass does not publish a checksum file.
- [ ] **Step 4:** Register `unpackDartSass` as a `Copy` task using `zipTree`/`tarTree`, and `sassCompile` as an `Exec` task. On non-Windows systems restore executable bits on `dart-sass/sass` and `dart-sass/src/dart` after unpacking. The executable is `dart-sass/sass.bat` on Windows and `dart-sass/sass` otherwise.
- [ ] **Step 5:** In `sassCompile`, derive one output mapping per non-partial input: strip `.scss`, `.sass`, or `.css` and write `<basename>.min.css` directly in `outputDir`; expect the sibling `<basename>.min.css.map`. Fail configuration with a clear duplicate-output error if two inputs map to the same output. Declare the complete unfiltered source tree—including `_*.scss`/`_*.sass` partials—the compiler executable, and the Sass version as inputs; filter partials only when constructing compiler entry points. Declare every `.min.css` and `.map` as outputs.
- [ ] **Step 6:** Invoke Sass with `--style=compressed --source-map` and `input:output` mappings. Compute the normalized common ancestor directory of every input file and `outputDir` (using `java.nio.file.Path` semantics), set `workingDir` to that ancestor, and pass forward-slash paths relative to it. These relative paths may contain `..` segments: downstream applications such as Danese include framework CSS outside the application project root. Fail clearly only when no common ancestor or drive exists (for example inputs on different Windows drives). Do not pass `--update`: Gradle inputs/outputs decide when compilation is needed.
- [ ] **Step 7:** In `YadaExamples/build.gradle` remove `com.meiuwa.gradle.sass` (do not add the download plugin: the shared script brings its own classpath), apply `../YadaTools/scripts/dart-sass.gradle`, construct the same source tree as the old `sassCompile` block, and call `configureDartSass(sources: <that tree>, outputDir: file('src/main/webapp/res/css'))`. Keep `eclipse.autoBuildTasks sassCompile`, but also wire `war` to depend on `sassCompile`: generated CSS lives under `src/main/webapp`, so `clean war` otherwise leaves stale CSS in place or can package no CSS in a fresh checkout.
- [ ] **Step 8:** Make the same replacement in `YadaTools/scripts/template.gradle`, so newly generated applications receive the Gradle 9-compatible implementation. A generated application cannot use the `../YadaTools/...` path: apply the script through the same framework-checkout convention the template already uses for `YadaWebLib` (`$projectDir/../../yadaframework/...`), with the same "add more `../`" comment. Also make the application template wire its WAR packaging task to `sassCompile`, and remove the unused `com.github.node-gradle.node` plugin from the example/template at this point rather than carrying an unused obsolete plugin forward.
- [ ] **Step 9:** Verify while still on Gradle 8 and Java 21: from `YadaExamples` run `./gradlew clean sassCompile --warning-mode all`. Expected: download/extraction happens automatically in the OS-specific folder, `yex.min.css`, `yada.min.css`, `yadacms.min.css`, and maps are produced, no `ConfigureUtil` or `Project.exec` reference remains, and no Sass `@import` deprecation is hidden (Task 3 removes them).
- [ ] **Step 10:** Verify cross-platform logic by inspection and, where available, by running the same command on Windows and inside WSL. Expected: two separate `.sass-windows-x64` and `.sass-linux-x64` folder families, neither overwriting the other.
- [ ] **Step 11:** Commit the Sass replacement separately before the Gradle wrapper upgrade.

### Task 3: Sass module syntax for Dart Sass 1.108.2

**Files:**
- Modify: `YadaExamples/src/main/webapp/res/css/yex.scss`
- Modify: `YadaExamples/src/main/webapp/res/css/_variables.scss` only if namespacing requires it
- Modify: `YadaExamples/src/main/webapp/res/css/_bootstrapFix.scss`
- Modify generated but uncommitted outputs under `YadaExamples/src/main/webapp/res/css`

- [ ] **Step 1:** Replace `@import './_variables.scss';` and `@import './_bootstrapFix.scss';` in `yex.scss` with `@use 'variables' as v;` and `@use 'bootstrapFix';`; change `$font-size-base` in `yex.scss` to `v.$font-size-base`.
- [ ] **Step 2:** In `_bootstrapFix.scss` add `@use 'variables' as v;` at the top and replace both uses of `$c-error` with `v.$c-error`. Do not rename variables in `_variables.scss`.
- [ ] **Step 3:** Run `./gradlew sassCompile` from `YadaExamples`. Expected: no `DEPRECATION WARNING [import]` output. If another Sass warning appears, fix the SCSS rather than silencing Dart Sass.
- [ ] **Step 4:** Diff the newly generated CSS against the reference copies from Task 1. Minification and formatting differences are acceptable after the compiler upgrade; selector loss, changed cascade order, missing framework rules, or a missing `@import url(...)` for external CSS are not. Stop and report any behavioral difference before proceeding.
- [ ] **Step 5:** Commit the SCSS module migration. Generated example outputs remain ignored/uncommitted according to the existing `.gitignore`.

### Task 4: Gradle 9.9.2 while compiling Java 21

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
- Modify: `YadaExamples/gradle/wrapper/gradle-wrapper.properties`, `YadaExamples/gradlew`, `YadaExamples/gradlew.bat`, `YadaExamples/gradle/wrapper/gradle-wrapper.jar`
- Modify: `YadaTests/build.gradle`
- Modify: `YadaTools/scripts/template.gradle`

- [ ] **Step 1:** In `YadaTests/build.gradle` replace `all*.exclude group: 'commons-logging', module: 'commons-logging'` with `configureEach { exclude group: 'commons-logging', module: 'commons-logging' }` inside the existing `configurations` block. The audit showed that the original expression makes Gradle observe the `:YadaTests:all` configuration before `lockAllConfigurations()` can configure it. Do not alter the dependency semantics. This must come before Step 2: on Gradle 9 the unchanged root build fails during configuration, so the second wrapper run below could not execute.
- [ ] **Step 2:** From the framework root run `./gradlew wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum 971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b`. Run the same command from `YadaExamples` using its wrapper. Then run the wrapper task a second time in both locations using the newly downloaded Gradle 9.9.2 distribution so `gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` are regenerated by the target Gradle version; retain the same pinned distribution SHA-256 in both `gradle-wrapper.properties` files.
- [ ] **Step 3:** Align SSH plugin versions: `org.hidetake.ssh` must be `2.12.0` in the application template and remain `2.12.0` in `YadaExamples`. Do not reintroduce the Node plugin in framework files; it is unused there.
- [ ] **Step 4:** Verify `./gradlew tasks` from the root and `./gradlew tasks` from `YadaExamples` on Gradle 9.9.2. Expected: build configuration succeeds with `java_version` still `21`.
- [ ] **Step 5:** Run `./gradlew compileJava compileTestJava` and, from `YadaExamples`, `./gradlew compileJava compileTestJava`. Expected: Java 21 bytecode still, because `shared.gradle.properties` has not yet changed.
- [ ] **Step 6:** Commit the Gradle wrapper and configuration-order migration separately.

### Task 5: Runtime warning remediation — Vibur statement cache

**Files:**
- Modify: `YadaWeb/build.gradle`
- Modify: `YadaWeb/src/main/java/net/yadaframework/core/YadaJpaConfig.java`
- Create if needed: `YadaWeb/src/main/java/net/yadaframework/core/YadaViburDBCPDataSource.java`
- Modify: `YadaTests/src/integrationTest/java/net/yadaframework/tests/support/EmbeddedDatabaseLifecycleTest.java` only if the existing invalid-pool test does not fully pin the regression
- Modify: `YadaTests/gradle.lockfile`

**Interfaces:**
- Consumes: existing `config/database/datasource` keys read by `YadaJpaConfig`, including `poolEnableConnectionTracking` and `statementCacheMaxSize`.
- Produces: a Vibur datasource startup path whose validation failure chain still contains the original `IllegalArgumentException` from `org.vibur.*` when connection tracking is enabled.

- [ ] **Step 1:** Upgrade `org.vibur:vibur-dbcp` from `25.0` to exactly `26.0` and remove the direct `runtimeOnly 'com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:1.4.2'` dependency. Vibur 26.0 uses Caffeine internally; do not pin Caffeine separately unless dependency resolution selects a version with a known Java 25 warning.
- [ ] **Step 2:** Reproduce the known startup-failure masking with a focused integration test or the existing invalid-pool test: enable `poolEnableConnectionTracking`, use an invalid datasource setting, refresh the context, and assert the failure chain contains the original Vibur validation `IllegalArgumentException`, not only a `NullPointerException` from `terminate()`.
- [ ] **Step 3:** If the test shows the known masking, add `YadaViburDBCPDataSource extends ViburDBCPDataSource` in `net.yadaframework.core` and override only the method that throws in the reproduced stack trace (expected: `getTakenConnections()`), so it returns an empty `TakenConnection` array when connection tracking is disabled or `getPool() == null`, otherwise delegates to `super`. Confirm the method against the Vibur 26.0 sources before writing the override. Use that subclass in `YadaJpaConfig`. Do not copy or fork other Vibur behavior. Update the comment on the Vibur dependency in `YadaWeb/build.gradle`, which currently explains why 26.0 was avoided.
- [ ] **Step 4:** First run the focused test without `--write-locks` to confirm behavior: `./gradlew :YadaTests:integrationTest --tests '*EmbeddedDatabaseLifecycleTest*'`. Then refresh every affected locked configuration with the repository-standard full resolution `./gradlew :YadaTests:dependencies --write-locks` (run from the framework root with the correct project path if necessary), because Vibur/CLHM appear in unit, browser, application, and other classpaths. Expected: the validation test passes and `YadaTests/gradle.lockfile` contains Vibur 26.0/Caffeine entries but no `concurrentlinkedhashmap-lru` entry.
- [ ] **Step 5:** Run a broader integration subset with normal locking enabled (no `--write-locks`) that starts the embedded database. Inspect the output for `sun.misc.Unsafe`, `objectFieldOffset`, and `concurrentlinkedhashmap`. Expected: none.
- [ ] **Step 6:** Commit the runtime-warning fix and lockfile update.

### Task 6: Runtime warning remediation — Mockito agent

**Files:**
- Modify: `YadaTests/build.gradle`
- Modify: `YadaTests/gradle.lockfile`

**Interfaces:**
- Produces: all `Test` tasks (`test`, `integrationTest`, `e2eTest`, `toolTest`) start with `mockito-core` as an explicit `-javaagent` rather than relying on Mockito/Byte Buddy dynamic self-attachment.

- [ ] **Step 1:** Add a resolvable, non-consumable `mockitoAgent` configuration and a dependency on `org.mockito:mockito-core:${mockitoVersion}` with `transitive = false`. Keep the normal `testCommon` Mockito dependencies unchanged.
- [ ] **Step 2:** In the existing `configureSuite` closure, add the agent to every test JVM. Prefer a `CommandLineArgumentProvider` with the agent file annotated as a classpath input; if the simpler documented Groovy form is used, it must still avoid resolving unrelated transitive dependencies and must produce exactly one `-javaagent:<mockito-core.jar>` argument.
- [ ] **Step 3:** Run `./gradlew :YadaTests:dependencies --write-locks` so the newly resolvable `mockitoAgent` configuration is locked with the exact `mockito-core` artifact, then run `./gradlew :YadaTests:test --warning-mode all` with normal locking enabled. Expected: no `A Java agent has been loaded dynamically`, no Byte Buddy dynamic attach warning, and no new Gradle warning.
- [ ] **Step 4:** Run `./gradlew :YadaTests:integrationTest :YadaTests:toolTest` and inspect output for the same warning. The browser suite can be deferred to final verification because of its cost, but it uses the same closure and must be checked before completion.
- [ ] **Step 5:** Commit the Mockito configuration and lockfile update.

### Task 7: Gradle warning cleanup

**Files:**
- Modify: `YadaDocs/build.gradle`
- Modify: `YadaTests/build.gradle`
- Modify: `YadaTools/build.gradle`
- Modify: `YadaWebCommerce/build.gradle`

**Interfaces:**
- Produces: the documentation build is unchanged (Asciidoctor plugin 4.0.5, same tasks, same output under repository `docs/`), and `:YadaTests:verifyDocs` still fails on Asciidoctor warnings and broken relative links.

- [ ] **Step 1:** Fix all Groovy space-assignment warnings with explicit assignments: `outputDir = ...` in `YadaDocs/build.gradle` lines around 104, 206, and 252; `exceptionFormat = 'full'` in `YadaTests/build.gradle`; `name = 'PublicRepo'` and `name = 'LocalRepo'` in `YadaTools/build.gradle` and `YadaWebCommerce/build.gradle`. Use exact assignment syntax rather than method-call syntax. In `YadaTests/build.gradle`, also replace the deprecated `testTask.afterSuite { ... }` closure in `failWhenNoTestsDiscovered` with an equivalent `TestListener` registered through `addTestListener`, keeping the "no test discovered" failure for unfiltered runs. Gradle 9 may have its own failure for test tasks that discover no tests (unverified here). Remove the custom helper only if a deliberately mis-wired, unfiltered suite still fails without it.
- [ ] **Step 2:** Run `./gradlew :YadaTests:verifyDocs --warning-mode all`, then `./gradlew :YadaDocs:asciidoctor :YadaDocs:asciidoctorPdf` to confirm that the assignment-syntax change still writes into repository `docs/en`. Expected: documentation renders, link verification passes, no Groovy space-assignment or `afterSuite(Closure)` deprecation appears. The Asciidoctor and Eclipse deprecations remain by decision. If the regenerated files under `docs/` differ only in generated timestamps, restore them with git instead of committing churn.
- [ ] **Step 3:** Commit the warning cleanup.

### Task 8: Arabic money-formatting behavior and documentation

**Files:**
- Modify: `YadaTests/src/test/java/net/yadaframework/commerce/persistence/entity/TestYadaMoney.java`
- Modify: `YadaDocs/src/docs/asciidoc/en/internationalization.adoc`
- Modify: `YadaDocs/src/docs/asciidoc/en/upgrade.adoc`
- Modify: `aiknowledge/yadaWeb/YadaMoney.md` or the closest existing knowledge file

**Interfaces:**
- Consumes: `YadaMoney.toString(Locale)`, which applies decimal pattern `0.00` through `NumberFormat.getNumberInstance(locale)`.
- Produces: documented downstream guidance that plain `ar` is locale-data dependent and explicit numbering systems are the stable mechanism.

For the amount used in the audit, plain `ar` produced `١٢٣٤٥٫٦٧` on Java 21 and `12345.67` on Java 25, while `ar-u-nu-arab` produced Arabic-Indic digits on both JDKs. This is a locale-data behavior change, not a `YadaMoney` rounding regression.

- [ ] **Step 1:** Keep or add a unit test proving that `new YadaMoney(12345.6789).toString(Locale.ENGLISH)` remains `12345.68` (the existing rounding behavior). The current `testRounding` test already contains this assertion; extend that test class rather than duplicating it.
- [ ] **Step 2:** Add a unit test proving `Locale.forLanguageTag("ar-u-nu-arab")` formats the same rounded amount with Arabic-Indic digits as `١٢٣٤٥٫٦٨`, and a test proving `Locale.forLanguageTag("ar-u-nu-latn")` uses Latin digits. These explicit-numbering tests must pass on both Java 21 and Java 25.
- [ ] **Step 3:** Add a migration-specific assertion for plain `Locale.forLanguageTag("ar")` only if it can be written without conditional ambiguity. Acceptable implementation: assert Latin digits when `Runtime.version().feature() >= 24` and Arabic-Indic digits otherwise, with a comment explaining that CLDR changed the default numbering system. Do not hard-code Arabic-Indic digits into `YadaMoney`.
- [ ] **Step 4:** Document in `internationalization.adoc` that `YadaMoney.toString(Locale)` is number formatting, not currency-symbol formatting or currency conversion, and that Java 25/CLDR changed the plain Arabic locale default. Document `ar-u-nu-arab` and `ar-u-nu-latn` as explicit choices.
- [ ] **Step 5:** Add a `Changes in 0.8.0` section to `upgrade.adoc` covering Java 25/Gradle 9.9.2, the Sass replacement (how an application applies the shared script), the Vibur 26 statement-cache change and the Arabic locale note. Update the version-comparison table with the new Java 25 row without rewriting unrelated history.
- [ ] **Step 6:** Run `./gradlew :YadaTests:test --tests '*TestYadaMoney*'`. Expected: all money tests pass on the current JDK.
- [ ] **Step 7:** Commit the locale tests and documentation.

### Task 9: Flip compilation to Java 25

**Files:**
- Modify: `shared.gradle.properties`
- Modify: `.github/workflows/yada-tests.yml`
- Modify: `YadaTools/scripts/template.gradle`
- Modify: module `build.gradle` files whose `toolchain` blocks are commented out, including at least `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools`, and `YadaTests` if no toolchain exists
- Modify: `eclipseSetup.gradle` only if Java 25 metadata is not already derived from `java_version`

**Interfaces:**
- Produces: all framework compilation, test execution, CI, and Eclipse metadata use Java 25; produced class files are major version 69.

- [ ] **Step 1:** Make the test runtime independently selectable in `YadaTests`: in `configureSuite`, set each test task's `javaLauncher` from the Java toolchain service using optional Gradle property `testRuntimeVersion`, defaulting to `java_version`. Then perform the runtime-first gate while compilation is still 21 with `./gradlew :YadaTests:test :YadaTests:integrationTest -PtestRuntimeVersion=25`. Confirm bytecode is still major version 65 with `javap -verbose` on one compiled class. This is the rollback-friendly phase applications can deploy first, and it does not depend on whichever JVM started the Gradle daemon.
- [ ] **Step 2:** Commit the launcher change and push. This commit is the Java 21 checkpoint: it contains Tasks 1-8 and the runtime gate while `java_version` is still 21. Source-linked applications must build against this commit while they still compile Java 21, because each framework module's own build script sets its Java level from `java_version` and overrides the application's settings. Keep the flip below in a separate commit that changes `java_version`, so the checkpoint stays identifiable from history as the last commit where `shared.gradle.properties` has `java_version=21`. Record its hash in the final report.
- [ ] **Step 3:** Change `java_version=21` to `java_version=25` in `shared.gradle.properties`.
- [ ] **Step 4:** Make toolchain use consistent in every framework module, `YadaTests` included: `toolchain { languageVersion = JavaLanguageVersion.of(java_version) }` (all of them load `java_version` through `loadSharedProperties.gradle`). Keep source/target compatibility aligned. Do not rely on whichever JVM happens to start the Gradle daemon. Without a toolchain, a module with target 25 fails with "invalid target release" when the daemon runs on Java 21.
- [ ] **Step 5:** Update `.github/workflows/yada-tests.yml` from Temurin `21` to Temurin `25`.
- [ ] **Step 6:** Update `YadaTools/scripts/template.gradle` hard-coded Java values: `JavaVersion.VERSION_25`, `JavaLanguageVersion.of(25)`, and Eclipse JDT metadata for Java 25. Keep `-parameters` and all existing comments that explain non-obvious behavior.
- [ ] **Step 7:** Run `./gradlew clean :YadaTests:compileJava :YadaTests:compileTestJava` and inspect one framework class with `javap -verbose`. Expected: `major version: 69`.
- [ ] **Step 8:** Regenerate the Eclipse metadata with the existing workflow: `./gradlew cleanEclipse`, then `./gradlew eclipse` in a separate invocation (their deprecation warnings are accepted), then refresh the framework through Buildship in the supported Eclipse version. Inspect `.settings/org.eclipse.jdt.core.prefs` and, in WTP modules, `.settings/org.eclipse.wst.common.project.facet.core.xml`. Expected: Java 25 (`JavaSE-25` or the exact current Eclipse representation) everywhere, with no stale Java 21 facet.
- [ ] **Step 9:** Commit the Java 25 baseline flip separately from earlier build migrations and from the Step 2 checkpoint.

### Task 10: Full verification and application handoff

**Files:**
- Modify: `aiknowledge/workspace_build_notes.md`
- Modify: `aiknowledge/yada_tests_notes.md`

- [ ] **Step 1:** Run the complete framework verification on Java 25: `./gradlew :YadaTests:frameworkCheck :YadaTests:frameworkCoverage --continue`. Expected suite counts at least equal to the audit: 423 unit, 180 integration, 11 tooling, and 26 browser cases, accounting for any tests deliberately added by this migration.
- [ ] **Step 2:** From `YadaExamples` run `./gradlew clean test war` only after deleting or moving aside generated `src/main/webapp/res/css/*.min.css*` (or using a fresh checkout/worktree). Expected: the standalone example assembles on Java 25, tests pass, the new `sassCompile` runs because `war` depends on it, and the WAR contains `yex.min.css`, `yada.min.css`, and `yadacms.min.css` without relying on pre-existing generated files.
- [ ] **Step 3:** Run root and example `tasks --warning-mode all` plus the verification command output through a warning inventory. Expected migration warnings absent: `ConfigureUtil`, `Project.exec`, Groovy space assignment, `AbstractTestTask.afterSuite(Closure)`, `sun.misc.Unsafe`/`objectFieldOffset` from CLHM, and dynamic Java agent loading. Expected remaining warnings, accepted by decision: `StartParameter.isConfigurationCacheRequested` from the Asciidoctor plugin 4.0.5, and the Eclipse IDE deprecations (`EclipseJdt.file`, `EclipseWtp.facet`, and the `eclipse*` tasks when they run).
- [ ] **Step 4:** Verify the application template on Gradle 9.9.2, because no other step executes it: in a scratch folder outside the repository, lay out a minimal application from `YadaTools/scripts/template.gradle` next to a framework checkout (the template expects `../../yadaframework`), and run `tasks --warning-mode all` plus `sassCompile`. The template still loads the published `net.yadaframework:yadatools:0.7.8.1` plugin, which was built with Gradle 8; report whether it applies cleanly on Gradle 9, and do not change the published version as part of this migration.
- [ ] **Step 5:** Verify documentation and schema tooling: `./gradlew :YadaTests:verifyDocs :YadaTests:dbSchema :YadaTests:webTestDbSchema` passes.
- [ ] **Step 6:** Update `aiknowledge/workspace_build_notes.md` with the final build facts: Gradle 9.9.2, Java 25 toolchain, Dart Sass 1.108.2 shared script and OS-specific folder layout, no terraforming involvement, the Eclipse/Buildship import expectations, and the accepted Asciidoctor and Eclipse deprecation warnings.
- [ ] **Step 7:** Update `aiknowledge/yada_tests_notes.md` with the test-suite facts: explicit Mockito agent, Vibur 26/Caffeine, warning inventory expectations, and the Java 25 runtime/bytecode split used during rollout.
- [ ] **Step 8:** Do not change `yada_version` or publish artifacts as part of this migration unless the maintainer explicitly asks. Record in the final report that Java 25 bytecode is a breaking runtime requirement for downstream applications and must be reflected in any release/version decision. Also record the out-of-scope `terraforming.sh` deployment caveat: installations still using that script must replace the rejected Java 8 `UseGCLogFileRotation` logging flags with unified `-Xlog` logging when moving their runtime to Java 25.
- [ ] **Step 9:** Final commit must contain no generated temporary files, no `.sass-*` folders, no `/tmp` outputs, and no unrelated generated documentation churn. Report the exact remaining warnings, if any, rather than claiming a warning-free build without evidence.
