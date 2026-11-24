# `net.yadaframework.security.components.YadaSecurityTokenCodec`

| Method | Description |
|---|---|
| `generateSecret` | Returns 32 `SecureRandom` bytes as 64 lowercase hex characters. |
| `digest` | SHA-256 of a fixed domain prefix, the purpose (e.g. `AUTOLOGIN`, `PASSWORD_RECOVERY`) and the secret, as 64 hex characters; this is what token entities store in `tokenHash`. |
| `matches` | Constant-time comparison of a presented secret with a stored digest for a purpose. |
| `parse` | Strictly parses `<positive id>-<64 lowercase hex>` into `ParsedSecurityToken(id, secret)` (whose `toString` hides the secret), null otherwise. |
| `format` | Builds `<id>-<secret>`. |
| `isExpired` | Static expiry rule of every token: expired when `now >= expiration` or the expiration is null. |
