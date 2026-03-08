# `net.yadaframework.components.YadaUtil`

| Method | Description |
|---|---|
| `ensurePoiTempFolder` | Ensure the temporary folder for POI files exists and is writable. |
| `lazyUnsafeInit` | Utility method for lazy initialization of any object type. |
| `getFilesInFolder` | Returns a list of files from a folder where the name contains the given string, sorted alphabetically |
| `getRandomString` | Returns a random string of the given length with characters in the range "A".."Z", "a".."z", "0".."9" |
| `getRandomText` | Returns a random string (currently an hex random number) |
| `joinIfNotEmpty` | Joins a number of strings, adding a separator only when the strings are not empty. |
| `getTimestampAsRelative` | Given a date in the past, returns a string like "12 minutes ago", "2 hours ago", "today at 12:51", "yesterday at 5:32"... |
| `stringToDouble` | Parse a string as a double, using the correct decimal separator (if any). |
| `addIfNotNull` | Add an element to the list only if the element is not null |
| `addIfMissing` | Add an element to the list only if not there already |
| `getEmptySortedSet` | Create a new TreeSet that sorts values according to the order specified in the parameter. |
| `getDays` | Returns a list of days between two dates included. |
| `getDateFromDateTimeIsoString` | Given a ISO date, a ISO time and a timezone, return the Date. |
| `getRfcDateTimeStringForTimezone` | Returns a string for date and time in the specified timezone and locale |
| `getIsoDateStringForTimezone` | Convert a date in the timezone to a ISO string, like '2011-12-03' |
| `getIsoTimeStringForTimezone` | Convert a time in the timezone to a ISO string, like '10:15' or '10:15:30' |
| `getIsoDateTimeStringForTimezone` | Convert a datetime in the timezone to a ISO string, like '2011-12-03T10:15:30' |
| `makeJsonObject` | Create a single empty "json" object for use in other methods. |
| `getJsonObject` | Returns a nested JSON object at a path. |
| `getJsonArray` | Returns a nested JSON array at a path. |
| `getJsonAttribute` | Given a json stored as a map, returns the value at the specified key, with optional nesting. |
| `getTimezoneOffsets` | Get a list of GMT/UTC time offsets from UTC-12:00 to UTC+14:00 |
| `isEmailValid` | Simple email address syntax check: the format should be X@Y.Y where X does not contain @ and Y does not contain @, nor . |
| `mapToString` | Convert a map of strings to a commaspace-separated string of name=value pairs |
| `getRandom` | Returns a random integer number |
| `findGenericClass` | Finds the concrete class bound to a generic superclass. |
| `joinFiles` | Merges all files matched by a pattern, in no particular order. |
| `relativize` | Finds the path between two files or folders using forward (unix) slashes as a separator |
| `splitHtml` | Split an HTML string in two parts, not breaking words, handling closing and reopening of html tags. |
| `findAvailableFilename` | Ensure that the given filename has not been already used, by adding a counter. |
| `dateValid` | Check if a date is not more than maxYears years from now, not in an accurate way. |
| `getImageDimensionDumb` | Gets image dimensions for given file, ignoring orientation flag |
| `getImageDimension` | Gets the image dimensions considering the EXIF orientation flag. |
| `getRandomElement` | Returns a random element from the list |
| `formatTimeInterval` | Convert from an amount of time to a string in the format xxd:hh:mm:ss |
| `autowire` | Autowires an object that was created outside the Spring context. |
| `autowireAndInitialize` | Autowires and initializes an object created outside the Spring context. |
| `stripCounterFromFilename` | Remove a counter that has been added by #findAvailableName |
| `findAvailableNameHighest` | Returns a file that doesn't already exist in the specified folder with the specified leading characters (baseName) and optional extension. |
| `findAvailableName` | Creates an empty file that doesn't already exist in the specified folder with the specified leading characters (baseName). |
| `prefetchLocalizedStrings` | Force initialization of localized strings implemented with Map&lt;Locale, String>. |
| `prefetchLocalizedStringsRecursive` | Force initialization of localized strings implemented with Map&lt;Locale, String>. |
| `prefetchLocalizedStringListRecursive` | Force initialization of localized strings implemented with Map&lt;Locale, String>. |
| `prefetchLocalizedStringList` | Force initialization of localized strings implemented with Map&lt;Locale, String>. |
| `getObjectToString` | Returns a string representation of the object as Object.toString() does, even if toString() has been overridden |
| `getLocalValue` | Returns the localized value from a map of Locale -> String. |
| `deleteFileSilently` | Deletes a file without reporting any errors. |
| `closeSilently` | Close a closeable ignoring exceptions and null. |
| `sleepRandom` | Sleeps for a random duration inside the given bounds. |
| `sleep` | Sleeps for the given number of milliseconds. |
| `md5Hash` | Create a MD5 hash of a string (from http://snippets.dzone.com/posts/show/3686) |
| `copyStream` | Copies an inputStream to an outputStream. |
| `getFieldNoTraversing` | Get the Field of a given class, even from a superclass but not "nested" in a path |
| `getType` | Reflection to get the type of a given field, even nested or in a superclass. |
| `isCollection` | Check if the last segment of an attribute path is a Collection (List, Set, etc.) |
| `getMessage` | Returns a message. |
| `getNewInstanceSamePackage` | Create an instance of a class that belongs to the same package of some given class |
| `getClassesInPackage` | Return all the classes of a given package. |
| `getBean` | Get any bean defined in the Spring ApplicationContext |
| `minutesAgo` | Returns a timestamp a given number of minutes in the past. |
| `daysAgo` | Returns a timestamp a given number of days in the past. |
| `deleteAll` | Delete all files in a folder that have the specified prefix |
| `deleteSilently` | Delete a file ignoring errors. |
| `deleteIfEmpty` | Deleted a folder only when empty |
| `cleanupFolder` | Removes files from a folder starting with the prefix (can be an empty string) The folder itself is not removed. |
| `getFileNoPath` | Returns the file name given the file path |
| `splitFileNameAndExtension` | Splits a filename in the prefix and the extension parts. |
| `getFileExtension` | Returns a file extension. |
| `shellExec` | Runs an external command configured in Yada settings. |
| `isCodiceFiscaleValid` | Checks whether it is codice fiscale valid. |
| `getRootException` | Returns a root exception. |
| `splitAtWord` | Splits a string near the requested position without breaking the last word. |
| `abbreviate` | Cuts the input string at the given length, optionally keeping the whole last word and adding some dots at the end |
| `copyEntity` | Copies entity. |
| `isType` | Check if a class is of a given type, considering superclasses and interfaces (of superclasses) |
| `dateWithin` | Check if a date is within two dates expressed as month/day, regardless of the year and of the validity of such dates. |
| `roundForwardToHour` | Returns the same calendar object aligned to the next hour |
| `roundBackToHour` | Returns the same calendar object aligned to the previous hour |
| `daysBetween` | Returns the days between two dates. |
| `daysDifference` | Counts the days interval between two dates. |
| `minutesDifference` | Returns the minutes between two dates. |
| `minutesDifferenceAbs` | Returns the absolute value of the minutes between two dates. |
| `millisSinceMidnight` | Returns the number of milliseconds since midnight |
| `roundBackToMidnight` | Rounds back to midnight. |
| `roundBackToMidnightClone` | Create a new calendar rounded back to the start of the day. |
| `roundForwardToAlmostMidnight` | Rounds forward the calendar to the end of the day at 23:59:59.999 |
| `addDaysClone` | Adds or removes the days. |
| `addDays` | Adds or removes the days. |
| `addMinutes` | Adds or removes the minutes. |
| `addHours` | Adds a hours. |
| `addYears` | Adds a years. |
| `sameDay` | Returns true if the two dates are on the same day |
| `createZipProcess` | Create a zip of a set of files using an external process. |
| `createZipFileFromFolders` | Create a zip file of a folder |
| `createZipFile` | Create a zip of a list of files. |
| `roundBackToLastMonthStart` | Rounds back to last month start. |
| `roundBackToMonth` | Rounds back to month. |
| `roundFowardToMonth` | Rounds foward to month. |
| `sortByValue` | Sorts by value. |
| `sortByKey` | Sets the localized sort by key. |
| `ensureSafeFilename` | Converts a candidate filename so that it is valid on all operating systems and browsers, if needed, and also to lowercase. |
