# `net.yadaframework.security.persistence.repository.YadaRegistrationRequestDao`

| Method | Description |
|---|---|
| `issue` | Persists a new request (id must be null, type required) with a fresh token digest, timestamp and expiration for its type: `security/passwordReset/expirationMinutes` (30), `security/emailChange/expirationMinutes` (30), `security/registration/expirationHours` (24). The returned instance carries the raw secret for the email link. |
| `getLifetimeMillis` | The configured lifetime of a request type. |
| `findValid` | Returns the request of a parsed link when its type, digest and expiration are valid, without using it. |
| `consume` | Locks the row, validates type/digest/expiration, deletes the row and runs a callback in the same transaction (other DAOs join it); a callback exception rolls everything back and leaves the link usable. Returns `Optional.empty()` for an invalid link. |
| `findByEmailAndRegistrationType` | Finds by email and registration type. |
| `findByUserAndRegistrationType` | Finds the requests of a user with a type, e.g. pending email changes. |
| `findByTimestampBefore` | Finds the requests created before a date (maintenance cleanup). |
| `delete` | Deletes the current item. |
| `save` | Deprecated alias of `issue`; throws for a request with an id, so a bound id can never overwrite a stored request. |
