# `net.yadaframework.persistence.YadaDataTableQueryPolicy`

| Method | Description |
|---|---|
| `forEntity` | Starts a builder for an entity class; `column(path)`, `column(path, searchable, orderable)` and `extraOutput(path)` add paths and `build()` validates them against the entity (no credential names, no whole entities, persistent fields for queryable paths). |
| `getColumnPaths` / `getExtraOutputPaths` | Return the declared paths; a column path is both the query path and the json path of the value. |
| `isSearchable` / `isOrderable` | Return the server permission of a column. |
| `isSecretName` | Tells whether a field name is a credential that is never queried or returned (`password`, `token`, `tokenHash`, `secret`, ...). |

`YadaDataTableProxy.getQueryPolicy()` derives the policy of a configured table from its column names (or data paths), skipping selection, command and javascript-function columns.
