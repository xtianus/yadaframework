# `net.yadaframework.raw.YadaRegexUtil`

| Method | Description |
|---|---|
| `escapeDots` | Escapes dots in regular expressions, replacing them with \. |
| `getOrCreatePattern` | Return a compiled Pattern, either from the cache or new. |
| `createMatcher` | Return a matcher for the given source document and pattern. |
| `replaceInRegion` | Performs a find-and-replace in a delimited area of the source, returning the source with replacements applied. |
| `extractInRegion` | Searches a region of text for a pattern that contains a capturing group, and returns the captured group. |
