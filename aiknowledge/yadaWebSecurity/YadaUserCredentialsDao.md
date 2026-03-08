# `net.yadaframework.security.persistence.repository.YadaUserCredentialsDao`

| Method | Description |
|---|---|
| `changeUsername` | Renames a user account from the old username to the new one. |
| `changePassword` | Changes the password |
| `create` | Creates a configured user if one does not already exist. |
| `findByUserProfileId` | Finds the credentials linked to the given user-profile ID. |
| `findFirstByUsername` | Finds the first credentials record for the given username. |
| `updateLoginTimestamp` | Updates the login timestamp of the user |
| `incrementFailedAttempts` | Updates the login failed attempts counter for the user |
| `resetFailedAttempts` | Resets the login failed attempts counter for the user |
