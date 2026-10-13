# `net.yadaframework.security.web.YadaRegistrationController`

| Method | Description |
|---|---|
| `setRegistrationAllowedFields` | Static helper for an application `@InitBinder`: restricts registration form binding to `email`, `password`, `confirmPassword` plus the given subclass fields. |
| `handleRegistrationConfirmation` | Consumes a REGISTRATION link once (purpose, expiry, lock) and creates the user in the same transaction; logs in after commit and returns the outcome. |
| `createNewUser` | Creates and persists a new enabled user profile with encoded credentials and roles. |
| `handleRegistrationRequest` | Rejects a request that already has an id or user (`YadaInvalidUsageException`), validates it, replaces older requests of the same email and issues the confirmation token, then sends the email. |
| `passwordChangeAfterRequest` | Processes the final password-change form reached from a reset request. |
| `passwordResetForm` | Checks a PASSWORD_RECOVERY link (purpose and expiry) without using it and populates the model for the final password-change dialog. |
| `yadaPasswordResetPost` | Binds only `YadaPasswordResetForm.email`; when the user exists, replaces older recovery requests with a new one and emails its link. |
| `handleEmailChangeRequest` | Issues an EMAIL_CHANGE request for a user (replacing older ones) and emails its `/changeEmailConfirm/<token>` link. |
| `changeUsername` | Consumes an EMAIL_CHANGE link once and renames the user in the same transaction. |
