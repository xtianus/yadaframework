# Ajax CSRF Notes

Yada ajax requests use CSRF data exposed in the HTML page `<head>`. Pages that run with Spring Security CSRF protection include these meta tags in their shared header fragment:

- `_csrf`, containing the token value
- `_csrf_header`, containing the HTTP header name
- `_csrf_parameter`, containing the request parameter name

`yada.ajax.js` reads those meta tags at script load. When `_csrf` and `_csrf_header` are present, it adds the configured CSRF header to same-origin jQuery ajax requests that use unsafe HTTP methods.

## Forcing the HTTP method of an ajax request

`makeAjaxCall` sends GET unless the request is multipart or the element belongs to a `yadaFormGroup` whose first
form declares another method. A state-changing link or button is therefore unprotected by CSRF until its method is
changed, because Spring Security only validates unsafe methods.

`data-yadaMethod`, written as `yada:method="POST"` in a template, forces the method of the ajax call made by an
anchor, a button or an input. A multipart upload still forces `POST` and a `yadaFormGroup` still wins over it.

`yada.dataTableCrud` of `yada.datatablesLegacy.js` sends its row commands with GET as well. The `deleteDef` and the
`extraButtons` definitions accept an optional `method` property for the same reason; it is opt-in, so tables that do
not set it keep the previous behaviour.
