# Workspace Build Notes

- `YadaWebSecurity` is a Gradle multi-project build that includes `:YadaWeb` through [YadaWebSecurity/settings.gradle](/c:/work/gits/YadaDevelopment/yadaframework/YadaWebSecurity/settings.gradle) and uses `testImplementation(testFixtures(project(':YadaWeb')))` in [YadaWebSecurity/build.gradle](/c:/work/gits/YadaDevelopment/yadaframework/YadaWebSecurity/build.gradle).
- The test fixture classes `net.yadaframework.core.YadaEmbeddedTomcatTestServer` and `net.yadaframework.core.YadaTestConfigurationScope` live in [YadaWeb/src/testFixtures/java/net/yadaframework/core](/c:/work/gits/YadaDevelopment/yadaframework/YadaWeb/src/testFixtures/java/net/yadaframework/core).
- Eclipse metadata for `YadaWebSecurity` is regenerated with `.\gradlew eclipse` from the `YadaWebSecurity` directory. The generated `.classpath` must expose the `/YadaWeb` project dependency with test code enabled so `YadaWebSecurity` test sources can resolve `YadaWeb` test fixtures.
