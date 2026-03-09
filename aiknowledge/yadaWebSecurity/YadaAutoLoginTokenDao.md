# `net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao`

| Method | Description |
|---|---|
| `findByYadaUserCredentials` | Returns the list of objects associated with the YadaUserCredentials |
| `findByIdAndTokenOrderByTimestampDesc` | Returns the objects that match both id and token (should be no more than one I guess) |
| `deleteExpired` | Deletes expired elements |
| `save` | Saves the current data. |
