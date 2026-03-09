# `net.yadaframework.persistence.YadaMoney`

| Method | Description |
|---|---|
| `fromDatabaseColumn` | Creates a YadaMoney from the raw database value. |
| `isAtLeast` | Returns true if the current value is equal to the amount specified or higher |
| `getSum` | Returns a new amount obtained by adding the given value. |
| `getSubtract` | Returns a new amount obtained by subtracting the given value. |
| `getDivide` | Returns a new amount obtained by dividing by the given factor. |
| `getMultiply` | Returns a new amount obtained by multiplying by the given factor. |
| `getRoundValue` | Returns the monetary amount rounded to the requested number of decimals. |
| `toIntString` | Converts to a string with no decimal places (truncated) |
| `compareTo` | Compares values. |
