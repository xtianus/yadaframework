# Security Token Notes

Bearer-token links of YadaWebSecurity: autologin (`/autologin/<id>-<secret>`), registration confirmation (`security/registration/confirmationLink`, default `/registrationConfirmation/`), password recovery (`/passwordReset/`) and email change (`/changeEmailConfirm/`).

## Token contract

- The link part is `<positive id>-<64 lowercase hex secret>` (`YadaSecurityTokenCodec`). Rows store only `tokenHash`, the SHA-256 of a domain prefix, the purpose (`AUTOLOGIN` or the `YadaRegistrationType` name) and the secret. No pepper/HMAC: the secrets have 256 random bits.
- Only the instance returned at issue time (`YadaAutoLoginTokenDao.issue` via `YadaTokenHandler.makeAutoLoginToken`, `YadaRegistrationRequestDao.issue`) carries the `@Transient` raw secret; `YadaTokenHandler.makeLink`/`makeAutologinLink` throw `YadaInvalidUsageException` on a reloaded entity. To send a link later, issue a new token.
- Validity: row exists, purpose matches, digest matches (constant time), `now < expiration` (null expiration is invalid). Autologin also needs an enabled user: only `isEnabled()` is checked, so a user locked out by failed password attempts can still log in with a valid autologin link, and that login resets the failure counter. Lifetimes: recovery and email change 30 minutes, registration 24 hours, autologin `security/autologinExpirationHours` (10); cleanup jobs are maintenance only.
- Consumption locks the row with `SELECT ... FOR UPDATE` (`em.find(..., PESSIMISTIC_WRITE)`), deletes it and performs the change in the same transaction; authentication happens after commit. A rolled-back change leaves the link usable. Verified on MariaDB 11.4 (InnoDB); the lock is in the database, so it holds across application instances.
- Registration requests are never updated: `issue` and the deprecated `save` refuse an entity with an id, and `handleRegistrationRequest` throws for a bound id or user. Application controllers restrict registration binding with `YadaRegistrationController.setRegistrationAllowedFields(binder, subclassFields...)` in an `@InitBinder("<model attribute>")`; `/yadaPasswordResetPost` binds `YadaPasswordResetForm` (email only). `/yadaPasswordResetPost` and `/passwordChangeAfterRequest` are `@PostMapping` (GET answers 405), because they delete pending links, send mail or change the password.

## Autologin

- `GET /autologin/{token}?action=` validates the token without using it and renders `/yada/security/autologinConfirm` (in YadaWebSecurity resources), which shows the account and a form; it never submits itself and keeps the link `#fragment` for the redirect.
- `POST /autologin/{token}` (CSRF-protected by Spring Security) consumes the token, changes the session id and logs in. A user who is already logged in is never replaced and the token stays unused.
- `action` must pass `YadaWebUtil.safeLocalRedirect` unchanged when the link is created; every exit redirects to `"redirect:" + safeLocalRedirect(action)`, with no extra URL decoding.
- The confirmation page reads `yada.autologin.confirm.*` from the application bundle, with English defaults.

## Logs

`YadaLogRedaction` (YadaWeb) hides token secrets in paths, query strings and email bodies. `isSecretParameter` is the one list of secret parameters (`token`, `password`, `confirmPassword`, `newPassword`, `_csrf`, also as the last segment of a property path like `userCredentials.newPassword`), used by `redactQueryString` and by the `AuditFilter` DEBUG parameter dump. `AuditFilter` redacts its request lines and the MDC `endpoint`; `YadaGlobalExceptionHandler`, `YadaController.yadaError` and `YadaUploadLimitAccessDeniedHandler` redact the urls and exception texts they log.

Outside the framework: Spring logs the complete url of an unmapped request (e.g. a token link with a trailing slash) at WARN on `org.springframework.web.servlet.PageNotFound`, which the YadaTools logback template, the YadaExamples `env` files and the YadaTests `logback.xml` keep at ERROR; Spring and Hibernate DEBUG logging print full request urls; container and proxy access logs record the GET token urls (the GET of an autologin or recovery link doesn't consume the token). All of these must be kept off or masked in production.

## Migrations

`V002__hashed_security_tokens.sql` in YadaTests and YadaExamples deletes every token row (subclass tables first), drops `token`, adds `tokenHash` and the registration request `expiration`; the YadaExamples one also replaces the stale `yadaUserCredentials_id`/`trattamentoDati_id` unique constraints with plain indexes. `SecurityTokenMigrationRehearsalTest` in both projects replays the upgrade from V001 with legacy rows.
