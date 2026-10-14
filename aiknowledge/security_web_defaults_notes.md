# Security Web Defaults Notes

Defaults of YadaWebSecurity and YadaWeb that affect every application extending `YadaSecurityConfig`.

## Response headers

- `YadaSecurityConfig` keeps the Spring Security default headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache, no-store, max-age=0, must-revalidate` with `Pragma: no-cache` and `Expires: 0` on dynamic responses, `X-XSS-Protection: 0`, and `Strict-Transport-Security` on secure requests only. It adds `Referrer-Policy: same-origin`.
- Resources served by the `YadaWebConfig` resource handlers keep their own `max-age`. A subclass that frames its own pages sets `frameOptions(sameOrigin)`. Behind a TLS-terminating proxy HSTS appears only when the request is seen as secure (`RemoteIpValve`, see `client_address_notes.md`).

## Login failure

- `YadaAuthenticationFailureHandler` sets `passwordError` for a wrong password and for an unknown username alike (`DaoAuthenticationProvider` hides `UsernameNotFoundException`), so a single wrong password doesn't tell whether an account exists. The form still reveals existence in other ways: disabled and locked accounts get their own outcome, and since unknown usernames never lock, an account that locks after `security/maxFailedAttempts`+1 wrong attempts exists. `usernameNotFound` and the `password` request attribute are never set (`REQUESTATTR_USERNAMENOTFOUNDFLAG` and `REQUESTATTR_PASSWORD` are deprecated). Login templates show one generic message and never use `${password}`. The 401 fallback sends "Authentication Failed" without the exception message.
- A lockout is signalled by `YadaUserDetailsService.loadUserByUsername` returning a locked `UserDetails`, so the provider throws `LockedException`; the handler checks both `LockedException` and `TooManyFailedAttemptsException` (the framework itself throws only `LockedException` for a lockout). An unknown username also goes through `incrementFailedAttempts` and updates no row. The lockout branch skips the credentials lookup when the `username` parameter is missing.

## Error responses

- Error pages and Ajax modals carry only the standard HTTP reason phrase; exception and servlet messages are logged, never sent. See `yadaWeb/YadaController.md` and `yadaWeb/YadaGlobalExceptionHandler.md`. A committed response is not completed with a fake ending: the client sees a truncated or aborted response. DataTables database failures answer `yadaError.error` "Server error".

## Redirect targets and impersonation

- A redirect target from the client goes through `YadaWebUtil.safeLocalRedirect` and is used as `"redirect:" + target`; see `yadaWeb/YadaWebUtil.md`.
- `/yadaLoginSuccess?targetUrl=` (`YadaLoginController`) validates the target with `safeLocalRedirect` before adding the locale, and `yada/ajaxRedirect.html` (`YadaViews.AJAX_REDIRECT`) writes `targetUrl`/`targetUrlRelative` with `YadaWebUtil.escapeJsonString`, so the JSON answer can carry no markup or extra property.
- Impersonation and deimpersonation are POST-only in the example application and in the YadaTools project template banner; `yada.impersonate` sends a local path (see `ajax_csrf_notes.md`).

## Uploads

- The CSRF token of a multipart form is a hidden field or an Ajax header, never part of the action url; a plain form over `maxFileUploadSizeBytes` gets 413 from `YadaUploadLimitAccessDeniedHandler` (see `ajax_csrf_notes.md`).
