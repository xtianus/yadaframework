# `net.yadaframework.persistence.YadaMoney`

| Method | Description |
|---|---|
| `fromDatabaseColumn` | Create a YadaMoney from the value stored in the database |
| `isAtLeast` | Returns true if the current value is equal to the amount specified or higher |
| `getSum` | Returns a sum. |
| `getSubtract` | Return a new YadaMoney where the value is the subtraction of the current value and the argument. |
| `getDivide` | Return a new YadaMoney where the value is the division of the current value by the argument. |
| `getMultiply` | Return a new YadaMoney where the value is the multiplication of the current value by the argument. |
| `getRoundValue` | Returns the value with N decimal places |
| `toIntString` | Convert to a string with no decimal places (truncated) |
| `compareTo` | Compares values. |
