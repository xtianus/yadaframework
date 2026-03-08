# `net.yadaframework.security.web.YadaRegistrationController`

| Method | Description |
|---|---|
| `handleRegistrationConfirmation` | Processes a registration-confirmation token, creates the user when valid, and returns the outcome. |
| `createNewUser` | Creates and persists a new enabled user profile with encoded credentials and roles. |
| `handleRegistrationRequest` | Validates a registration request, stores the confirmation token, and sends the registration email. |
| `passwordChangeAfterRequest` | Processes the final password-change form reached from a reset request. |
| `passwordResetForm` | Validates a password-reset token and populates the model for the final password-change dialog. |
| `yadaPasswordResetPost` | Creates a password-reset request and sends the recovery email when the user exists. |
| `changeUsername` | Processes an email-change confirmation token and renames the user when it is valid. |
