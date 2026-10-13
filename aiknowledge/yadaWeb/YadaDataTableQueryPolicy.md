# `net.yadaframework.persistence.YadaDataTableQueryPolicy`

| Method | Description |
|---|---|
| `forEntity` | Starts a builder for an explicit (custom endpoint) policy; `column(path)`, `column(path, searchable, orderable)` and `extraOutput(path)` add paths and `build()` validates them against the entity and fails fast: credential names in any segment (map keys too), whole entities and embeddables are refused, and searchable/orderable paths must be persistent fields of that class (subclass-only attributes need a policy for the subclass). |
| `forConfiguredTable` | Builder used by `YadaDataTableProxy.getQueryPolicy()`: same checks, but a column that is not a persistent field (getter, computed value, subclass-only attribute) becomes output-only (never searched or sorted; sorting on it is rejected) with one WARN per table and column. Subclass-only values are read reflectively from each row. |
| `getColumnPaths` / `getExtraOutputPaths` | Return the declared paths; a column path is both the query path and the json path of the value. |
| `isSearchable` / `isOrderable` | Return the server permission of a column. |
| `isSecretName` | Tells whether a field name is a credential that is never queried or returned (`password`, `token`, `tokenHash`, `secret`, ...). |

`YadaDataTableProxy.getQueryPolicy()` derives the policy of a configured table from its column names (or data paths), skipping selection, command and javascript-function columns. `YadaDataTableDao` binds map keys as parameters converted to the map key type (`Locale`, enum, number, String), with a parameter name that hex-encodes the key so that `en-US` and `en_US` never collide.
