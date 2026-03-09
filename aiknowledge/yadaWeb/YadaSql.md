# `net.yadaframework.persistence.YadaSql`

| Method | Description |
|---|---|
| `getOrderByNative` | Returns the "order by" statement in MySql syntax |
| `union` | Adds a query as a union of the current one. |
| `deleteFrom` | Starts a delete query. |
| `selectFrom` | Starts or extends a select query. |
| `selectFromReplace` | Replaces the current select section. |
| `set` | Adds assignments to the current set clause. |
| `updateSet` | Starts an update query. |
| `instance` | Creates an empty query builder. |
| `join` | Adds join clauses if missing. |
| `type` | Adds a JPQL TYPE condition. |
| `dtype` | Adds a native DTYPE condition. |
| `where` | Adds a where condition. |
| `whereNotEmpty` | Adds a where condition only when a collection has values. |
| `whereIn` | Adds an IN condition. |
| `having` | Adds a having condition. |
| `and` | Queues an AND operator for the next condition. |
| `or` | Queues an OR operator for the next condition. |
| `xor` | Queues an XOR operator for the next condition. |
| `startSubexpression` | Starts a grouped subexpression. |
| `endSubexpression` | Closes the current grouped subexpression. |
| `groupBy` | Adds group-by clauses. |
| `orderBy` | Adds order-by clauses. |
| `orderByNative` | Adds MySQL order-by clauses from a page request. |
| `limit` | Sets the query limit. |
| `clearWhere` | Clears all where conditions. |
| `toCount` | Transforms the query into a count query. |
| `toSelectFrom` | Replaces the current select-from section. |
| `nativeQuery` | Creates a native JPA query from the built SQL. |
| `query` | Creates a JPQL query from the built SQL. |
| `add` | Merges joins, filters, grouping, ordering, and parameters from another query. |
| `sql` | Returns the built SQL string. |
| `overwriteQuery` | Replaces the whole query text. |
