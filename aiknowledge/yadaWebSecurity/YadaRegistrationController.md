# `net.yadaframework.security.web.YadaRegistrationController`

| Method | Description |
|---|---|
| `handleRegistrationConfirmation` | To be called when the link in the registration confirmation email has been clicked. |
| `createNewUser` | Create a new user. |
| `handleRegistrationRequest` | This method should be called by a registration controller to perform the actual registration |
| `passwordChangeAfterRequest` | Default method to change a user password. |
| `passwordResetForm` | To be called in the controller that handles the password recovery link in the email. |
| `yadaPasswordResetPost` | Handles the password reset form |
| `changeUsername` | Change a username after the user clicked on the confirmation email link |
