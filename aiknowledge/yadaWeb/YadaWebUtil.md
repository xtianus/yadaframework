# `net.yadaframework.components.YadaWebUtil`

| Method | Description |
|---|---|
| `getFormFragment` | Returns the view name of a form fragment for the configured bootstrap version. |
| `isFragment` | Checks if an object holds a thymeleaf fragment instance. |
| `hasGlobalError` | Checks if a global error with the given code already exists in the binding result. |
| `relinkBindingResult` | Creates a new BindingResult associated with a new instance of the same type, preserving all error information from the original BindingResult. |
| `isMultipartMissing` | Returns true if the MultipartFile has not been uploaded at all, not even an empty file |
| `registerDynamicMapping` | Registers a request mapping dynamically at runtime. |
| `getContentUrlRelative` | Returns the application-relative url of a file from the contents folder |
| `getCookieValue` | Returns the value of a cookie, null if not defined |
| `saveBase64Image` | Save to disk a base64 image received from javascript. |
| `autowireAndInitialize` | Perform autowiring of an instance that doesn't come from the Spring context, e.g. |
| `isEmpty` | Checks whether a paged row wrapper is null or empty. |
| `getRequestMapping` | Returns the last part of the current request, for example from "/some/product" returns "product" |
| `removeLanguageFromOurUrl` | If the url is a full url that points to our server, make it relative to the server and strip any language in the path. |
| `passThrough` | Copies request parameters into the model. |
| `addOrUpdateUrlParameter` | Adds a url parameter or change its value if present |
| `getFullUrl` | Returns a full url, including the server address and any optional request parameters. |
| `isErrorPage` | Returns true if we are in a forward that should display an error handled by YadaController.yadaError() or YadaGlobalExceptionHandler |
| `downloadTempFile` | Copies the content of a file to the Response then deletes the file. |
| `downloadFile` | Copies the content of a file to the Response then optionally deletes the file. |
| `redirectString` | Creates a redirect string to be returned by a @Controller, taking into account the locale in the path. |
| `enhanceUrl` | Creates a new URL string from a starting one, taking into account the locale in the path and optional url parameters. |
| `downloadZip` | Make a zip file and send it to the client. |
| `sortLocalEnum` | Sorts a localized enum according to the locale specified |
| `saveAttachment` | Save an uploaded file to a temporary file |
| `sanitizeUrl` | Fix a url so that it valid and doesn't allow XSS attacks |
