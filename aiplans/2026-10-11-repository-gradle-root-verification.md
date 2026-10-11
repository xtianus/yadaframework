# Repository Gradle Root Verification

Verified on native Linux with Java 21.0.12.1 and Gradle 8.14.3.

## Baseline and build results

Before implementation, each of `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools`, `YadaDocs`, `YadaExamples`, and `LumiligoSite` passed its own `projects`, `build --dry-run`, and `build` commands. Baseline POMs were saved for all six libraries. Standalone `YadaTools` publication dry run failed because its settings excluded the sibling projects required by its publication tasks.

| Check after implementation | Result |
|---|---|
| Root `./gradlew --version` and `./gradlew projects` | Gradle 8.14.3; root `yadaframework`; exactly seven framework projects |
| Root `./gradlew build --dry-run` | Framework tasks only; no example, Sass, schema, initialization, or deployment tasks |
| Root `./gradlew :YadaWebSecurity:test --dry-run` | Security tests and core test-fixture compilation included |
| Root `./gradlew test` | 114 tests; no failures, errors, or skips |
| Root `./gradlew build` | Passed; framework artifacts and documentation project compiled |
| `YadaExamples`: `bash ./gradlew build` | Passed; application distributions and WAR built |
| `LumiligoSite`: `./gradlew build` | Passed; application distributions and WAR built |
| `LumiligoSite`: `./gradlew :test --rerun-tasks` | Fresh run passed; 839 tests, zero failures/errors, one skipped |
| Root `./gradlew :YadaDocs:asciidoctor` | Passed; output under repository `docs/` |
| Six qualified `generatePomFileFor…LibraryPublication` tasks | Passed; XML semantically identical to each standalone baseline POM |
| Tools local and public publication dry runs | Passed; exactly tools, web, security, CMS, and commerce publication tasks |
| Root `./gradlew publish --dry-run -PrepoPath=/tmp/yada-root-plan-publish` | Passed; all six library publish tasks included |
| Root `./gradlew cleanEclipse`, then `./gradlew eclipse` | Both passed in separate invocations |
| `git -c core.whitespace=cr-at-eol diff --check` | Passed; existing CRLF lines retained |

Framework suites comprise 100 core, seven security, one commerce, and six AI tests. `LumiligoSite` skips `RedisSessionIntegrationTest.sessionAttribute_RoundTripsAcrossApplicationContextsAndExistsInRedis()` unless `LML_REDIS_TEST_URL` matches `redis://.+`. The remaining 838 application tests passed in the forced post-change run; the earlier post-change build had reused the successful baseline application test results. Existing compiler/Javadoc deprecation warnings, Gradle 9 compatibility warnings, and Asciidoctor reference/callout warnings occur in the baseline too.

Publication checks generated POMs and inspected dry-run graphs only; they did not publish artifacts or verify signing credentials or remote repositories.

## Build contexts and discovery

- `./gradlew -p YadaWebSecurity :projects` discovers the framework root and all seven modules. `./gradlew -p YadaWebSecurity test --dry-run` and `../gradlew test --dry-run` from the security directory include its suite and core fixture compilation, without `:YadaWeb:test`. Module-directory `help` also passes.
- The independent example retains its root and four framework dependencies. Its projects and build task graph match the baseline exactly. Its settings and all wrapper files retain their original hashes. Its only build script edit is the loader path.
- A disposable consumer outside the repository mapped `YadaWeb` and `YadaWebSecurity` through absolute `projectDir` paths. Its own root, projects, security dry run, and `compileTestJava` all worked. It was removed afterward.
- Directly applying the module loader from an external application reproduces a missing parent properties file. Explicitly loading the repository-root properties file succeeds. The README and knowledge notes distinguish this from the already-verified module-consumption path.
- `:YadaWeb:properties` resolves `yada_version: 0.7.9` and `java_version: 21` under framework, example, and disposable-consumer settings.
- There are exactly two wrapper installations, at the repository root and under `YadaExamples`. Framework modules have no settings files. The three shared files exist only at the root. No tracked file outside the historical plans references their former module locations.
- Every module build script changed only its authorized shared-script apply paths. Dependency coordinates, publication names, signing, bootstrap dependencies, test fixtures, and output paths retain their original configuration.
- Dependabot is configured for `/` and `/YadaExamples` in one weekly block. Inspection of the official [Gradle fetcher](https://raw.githubusercontent.com/dependabot/dependabot-core/main/gradle/lib/dependabot/gradle/file_fetcher.rb) and [settings parser](https://raw.githubusercontent.com/dependabot/dependabot-core/main/gradle/lib/dependabot/gradle/file_fetcher/settings_file_parser.rb) confirms that absent root build files are tolerated, settings includes locate the seven framework build scripts, and wrapper files are collected for each entry point. This is source inspection, not an execution of GitHub's hosted update job. Multiple scan roots use the documented [directories option](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#directories-or-directory).

## Eclipse checks and limitations

Generated metadata for all six shared-script modules contains the JavaSE-21 classpath container and Buildship nature. WTP modules contain exactly one installed `jst.java` facet at version 21. The security classpath exposes `/YadaWeb` with `without_test_code=false`, enabling the core test fixtures. Metadata stays ignored.

Interactive root import and the module-directory `GRADLE_DISTRIBUTION(WRAPPER)` launch remain pending because Eclipse/Buildship is unavailable in this environment. CLI module-directory discovery and `help` passed; those do not substitute for the interactive launch check. Buildship connections and checkout/workspace ownership are documented in `aiknowledge/workspace_build_notes.md`.

Documentation generation changed timestamps in 31 tracked HTML files. Those generated-only changes were inspected and restored; none existed in the initial working tree. Unrelated security dictionary edits and pre-existing untracked plans are preserved.

## Implementation decisions

- Work used the existing application submodule checkout so `LumiligoSite` could exercise its current sibling source mappings. The tradeoff is that implementation and existing user edits share a checkout; staging and review cover only implementation-owned paths.
- The new root wrapper is executable. Its source wrapper was mode 100644, contrary to the plan's executable-permission assumption; the only additional change is the root script's executable bit. The example wrapper retains its original mode and is invoked through Bash on Linux.

Independent final review found no implementation defects and identified the direct-loader documentation caveat above. It was treated as actionable because following the incomplete guidance could break external application configuration; documentation was clarified and both the failing direct-apply case and working explicit-properties alternative were exercised.
