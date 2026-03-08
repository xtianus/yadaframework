# `net.yadaframework.security.components.YadaTokenHandler`

| Method | Description |
|---|---|
| `makeAutoLoginToken` | Create a new YadaAutoLoginToken for the given user that expires after the configured amount of hours (config/security/autologinExpirationHours) |
| `makeAutologinLink` | Return the autologin link generated from the given parameters |
| `makeLink` | Create a token-link |
| `extendAutologinLink` | Add a string of parameters to the target action link |
| `parseLink` | Splits a token-link string into the two components: id and token. |
