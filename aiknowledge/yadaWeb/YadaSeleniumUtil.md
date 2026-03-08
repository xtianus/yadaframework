# `net.yadaframework.selenium.YadaSeleniumUtil`

| Method | Description |
|---|---|
| `runJavascript` | Run some javascript. |
| `urlMatches` | Returns true if the current url matches the specified pattern |
| `urlContains` | Returns true if the current url contains the specified string at any position |
| `getSourceSnippet` | Returns a part of the page source. |
| `getByJavascript` | Returns a value calculated via javascript. |
| `typeAsHuman` | Insert some text slowly into a field |
| `findOrNull` | Returns the first element matched by the selector, or null if not found |
| `getTextIfExists` | Search for text |
| `foundByText` | Returns true if an element contains the given text (literally) |
| `findById` | Get an element by id |
| `foundById` | Checks if an element with a given id exists |
| `foundByClass` | Checks if at least one element with the given class exists |
| `makeWebDriver` | Creates a new browser instance positioning the window |
| `positionWindow` | Sets the browser window position and size. |
| `clickByJavascript` | Click on the given element using javascript. |
| `randomClick` | Click on the given element in a range between 20% and 80% of the dimensions |
| `waitUntilAttributeNotEmpty` | Waits until the attribute contains some non-empty text |
| `waitWhileEmptyText` | Waits until the selected element contains some non-empty text (warning: this method may not work as expected) |
| `waitUntilPresent` | Wait until the selector matches an element |
| `waitUntilVisible` | Wait until the selector matches a visible element |
| `waitWhilePresent` | Wait until the selector matches zero elements |
| `waitWhileVisible` | Waits until the element becomes invisible. |
| `waitUntilLost` | Waits until the element is no more attached to the DOM (stale). |
| `relativeToAbsolute` | Given a relative address, computes the new full address. |
| `takeScreenshot` | Take a browser screenshot and move it to the specified path |
