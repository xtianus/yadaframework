# Repository Gradle Root Implementation Plan

> **For agentic workers:** Use the `executing-plans` skill to implement this plan task by task. Steps use checkboxes for tracking. This document authorizes no deployment or publication.

**Goal:** Give the framework one repository-level Gradle build while preserving `YadaExamples` as an independent application that consumes framework sources.

**Architecture:** A root `settings.gradle` includes seven framework projects by their existing directory names. `YadaExamples` keeps its own settings and wrapper and includes the framework modules it needs through sibling-directory mappings. Framework build scripts work both under the framework root and under an external application's settings.

**Tech stack:** Gradle 8.14.3, Groovy build scripts, Java 21, Maven publishing, Eclipse/Buildship.

## Requirements

- The root project is named `yadaframework`, set explicitly with `rootProject.name`.
- The root includes exactly `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools`, and `YadaDocs`. `YadaExamples` and `YadaBones` are excluded; `YadaBones` has no Gradle build script.
- `YadaExamples/settings.gradle` and its wrapper are unchanged. The only edit to `YadaExamples/build.gradle` is the `loadSharedProperties.gradle` path. Its root remains `YadaExamples`, including its schema and Sass paths.
- Project dependency paths, artifact IDs, group, versions, publications, test fixtures, and module output directories are unchanged.
- One framework wrapper exists at the repository root. Framework-module settings and wrappers are removed; the `YadaExamples` wrapper stays.
- `shared.gradle.properties` and `loadSharedProperties.gradle` move from `YadaWeb` to the repository root under the same names. The loader reads `../shared.gradle.properties`. All eight build scripts that apply the loader (the seven framework modules and `YadaExamples`) use `apply from: '../loadSharedProperties.gradle'`. Both paths resolve against the applying project's directory, so they work under the framework root, under `YadaExamples`, and under external consumers that include modules from a checkout, such as `LumiligoSite`. No forwarding stub remains in `YadaWeb`.
- The properties file is not named `gradle.properties`: Gradle would auto-load it only for the framework root build, while `YadaExamples` and external consumers need the explicit loader.
- `eclipseSetup.gradle` moves from `YadaWeb` to the repository root under the same name. The six modules that apply it (`YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools`) use `apply from: '../eclipseSetup.gradle'`. `YadaDocs` and `YadaExamples` do not apply it. Its internal `file('.settings...')` paths resolve against the applying project and are unchanged. No forwarding stub remains in `YadaWeb`.
- Each module's plugin and dependency configuration stays in its own build script. No convention plugins, version catalog, or broad shared configuration are introduced.
- The published `net.yadaframework:yadatools:0.7.7.R4` buildscript dependency stays in `YadaWeb` and `YadaExamples`. A sibling `YadaTools` project cannot supply classes to another project's build script configuration.
- `YadaTools`' SSH plugin 2.11.2 and `YadaExamples`' SSH plugin 2.10.1 belong to separate builds and are not aligned.
- The `YadaTools` publish tasks depend on the publish tasks of `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, and `YadaWebCommerce`. Under the root, those sibling paths resolve, so publishing `YadaTools` publishes five artifacts. This is intended; `YadaAi` is not added to that list.
- Unqualified `publish` from the repository root publishes all six libraries.

## Current state

- All eight wrappers (seven framework modules and `YadaExamples`) use Gradle 8.14.3.
- Root `.gitignore` ignores `*.jar` but un-ignores `gradle-wrapper.jar`, so the root wrapper JAR is trackable as is.
- `YadaDocs/settings.gradle` and `YadaWeb/settings.gradle` contain only comments. `YadaTools/settings.gradle` has its four sibling includes commented out, so its publish tasks fail standalone.
- Every module applies shared scripts with `../YadaWeb/...`, relative to its own project directory.
- Only `loadSharedProperties.gradle` reads `shared.gradle.properties`. No application in this workspace and no template or documentation under `YadaTools` or `YadaDocs` applies the loader or `eclipseSetup.gradle` by path; `LumiligoSite` reaches both only through the module build scripts.
- Tracked Eclipse launch configurations live in `Yada*/Launches/`. `YadaWeb - wrapper update.launch` and `YadaTools - wrapper update.launch` run `wrapper` from their module directory. Several launches use `GRADLE_DISTRIBUTION(WRAPPER)` with a module working directory.
- `aiplans/2026-10-11-yada-tests.md` assumes that no repository root exists (its finding F1). It includes `YadaDocs` through `includeBuild`. That plan is revised after this one is implemented.

## Build entry points

| Working directory | Command | Scope |
|---|---|---|
| Repository root | `./gradlew projects` | Seven framework projects; no example application |
| Repository root | `./gradlew build` | Framework projects with a `build` task |
| Repository root | `./gradlew :YadaWebSecurity:test` | Security tests and required project dependencies |
| Repository root | `./gradlew :YadaDocs:asciidoctor` | Documentation generation |
| `YadaExamples` | `./gradlew projects` | Application root plus its four framework dependencies |
| `YadaExamples` | `./gradlew build` | Independent application and its source dependencies |
| `YadaWebSecurity` | `../gradlew test` | Security suite with required dependency compilation |

On Windows, use `gradlew.bat` with the equivalent relative path. Framework build/test graphs exclude application build, Sass, schema, deployment, and initialization tasks. Explicit framework tasks such as `:YadaWeb:testDbSchema` and `:YadaTools:terraforming` remain available. Documentation output stays under `docs/`.

`YadaWeb` is a sibling of `YadaWebSecurity`, so running `test` from `YadaWebSecurity` runs the security suite and required dependency compilation, without `:YadaWeb:test`. Run `./gradlew test` from the repository root for all framework suites.

## Constraints

- Gradle 8.14.3, Java 21, and current dependency versions are retained.
- Changes are limited to build ownership, wrapper placement, discovery configuration, relocation of the shared properties, loader and Eclipse setup script with their `apply from` paths, the affected launch configurations, and documentation.
- Independently configured applications keep consuming framework sources; they are not required to import the framework root or use Maven binaries.
- Signing and publication configuration is unchanged. Verification uses task graphs and POM generation, not remote publication. Publication dry runs prove task wiring, not signing credentials or remote repository availability.
- Gradle `--dry-run` still configures projects, and existing scripts write ignored Eclipse metadata during configuration. Inspect generated changes even when tasks are skipped.
- Do not overwrite unrelated working-tree changes or commit generated IDE metadata.
- Record the implemented architecture in `aiknowledge`.

## Task 1: Add and validate the framework root

**Files:** Create `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties`. No root `build.gradle`.

- [ ] Record a baseline before edits: working-tree status; project/build graphs for each existing build; test/build outcomes for `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, and `YadaDocs`; the example's graph and build outcome; and `YadaTools` `build` and publication graph outcomes. Record failures and environmental prerequisites as observed.
- [ ] Generate baseline POMs for the six library publications with each module's current wrapper, using the task names listed in Task 3. Keep copies outside build directories.
- [ ] Add root settings with `rootProject.name = 'yadaframework'` and the seven includes; no `projectDir` remappings, application includes, `includeBuild`, or dependency overrides.
- [ ] Copy the complete wrapper from `YadaWeb` to the root, preserving line endings and the executable permission on `gradlew`. Confirm with `git status` that the wrapper JAR is untracked, not ignored.
- [ ] Run `./gradlew --version` and `./gradlew projects` at the root. Expect Gradle 8.14.3, root `yadaframework`, and exactly seven projects.
- [ ] Run `./gradlew build --dry-run` and `./gradlew :YadaWebSecurity:test --dry-run`. Expect framework-only task graphs, including security fixture/dependency compilation.

