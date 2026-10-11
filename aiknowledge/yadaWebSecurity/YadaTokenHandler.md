# `net.yadaframework.security.components.YadaTokenHandler`

| Method | Description |
|---|---|
| `makeAutoLoginToken` | Creates and persists an autologin token for the given user. |
| `makeAutologinLink` | Returns the autologin link generated from the given parameters |
| `makeLink` | Builds the opaque id-token bearer fragment used in autologin and registration links; it does not sign the link or its query parameters. |
| `extendAutologinLink` | Adds a string of parameters to the target action link |
| `parseLink` | Splits a token-link string into the two components: id and token. |

The current link format uses a numeric row ID and numeric secret. The entity generators use `SecureRandom` with 63 random bits. Both token entities persist the secret directly; token hashing is not provided by this API. Autologin expiration is checked in the login controller, and tokens are reusable. Registration cleanup is separate from redemption and does not provide a redemption-time expiry check.
