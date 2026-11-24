# `net.yadaframework.persistence.YadaDataTableDao`

| Method | Description |
|---|---|
| `getConvertedJsonPage` | Returns one DataTables response page as a map of row data and counters. Requires a `YadaDataTableQueryPolicy` for the entity class: the request is checked against it before any query and rejected with a `yadaError` (no data) when it names an undeclared column or extra attribute, orders on a non-orderable column, sends a direction other than asc/desc or an out-of-range order index. Rows hold only policy columns, policy extra outputs, `DT_RowId` and `DT_RowClass`. A failure while reading the page (database error) returns HTTP 200 with `yadaError.error` set to the generic text "Server error"; the exception is only logged. |
