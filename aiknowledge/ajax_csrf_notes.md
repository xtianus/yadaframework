# Ajax CSRF Notes

Yada ajax requests use CSRF data exposed in the HTML page `<head>`. Pages that run with Spring Security CSRF protection include these meta tags in their shared header fragment:

- `_csrf`, containing the token value
- `_csrf_header`, containing the HTTP header name
- `_csrf_parameter`, containing the request parameter name

`yada.ajax.js` reads those meta tags at script load. When `_csrf` and `_csrf_header` are present, it adds the configured CSRF header to same-origin jQuery ajax requests that use unsafe HTTP methods.
