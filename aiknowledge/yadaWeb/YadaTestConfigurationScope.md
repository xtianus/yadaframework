# `net.yadaframework.core.YadaTestConfigurationScope`

| Method | Description |
|---|---|
| `YadaTestConfigurationScope` | Sets the `yada.configurationFile` system property to a specific configuration file and clears cached static Yada configuration for the lifetime of the scope. |
| `close` | Restores the previous system property and cached static configuration after the test server stops. |

## Notes

- Only one scope is active at a time inside the JVM.
- `close` stops the active scoped configuration reloading trigger before restoring the previous static configuration, so embedded-container tests do not leave `ReloadingTrigger-*` threads behind.
- Use it around embedded-container tests that need a dedicated `configuration.xml` without changing the default startup configuration for the rest of the build.
