# `net.yadaframework.core.YadaConfiguration`

| Method | Description |
|---|---|
| `copyTo` | Copy the already initialised configuration to a different instance |
| `getForB3B4B5` | Returns the value that matches the configured Bootstrap version. |
| `isLocalFlag` | Gets the value of any boolean config key defined in the /config/local configuration file (it should reside on the developers computer in a personal folder, not shared). |
| `getLocalConfig` | Gets the value of any config key defined in the /config/local configuration file (it should reside on the developers computer in a personal folder, not shared). |
| `isPreserveImageExtension` | Checks if the image extension has to be preserved when converting. |
| `getPasswordResetSent` | Returns the redirect URL used after sending a password-reset email. |
| `getRegistrationConfirmationLink` | Returns the registration-confirmation link with locale handling. |
| `emailBlacklisted` | Checks if an email address has been blacklisted in the configuration |
| `useDatabaseMigrationAtStartup` | True if during startup YadaAppConfig should run the FlyWay migrate operation |
| `flywayTableName` | Name of the flyway schema history table. |
| `useDatabaseMigrationOutOfOrder` | "Out of order" flag in FlyWay https://flywaydb.org/documentation/configuration/parameters/outOfOrder |
| `getCountryForLanguage` | Returns the configured country for a language. |
| `getWebappAddress` | Returns the webapp address without a trailing slash. |
| `getRoleSpringName` | Returns the Spring Security role name for a configured role ID. |
| `getRoleIds` | Converts from role names to role ids |
| `getRoleId` | Returns a role ID. |
| `getRoleKey` | Returns the message key for a configured role ID. |
| `containsInProperties` | Returns the subset of properties whose keys contain the search text. |
| `encodePassword` | Returns whether password encoding is enabled. |
| `seleniumWaitQuick` | Returns the short Selenium wait timeout. |
| `seleniumWait` | Returns the default Selenium wait timeout. |
| `seleniumWaitSlow` | Returns the long Selenium wait timeout. |
| `getString` | Returns a string. |
| `getInt` | Returns an int. |
| `getLong` | Returns a long. |
| `setReloadingTrigger` | Stores the periodic Commons Configuration reloading trigger owned by this configuration instance. |
| `startReloadingTrigger` | Starts the stored periodic reloading trigger when runtime configuration reloading is enabled. |
| `stopReloadingTrigger` | Shuts down the stored periodic reloading trigger and clears the reference. |
| `addConfigurationReloadListener` | Adds a configuration reload listener. |

## Notes

- `copyTo` copies the combined-configuration builder, the resolved immutable configuration, and the active reloading trigger reference, so static startup access and the Spring bean observe the same reloading state.
- `stopReloadingTrigger` shuts down the trigger executor, which is what releases the `ReloadingTrigger-*` thread used by Commons Configuration.
