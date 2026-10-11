# YadaFramework
Patterns, good practices and useful code to quickly build enterprise-grade web applications in Java using Spring, Hibernate and Thymeleaf.

Latest documentation: https://yadaframework.net/

## Gradle builds

The repository root is the `yadaframework` build, using Gradle 8.14.3 and Java 21. It includes `YadaWeb`, `YadaWebSecurity`, `YadaWebCMS`, `YadaWebCommerce`, `YadaAi`, `YadaTools`, and `YadaDocs`. There is no application project in this build; `YadaExamples` is independent and `YadaBones` has no Gradle build.

| Working directory | Command | Purpose |
|---|---|---|
| Repository root | `./gradlew projects` | List the seven framework projects |
| Repository root | `./gradlew test` | Run all framework test suites |
| Repository root | `./gradlew build` | Build all framework projects |
| Repository root | `./gradlew :YadaWebSecurity:test` | Run security tests and compile their dependencies and fixtures |
| Repository root | `./gradlew :YadaDocs:asciidoctor` | Generate documentation under `docs/` |
| `YadaWebSecurity` | `../gradlew test` | Run the security suite from its module directory |
| `YadaExamples` | `./gradlew build` | Build the independent example application and its four framework source dependencies |

On Windows, use `gradlew.bat` with the equivalent relative path. Framework build and test graphs exclude application Sass, schema, initialization, and deployment tasks. Explicit framework tasks remain available through module-qualified names.

`shared.gradle.properties`, `loadSharedProperties.gradle`, and `eclipseSetup.gradle` live at the repository root. Module build scripts apply the shared scripts through project-relative paths (`../loadSharedProperties.gradle` and `../eclipseSetup.gradle`); the loader reads `../shared.gradle.properties` relative to the applying project. This also supports applications that include framework source directories in their own settings, such as `LumiligoSite`. Because the loader reads `../shared.gradle.properties` next to the applying project, a sibling application cannot apply it directly; applications get the shared properties by including the framework modules. `eclipseSetup.gradle` must be applied after the loader, since it reads `java_version`.

Root `./gradlew publish` publishes all six libraries. The `YadaTools` publication tasks and `YadaTools - upload to public repo.launch` publish five artifacts: tools, web, security, CMS, and commerce. They do not publish AI. Use `--dry-run` to inspect publication wiring without publishing.

Import the repository root into Eclipse/Buildship for framework work, or import `YadaExamples` for application work. Buildship stores one owning build connection per module directory. To own the shared modules from both builds at the same time, use separate checkouts and separate Eclipse workspaces; within one checkout, switch ownership by reimporting the chosen build. See [workspace build notes](aiknowledge/workspace_build_notes.md) for metadata generation and shared-script ordering.


