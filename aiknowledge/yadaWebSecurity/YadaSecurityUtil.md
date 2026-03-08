# `net.yadaframework.security.components.YadaSecurityUtil`

| Method | Description |
|---|---|
| `checkUrlAccess` | Checks if the current user has access to the specified path |
| `isLockedOut` | Checks if a user has been suspended for excess of login failures |
| `changePassword` | Sets a new password using the configured encoder. |
| `userCanChangeRole` | Returns true if the given user can change the role targetRoleId on users, based on its own roles |
| `userCanEditUser` | Checks if the roles of the actingUser allow it to change some target user based on its roles, as configured by &lt;handles> A target user can be changed only when both its current roles and its new roles can all be changed by any of the roles of the acting user. |
| `userCanImpersonate` | Checks if the roles of the actingUser allow it to impersonate the targetUser based on its roles, as configured by &lt;handles> The actingUser can impersonate the targetUser only when there is at least one role of actingUser that can change all roles of targetUser. |
| `logout` | Logs out the currently logged-in user |
| `copyLoginErrorParams` | Copy all not-null login error parameters to the Model |
| `addLoginErrorParams` | Adds to some url the login error request parameters defined in YadaAuthenticationFailureHandler so that the login modal can show them. |
| `generateClearPassword` | Generate a 32 characters random password |
| `performPasswordChange` | Changes the user password and log in |
| `loggedIn` | Checks if the current user is logged in. |
| `clearAnySavedRequest` | Clears any saved request. |
| `caseAnonAuth` | Returns one of two values depending on whether the current user is authenticated. |
| `registrationRequestCleanup` | Deletes old or duplicate registration requests for the same email and type. |
| `hasCurrentRole` | Checks if the current user has the provided role, case sensitive. |
| `hasAnyRole` | Checks if the current user has any of the provided roles, case sensitive. |
