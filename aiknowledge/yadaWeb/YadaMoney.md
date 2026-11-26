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
| `toString(Locale)` | Formats the amount with `NumberFormat.getNumberInstance(locale)` and the decimal pattern `0.00`: number formatting only, no currency symbol and no currency conversion. The digit set follows the locale numbering system: the plain `ar` locale is JDK-version dependent (Arabic-Indic digits before Java 24, Latin digits since Java 24's CLDR), so `ar-u-nu-arab` and `ar-u-nu-latn` are the stable explicit choices. |
| `compareTo` | Compares values. |
