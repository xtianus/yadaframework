# `net.yadaframework.security.persistence.repository.YadaUserCredentialsDao`

| Method | Description |
|---|---|
| `changeUsername` | Change a username |
| `changePassword` | Change the password |
| `create` | Creates a user when it doesn't exists, using the configured attributes. |
| `findByUserProfileId` | Find the credentials for the given user profile id |
| `findFirstByUsername` | Find the first user with the given username |
| `updateLoginTimestamp` | Updates the login timestamp of the user |
| `incrementFailedAttempts` | Updates the login failed attempts counter for the user |
| `resetFailedAttempts` | Resets the login failed attempts counter for the user |
