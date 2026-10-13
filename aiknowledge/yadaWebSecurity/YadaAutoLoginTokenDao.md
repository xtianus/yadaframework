# `net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao`

| Method | Description |
|---|---|
| `issue` | Persists a new token for a user with a required expiration, storing only the digest; the returned instance carries the raw secret. |
| `findValid` | Returns the token of a parsed link when digest, expiration and enabled user are valid, without using it (confirmation page). |
| `consume` | Locks the row (`SELECT ... FOR UPDATE`), validates it, deletes it and returns the enabled user; null when invalid. Single use across application instances. |
| `findByYadaUserCredentials` | Returns the unexpired tokens of a user. |
| `deleteExpired` | Maintenance: deletes expired tokens and tokens without expiration. |
| `save` | Merges an existing token; new tokens must use `issue`. |