## Task 2: Consolidate entry points and shared scripts

**Files:** Remove `settings.gradle`, `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` from the seven framework directories. Move `YadaWeb/shared.gradle.properties`, `YadaWeb/loadSharedProperties.gradle`, and `YadaWeb/eclipseSetup.gradle` to the root. Edit `loadSharedProperties.gradle` and the `apply from` lines in the build scripts of `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools`, `YadaDocs`, and `YadaExamples`. Edit `YadaWeb/Launches/YadaWeb - wrapper update.launch`; delete `YadaTools/Launches/YadaTools - wrapper update.launch`.

- [ ] Remove the framework settings and wrapper files; remove wrapper directories only when empty.
- [ ] `git mv` the three shared files to the root. Set the loader's properties path to `../shared.gradle.properties`, the eight loader `apply from` lines to `'../loadSharedProperties.gradle'`, and the six Eclipse setup `apply from` lines to `'../eclipseSetup.gradle'`. `git grep -n "YadaWeb/loadSharedProperties\|YadaWeb/shared.gradle\|YadaWeb/eclipseSetup" -- '*.gradle'` returns nothing. `aiknowledge/workspace_build_notes.md` still names the old path until Task 4.
- [ ] Point `YadaWeb - wrapper update.launch` at `${workspace_loc:/yadaframework}` with the wrapper distribution. Leave the other launches unchanged.
- [ ] Run `./gradlew -p YadaWebSecurity :projects`; expect `yadaframework` and seven projects. Run `./gradlew -p YadaWebSecurity test --dry-run`; expect `:YadaWebSecurity:test` and dependency/fixture compilation, without `:YadaWeb:test`. Run `../gradlew test --dry-run` from `YadaWebSecurity` with the same expectation.
- [ ] From `YadaExamples`, run `./gradlew projects` and `./gradlew build --dry-run`. Expect root `YadaExamples` and exactly `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, and `YadaWebCommerce`. Compare graphs with the baseline.
- [ ] Create a disposable consumer outside the repository whose settings include `YadaWeb` and `YadaWebSecurity` via absolute `projectDir` mappings. Using the repository wrapper with `-p`, run `:projects`, `:YadaWebSecurity:test --dry-run`, and `:YadaWebSecurity:compileTestJava`. Expect the consumer's root, shared-property loading, and fixture compilation. Remove the consumer afterward.
- [ ] In each of the three build contexts (root, `YadaExamples`, disposable consumer), filter `:YadaWeb:properties` for `yada_version` and `java_version`; both resolve. These are subproject `ext` properties and do not appear in the root project's output.

## Task 3: Verify builds, application consumption, and publications

**Files:** No planned changes. Modify a module build script only to fix an observed regression; publication names and coordinates stay unchanged.

- [ ] Run root `./gradlew test`, then `./gradlew build`. Verify suites, fixture compilation, framework JARs, and documentation project compilation. Separate environmental or baseline failures from regressions.
- [ ] From `YadaExamples`, run `./gradlew build`. If Sass, plugin, or environment requirements block it, record the limitation and verify `./gradlew compileJava`.
- [ ] Run `./gradlew :YadaDocs:asciidoctor`. Output stays under `docs/`; inspect generated changes before staging.
- [ ] Run these POM tasks with the root wrapper and compare the XML semantically with the baseline POMs. Artifact IDs, group, version, dependency coordinates and scopes, and publication metadata are unchanged; no root artifact dependency appears.

| Module | Qualified POM task |
|---|---|
| `YadaWeb` | `:YadaWeb:generatePomFileForYadaWebLibraryPublication` |
| `YadaWebSecurity` | `:YadaWebSecurity:generatePomFileForYadaWebSecurityLibraryPublication` |
| `YadaWebCMS` | `:YadaWebCMS:generatePomFileForYadaWebCMSLibraryPublication` |
| `YadaWebCommerce` | `:YadaWebCommerce:generatePomFileForYadaWebCommerceLibraryPublication` |
| `YadaAi` | `:YadaAi:generatePomFileForYadaAiLibraryPublication` |
| `YadaTools` | `:YadaTools:generatePomFileForYadaToolsLibraryPublication` |

- [ ] Run `./gradlew :YadaTools:publishYadaToolsLibraryPublicationToLocalRepoRepository --dry-run -PrepoPath=/tmp/yada-root-plan-publish` and `./gradlew :YadaTools:publishYadaToolsLibraryPublicationToPublicRepoRepository --dry-run`. Both graphs include the four sibling publish tasks; nothing is published.
- [ ] Audit the diff for buildscript bootstrap changes, root-relative module paths, dependency upgrades, signing changes, and example edits beyond the loader path.

## Task 4: IDE import, dependency discovery, and documentation

**Files:** Modify `.github/dependabot.yml`, `README.md`, and `aiknowledge/workspace_build_notes.md`. Keep `.settings/`, `.project`, and `.classpath` untracked.

**Out of scope:** `YadaDocs/src/docs/asciidoc/en/setup/gettingStarted.adoc`, `YadaDocs/src/docs/asciidoc/en/setup/eclipseSetup.adoc`, `YadaAi/README.md`, `YadaTools/scripts/template.gradle`, and `YadaTools/scripts/fixProject.sh` describe application projects. Per-module `.gitignore` `!gradle-wrapper.jar` lines stay.

- [ ] Set the weekly Gradle update block's `directories` to `/` and `/YadaExamples`, in a single update block. The [Gradle fetcher](https://raw.githubusercontent.com/dependabot/dependabot-core/main/gradle/lib/dependabot/gradle/file_fetcher.rb) follows root settings includes and tolerates an absent root build script. Check that it discovers all seven framework build scripts, the root wrapper, and the application's build, settings, and wrapper ([directory options](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#directories-or-directory)).
- [ ] In `README.md`, document the two entry points, the root wrapper version, module-qualified task usage, and that the framework root has no application project. State that `shared.gradle.properties`, `loadSharedProperties.gradle`, and `eclipseSetup.gradle` live at the root and are applied by project-relative paths. Applications that apply the loader or the Eclipse setup directly must use the root paths, for example `../yadaframework/loadSharedProperties.gradle`. Warn that root `publish` publishes six libraries and that the `YadaTools` publish tasks and `YadaTools - upload to public repo.launch` publish five artifacts.
- [ ] In `aiknowledge/workspace_build_notes.md`, replace per-module wrapper/settings guidance. Document that modules imported under the framework root use `connection.project.dir=..` and the example keeps its own root connection. Recommend reimporting through Buildship rather than committing connection preferences. Update the locations of all three shared files, including the `eclipseSetup.gradle` ordering note. State that every Java module except `YadaDocs`, which has its own Eclipse setup, applies `eclipseSetup.gradle`.
- [ ] Run root `./gradlew cleanEclipse`, then `./gradlew eclipse` as a separate invocation. Generated metadata for the six modules keeps the `JavaSE-<java_version>` container, the matching `jst.java` facet, and the Buildship nature. Security test sources see `YadaWeb` test fixtures. Verify the root import in Eclipse/Buildship when available; otherwise report the check as pending.
- [ ] Run one `GRADLE_DISTRIBUTION(WRAPPER)` launch with a module working directory, using the `help` task. Buildship finds the root wrapper. Report the check as pending if Eclipse is unavailable.
- [ ] Document that Buildship stores one build connection per module directory: owning the shared modules from both the framework root and the example requires separate checkouts and separate Eclipse workspaces. Within one checkout, switch ownership by reimporting the chosen build.
- [ ] Finish with `git diff --check`, a changed-file review, and the acceptance checklist. Commit only implementation-owned files.

## Acceptance checklist

- [ ] Root settings name the root `yadaframework` and contain exactly the seven framework projects.
- [ ] Exactly two wrapper installations exist: repository root and `YadaExamples`.
- [ ] Framework modules have no settings files.
- [ ] `shared.gradle.properties`, `loadSharedProperties.gradle`, and `eclipseSetup.gradle` exist only at the repository root. No tracked file outside `aiplans` references their `YadaWeb` paths. Both properties resolve from the framework root, `YadaExamples`, and the disposable consumer.
- [ ] No tracked launch configuration runs `wrapper` from a module directory.
- [ ] Framework build/test commands pass or have recorded pre-existing/environmental limitations.
- [ ] The example and the disposable consumer keep their own roots and source dependencies.
- [ ] Maven coordinates, task names, test fixtures, signing, and the published-tool bootstrap are unchanged; the `YadaTools` publish graph includes its four siblings.
- [ ] IDE ownership and dependency scanning are documented; generated artifacts are not staged.

## Rollback

Remove the root settings and wrapper; restore the seven module settings and wrappers. Move `shared.gradle.properties`, `loadSharedProperties.gradle`, and `eclipseSetup.gradle` back to `YadaWeb` and restore the loader path and the fourteen `apply from` lines. Restore the launch configurations, `dependabot.yml`, and documentation. Reimport the chosen build in Buildship; repository changes do not restore ignored IDE connection metadata.
