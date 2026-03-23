# `net.yadaframework.core.YadaAppConfig`

| Method | Description |
|---|---|
| `getStaticConfig` | Returns the shared startup configuration used before the Spring context is fully available. |
| `getConfigurationFile` | Resolves the main `configuration.xml` file, using the `yada.configurationFile` system property when it is set. |
| `makeCombinedConfiguration` | Builds the reloadable combined configuration tree from the resolved `configuration.xml` file and its includes. |

## Notes

- The static startup path and the Spring-managed bean both read the main configuration file through `getConfigurationFile`, so tests and custom launchers can override `configuration.xml` with the `yada.configurationFile` system property.
- `makeCombinedConfiguration` stores the `PeriodicReloadingTrigger` on the `YadaConfiguration` instance and starts it through configuration lifecycle methods instead of keeping the trigger only in a local variable.
- `destroy` stops the active configuration reloading trigger when the root Spring context closes.
