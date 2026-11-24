# `net.yadaframework.security.components.YadaTokenHandler`

| Method | Description |
|---|---|
| `makeAutoLoginToken` | Issues and persists an autologin token for the given user with a mandatory expiration (default `security/autologinExpirationHours`); also deletes expired tokens. |
| `makeAutologinLink` | Builds `<server>/autologin/<id>-<secret>?action=<encoded action>[hash]` from a token just returned by `makeAutoLoginToken`; throws `YadaInvalidUsageException` for a reloaded token (no raw secret) or an action that `YadaWebUtil.safeLocalRedirect` doesn't accept unchanged. |
| `makeLink` | Builds the `<id>-<secret>` fragment of a registration request just returned by `YadaRegistrationRequestDao.issue`, plus optional parameters; throws for a reloaded request. |
| `extendAutologinLink` | Adds a string of parameters to the target action link |
| `parseLink` | Returns a `YadaSecurityTokenCodec.ParsedSecurityToken` (id, secret) or null for any malformed value; logs nothing. |

Links are opaque bearer tokens, not signed links: the `action` parameter is validated at creation and again at every redirect exit. The database stores only a SHA-256 digest of each secret (see `YadaSecurityTokenCodec`), so a link can be built only from the instance returned at issue time; a caller that needs a link later issues a new token.
