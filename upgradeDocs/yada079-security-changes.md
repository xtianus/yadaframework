# Yada 0.7.9: code changes for client applications

This list covers only what an application built on Yada has to change when it moves to 0.7.9. Changes inside the framework
that need nothing from the application (the sorting fix, the embedded MariaDB install `tmpDir`, error page wording, log
redaction) are left out. The full notes are in `YadaDocs/src/docs/asciidoc/en/upgrade.adoc`.

The "before" samples come from the code as it was before 0.7.9 (YadaExamples, YadaTools templates). The "after" samples
come from the current YadaExamples / YadaTools code. Section 2 shows typical usage of the removed and new token signatures
rather than copied example code.

## Checklist

| # | Applies if your application... | Breaks at | Section |
|---|---|---|---|
| 1 | maps `YadaRegistrationRequest` / `YadaAutoLoginToken` (any Yada security app) | startup / runtime | [DB migration](#1-flyway-migration-for-hashed-tokens) |
| 2 | calls token APIs (`getToken`, `makeLink(long,long,…)`, `parseLink`, `findByIdAndToken…`) | compile | [Token API](#2-security-token-api) |
| 3 | binds a registration form (subclass of `YadaRegistrationRequest`) | runtime (`YadaInvalidUsageException` or ignored fields) | [Registration binder](#3-registration-form-binder) |
| 4 | has a password reset page | runtime | [Password reset](#4-password-reset-form) |
| 5 | has a login form/modal | wrong messages | [Login template](#5-login-template) |
| 6 | has custom DataTables endpoints | compile | [DataTables](#6-datatables-query-policy) |
| 7 | has multipart (upload) forms | 403 on submit | [Multipart CSRF](#7-multipart-forms-and-csrf) |
| 8 | has its own `accessDeniedHandler` or authentication failure handler | wrong status / lockout ignored | [Security config](#8-security-config-subclasses) |
| 9 | copied the example impersonation / user admin code | GET endpoints = CSRF holes | [Impersonation](#9-impersonation-and-user-admin-post-only) |
| 10 | redirects to a client-supplied location | open redirect | [Local redirects](#10-local-redirect-targets) |
| 11 | copied `/yada/ajaxRedirect.html` | XSS/JSON injection | [Ajax redirect](#11-copied-ajaxredirecthtml) |
| 12 | runs behind a reverse proxy / logs to files / uses big uploads | config | [Configuration](#12-configuration-files) |

---

## 1. Flyway migration for hashed tokens

Tokens are no longer stored as plain numbers: the `token` column is replaced by `tokenHash`, and registration requests get an
`expiration`. Add a migration with your next free version number, e.g. `V<n>__hashed_security_tokens.sql`.
The canonical script is `YadaExamples/src/main/resources/database/V002__hashed_security_tokens.sql`; its outline:

```sql
DELETE FROM YourRegistrationRequestSubclass;   -- every JOINED subclass first
DELETE FROM YadaRegistrationRequest;
DELETE FROM YadaAutoLoginToken;
ALTER TABLE YadaAutoLoginToken DROP COLUMN token, ADD COLUMN tokenHash varchar(64) not null;
ALTER TABLE YadaRegistrationRequest DROP COLUMN token, ADD COLUMN expiration TIMESTAMP NULL, ADD COLUMN tokenHash varchar(64) not null;
```

If the schema still has the old unique indexes on `yadaUserCredentials_id` (both tables) or `trattamentoDati_id`, first add a
plain index on each column in its own `ALTER`, then drop the unique index (`SHOW INDEX FROM YadaAutoLoginToken` gives the names).
Compare the result with the schema generated from your entities.

Rollout: stop all old nodes before migrating, back up the token tables (DDL isn't transactional in MySQL/MariaDB).
All outstanding autologin, registration, password recovery and email change links stop working.

## 2. Security token API

Links now look like `<id>-<64 hex>`. The secret exists only on the instance returned when the token is **issued**, so a link can't
be built from a token loaded from the database (that throws `YadaInvalidUsageException`).

**Removed:** `YadaRegistrationRequest.getToken/setToken`, `YadaAutoLoginToken.getToken/setToken`,
`YadaTokenHandler.makeLink(long, long, Map)`, `YadaRegistrationRequestDao.findByIdAndTokenOrderByTimestampDesc`,
`YadaAutoLoginTokenDao.findByIdAndTokenOrderByTimestampDesc`.

### Issuing a registration / recovery / email change request

Before:
```java
yadaRegistrationRequestDao.save(registrationRequest);
String link = yadaTokenHandler.makeLink(registrationRequest.getId(), registrationRequest.getToken(), params);
```
After:
```java
registrationRequest = yadaRegistrationRequestDao.issue(registrationRequest); // must be new, registrationType set
String link = yadaTokenHandler.makeLink(registrationRequest, params);
```
`save()` is deprecated: it still issues a new request, but throws for an existing one. For email changes you can use
`YadaRegistrationController.handleEmailChangeRequest(credentials, newEmail, request, locale)`.

### Reading and consuming a link

Before:
```java
long[] parts = yadaTokenHandler.parseLink(token);
List<R> found = yadaRegistrationRequestDao.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1], R.class);
```
After:
```java
ParsedSecurityToken parsed = yadaTokenHandler.parseLink(token); // YadaSecurityTokenCodec.ParsedSecurityToken, null if invalid
R request = yadaRegistrationRequestDao.findValid(parsed, YadaRegistrationType.PASSWORD_RECOVERY, R.class); // peek, e.g. to show a form
Optional<T> result = yadaRegistrationRequestDao.consume(parsed, YadaRegistrationType.PASSWORD_RECOVERY, R.class, req -> doWork(req));
```
`consume` locks the row, deletes it and runs the action in one transaction; a rollback leaves the link usable.

### Autologin tokens

Before:
```java
List<YadaAutoLoginToken> found = yadaAutoLoginTokenDao.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]);
```
After:
```java
YadaAutoLoginToken token = yadaAutoLoginTokenDao.issue(userCredentials, expiration);  // or yadaTokenHandler.makeAutoLoginToken(user)
YadaUserCredentials user = yadaAutoLoginTokenDao.consume(parsed);                     // null if invalid/expired/disabled
```
`YadaAutoLoginTokenDao.save` throws for a new token. The `targetAction` passed to `makeAutologinLink` must be a local path that
`YadaWebUtil.safeLocalRedirect` returns unchanged, and encoded **once** (double encoding no longer works).

`GET /autologin/{token}` now shows a confirmation page; only the `POST` logs in. Nothing to code, but if you set your own
`CsrfTokenRepository`, expose it as a bean. Optional message keys: `yada.autologin.confirm.title/.message/.button/.cancel`.

## 3. Registration form binder

`handleRegistrationRequest` rejects requests that already have an id or user. The controller binding the form **must** restrict
the bound fields.

Before: nothing.

After (`YadaExamples` `RegistrationController`):
```java
@InitBinder("yexRegistrationRequest")
void registrationBinder(WebDataBinder binder) {
	YadaRegistrationController.setRegistrationAllowedFields(binder, "name", "surname", "username");
}
```
The model attribute name must match your form object; list only your subclass fields (`email`, `password`, `confirmPassword`
are always allowed). Projects generated by YadaTools use `"name", "surname", "username"`.

## 4. Password reset form

`/yadaPasswordResetPost` binds the new `YadaPasswordResetForm` (only `email`), and with `/passwordChangeAfterRequest` accepts
POST only (GET → 405).

Controller, before:
```java
public String passwordReset(String email, YadaRegistrationRequest yadaRegistrationRequest) {
	yadaRegistrationRequest.setEmail(email);
	yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
```
After:
```java
public String passwordReset(String email, YadaPasswordResetForm yadaPasswordResetForm) {
	yadaPasswordResetForm.setEmail(email);
```

`passwordReset.html`, before:
```html
<form th:action="@{/yadaPasswordResetPost}" th:object="${yadaRegistrationRequest}" method="post">
```
After:
```html
<form th:action="@{/yadaPasswordResetPost}" th:object="${yadaPasswordResetForm}" method="post">
```
Any link or script that calls either url with GET must submit a POST form with the CSRF token instead.
`YadaSecurityUtil.performPasswordChange(form, forceDifferent, request, response)` is a new optional overload.

## 5. Login template

An unknown username now sets `passwordError` like a wrong password; `usernameNotFound` and the `password` attribute are no longer set.

Before:
```html
th:classappend="${usernameNotFound!=null || userDisabled!=null}?is-invalid"
<span th:if="${usernameNotFound}" class="invalid-feedback">User not found...</span>
<input type="password" name="password" ... th:value="${password}">
<span th:if="${passwordError}" class="invalid-feedback">Invalid password...</span>
```
After:
```html
th:classappend="${userDisabled!=null}?is-invalid"
<input type="password" name="password" ...>
<span th:if="${passwordError}" class="invalid-feedback">Invalid email or password...</span>
```
`REQUESTATTR_USERNAMENOTFOUNDFLAG` and `REQUESTATTR_PASSWORD` are deprecated: remove Java references to them.

## 6. DataTables query policy

Custom endpoints must pass a `YadaDataTableQueryPolicy`. The default `/yadaDataTableData` endpoint (tables configured in Java)
needs no change.

Before:
```java
return yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, UserProfile.class, locale);
```
After:
```java
private static final YadaDataTableQueryPolicy USER_TABLE_POLICY = YadaDataTableQueryPolicy.forEntity(UserProfile.class)
	.column("id").column("nickname").column("userCredentials.username").build();
...
return yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, UserProfile.class, locale, USER_TABLE_POLICY);
```
Or reuse a Java-configured table: `yadaDataTableFactory.getSingleton(tableId, locale).getQueryPolicy()`.

- Getters / computed values: `.column("path", false, false)` (output-only), otherwise `build()` fails.
- Extras: `yadaDatatablesRequest.addExtraJsonAttribute("x")` (deprecated) → `.extraOutput("x")` in the policy.
- `getJsonPage(...)` is removed.
- Subclass attributes: build the policy for the concrete subclass (or set it as `dtEntityClass()`).
- Credential-like paths (`password`, `token`, `secret`, ...) and whole entities are refused. A column missing from the policy
  makes the table come back empty with `yadaError`.

## 7. Multipart forms and CSRF

The CSRF token is no longer appended to multipart action urls.

Before (worked thanks to the url token):
```html
<form action="/upload" method="post" enctype="multipart/form-data">
```
After:
```html
<form th:action="@{/upload}" method="post" enctype="multipart/form-data">
```
Or add `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">`.
`yada:actionUpload` adds the token only for absolute local paths, so write it as `yada:actionUpload="@{/...}"`, not relative.

A plain (non-Ajax) form larger than `maxFileUploadSizeBytes` now gets HTTP 413 before the controller, so
`limitExceeded(request)` is never true there. Add a 413 error page in `web.xml`, or upload with Ajax.

## 8. Security config subclasses

Custom access denied handler, before:
```java
exceptionHandling.accessDeniedHandler(myHandler);
```
After (keeps the 413 for oversized uploads):
```java
exceptionHandling.accessDeniedHandler(new YadaUploadLimitAccessDeniedHandler(myHandler));
```

Custom authentication failure handler, before:
```java
if (exception instanceof TooManyFailedAttemptsException) {
```
After:
```java
if (exception instanceof TooManyFailedAttemptsException || exception instanceof LockedException) {
```

Default security headers are back (`X-Frame-Options: DENY`, `nosniff`, no-cache, HSTS on HTTPS, `Referrer-Policy: same-origin`).
If the application frames its own pages, override `configure(HttpSecurity http)`, call `super.configure(http)`, then add
`http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));`. Remove any
of these headers you were adding yourself.

## 9. Impersonation and user admin (POST only)

Applies to code copied from the example `UserProfileController`, the `users`/`usersLegacy` pages and the YadaTools `header.html`.

Controller mappings, before:
```java
@RequestMapping("/user/impersonate/{id}")
@RequestMapping(value="/user/deimpersonate")
@RequestMapping("/userwrite/ajaxDeleteUserProfile")
```
After:
```java
@PostMapping("/user/impersonate/{id}")
@PostMapping("/user/deimpersonate")
@PostMapping("/userwrite/ajaxDeleteUserProfile")   // also ajaxAddOrUpdateUserProfile
```

JavaScript, before / after:
```js
yada.impersonate(url, id);
yada.impersonate(url, id, "POST");
```

Java table definition, add `dtMethod("POST")` to the delete button:
```java
.dtButtonObj("button.delete").dtUrl("@{/dashboard/userwrite/ajaxDeleteUserProfile}").dtMultiRow().dtMethod("POST")
```

Legacy `yada.dataTableCrud` tables: add `"method": "POST"` to the delete definition and to the impersonate extra button
(`ajax: false` buttons with a non-GET method are now submitted with `yada.postNavigate`).

Deimpersonation banner in `header.html`, before:
```html
<a th:href="@{/dashboard/user/deimpersonate}"> ... <button type="button" class="btn-close"></button></a>
```
After:
```html
<form th:action="@{/dashboard/user/deimpersonate}" method="post">
	<button type="submit"> ... <span class="btn-close" aria-hidden="true"></span></button>
</form>
```

`ajaxAddOrUpdateUserProfile` in the example also binds only allowlisted fields (`@InitBinder("userProfile")` with
`setAllowedFields`), takes `userProfileId` explicitly, fails on an unknown id instead of creating a user, and only lets
`failedAttempts` be reset to 0. It's too long to inline: compare your copy with the current example.

## 10. Local redirect targets

`yada.impersonate` now sends `currentLocation` as `location.pathname + location.search` (includes the context path), not the
full `location.href`. Any redirect to a client-supplied location must go through `safeLocalRedirect`, never `redirectString`.

Before:
```java
userSession.impersonate(theUser.getId(), currentLocation, request, response);
...
return yadaWebUtil.redirectString(previousLocation, locale);
```
After:
```java
userSession.impersonate(theUser.getId(), contextRelativeLocation(currentLocation, request), request, response);
...
return "redirect:" + yadaWebUtil.safeLocalRedirect(previousLocation);
```
`contextRelativeLocation` is a private helper in the example `UserProfileController`: it validates with `safeLocalRedirect`
and strips the context path (otherwise a non-root app redirects to `/ctx/ctx/...`). Copy it.

`setDefaultTargetUrlAjaxRequest("/yadaLoginSuccess?targetUrl=...")` must use a local path; external targets now go to `/`.

## 11. Copied `ajaxRedirect.html`

If you copied `/yada/ajaxRedirect.html`, escape the values with the new static `YadaWebUtil.escapeJsonString`:

Before:
```html
{"redirect":"[(${targetUrl}?:@{${targetUrlRelative}})]",
"newTab":"[[${newTab}]]"
}
```
After:
```html
<th:block th:with="redirectUrl=${targetUrl}?:@{${targetUrlRelative}}">{"redirect":"[(${@yadaWebUtil.escapeJsonString(redirectUrl)})]",
"newTab":"[[${newTab}]]"
}</th:block>
```

## 12. Configuration files

`logback.xml` (production), add:
```xml
<logger name="org.springframework.web.servlet.PageNotFound" level="ERROR"/>
```
Optionally add `%X{forwardedFor}` to log patterns: `getClientIp()` no longer returns the `[for ...]` text.

`conf.webapp.xml`, only when needed:
```xml
<security><trustedProxies><proxy>10.0.0.1</proxy></trustedProxies></security>  <!-- only without Tomcat RemoteIpValve -->
<database><dialect>org.hibernate.dialect.MariaDBDialect</dialect></database>     <!-- MariaDB via MySQL Connector/J -->
```
Behind a reverse proxy, prefer Tomcat's `RemoteIpValve` and leave `trustedProxies` empty.

Optional token lifetimes (defaults shown): `security/registration/expirationHours` 24,
`security/passwordReset/expirationMinutes` 30, `security/emailChange/expirationMinutes` 30.

`web.xml`, for a friendly upload-too-large page:
```xml
<error-page><error-code>413</error-code><location>/errorPage</location></error-page>  <!-- your own error url -->
```

Access logs (Tomcat `AccessLogValve`, nginx...): mask the last segment of `/autologin/`, `/registrationConfirmation/`,
`/passwordReset/` and `/changeEmailConfirm/` urls.

Embedded database: if you rely on a MariaDB started separately on the embedded port, configure the same `datadir` or another
port. On Linux and macOS the data folder path must not contain spaces.
