# `net.yadaframework.raw.YadaHttpUtil`

| Method | Description |
|---|---|
| `validateProxy` | Validates a proxy over a given http page, with no proxy authentication |
| `hasCookie` | Checks whether the request contains a cookie with the given name. |
| `getCookies` | Returns all cookies from the request as a name-value map. |
| `getOneCookieValue` | Returns the value of the first cookie found with the given name. |
| `isAjax` | Checks whether a request is an Ajax request |
| `uncompress` | Uncompress an InputStream |
| `isContentTypeText` | Returns true if the contentType is of text type: html, css, javascript... |
| `getRequestDocumentType` | Returns the document type by checking the request content type and the extension of the request path |
| `getDocumentType` | Classifies a content type string into the Yada document-type constants. |
| `removePort` | Removes the port from a host address, which could also be ipv4 or ipv6 |
| `extractAddress` | Extracts the address from a url, without schema and path but with port. |
| `extractPath` | Extracts the path from a url (the servlet context and request parameters will be included) |
| `relativeToAbsolute` | Given the current full address and a relative address, computes the new full address. |
| `getWebappFullAddress` | Returns the current webapp address, e.g. |
| `redirectPermanent` | Sends a permanent HTTP redirect. |
| `redirectTemporary` | Sends a temporary HTTP redirect. |
| `forward` | Forwards the request to another page. |
