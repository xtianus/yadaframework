# `net.yadaframework.security.components.YadaTokenHandler`

| Method | Description |
|---|---|
| `makeAutoLoginToken` | Creates and persists an autologin token for the given user. |
| `makeAutologinLink` | Returns the autologin link generated from the given parameters |
| `makeLink` | Builds the signed id-token fragment used in autologin and registration links. |
| `extendAutologinLink` | Adds a string of parameters to the target action link |
| `parseLink` | Splits a token-link string into the two components: id and token. |
