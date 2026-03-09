# `net.yadaframework.persistence.repository.YadaLocaleDao`

| Method | Description |
|---|---|
| `prefetchLocalValuesRecursive` | Prefetches localized string maps on the given target to avoid lazy loading issues, recursively following JPA relations. |
| `findAllWithLocalValues` | Finds all entities of the given type, then initializes all localized string attributes defined as Map&lt;Locale, String> |
| `findOneWithLocalValues` | Finds an object of the given id, then initializes all localized string attributes defined as Map&lt;Locale, String> |
| `findOneWithLocalValuesRecursive` | Finds an object of the given id, then initializes all localized string attributes defined as Map&lt;Locale, String> with recursion on the fields up to the default max depth |
| `getLocalValue` | Get the localized value of an entity's attribute |
