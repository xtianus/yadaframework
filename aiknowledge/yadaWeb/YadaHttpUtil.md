# `net.yadaframework.raw.YadaHttpUtil`

| Method | Description |
|---|---|
| `validateProxy` | Validate a proxy over a given http page, with no proxy authentication |
| `hasCookie` | Checks whether it has cookie. |
| `getCookies` | Returns a cookies. |
| `getOneCookieValue` | Return the value of the first cookie found with the given name. |
| `isAjax` | Tests if a request is an Ajax request |
| `uncompress` | Uncompress an InputStream |
| `isContentTypeText` | Return true if the contentType is of text type: html, css, javascript... |
| `getRequestDocumentType` | Return the document type by checking the request content type and the extension of the request path |
| `getDocumentType` | Returms the content type of a response given the content-type |
| `removePort` | Removes the port from a host address, which could also be ipv4 or ipv6 |
| `extractAddress` | Extract the address from a url, without schema and path but with port. |
| `extractPath` | Extract the path from a url (the servlet context and request parameters will be included) |
| `relativeToAbsolute` | Given the current full address and a relative address, computes the new full address. |
| `getWebappFullAddress` | Returns the current webapp address, e.g. |
| `redirectPermanent` | Sends a permanent HTTP redirect. |
| `redirectTemporary` | Sends a temporary HTTP redirect. |
| `forward` | Forwards the request to another page. |
