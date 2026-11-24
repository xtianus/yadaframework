# `net.yadaframework.security.web.YadaPasswordResetForm`

| Method | Description |
|---|---|
| `getEmail` / `setEmail` | The only field of the password recovery form bound by `YadaRegistrationController.yadaPasswordResetPost`. |

## Notes

- Application pages that show the recovery form use `th:object="${yadaPasswordResetForm}"` and a `YadaPasswordResetForm` controller parameter; the recovery request itself is always built on the server.
