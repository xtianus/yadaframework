# `net.yadaframework.security.web.YadaMultipartFormTagProcessor`

| Method | Description |
|---|---|
| `doProcess` | On multipart POST forms, removes the CSRF parameter from the rendered action query, if a template wrote it there. |

## Notes

- The processor runs inside `YadaDialectWithSecurity` and never adds a token: `th:action` forms get the hidden `_csrf` field from Spring Security, `yada:actionUpload` (`YadaActionUploadAttrProcessor`) adds it only for absolute local paths accepted by `YadaWebUtil.isSafeLocalRedirect`, and `yada.ajax.js` sends the header from the `_csrf`/`_csrf_header` meta tags.
- It unescapes the attribute value, removes only the CSRF parameter with `UriComponentsBuilder`, and keeps the other query parameters and the fragment unchanged.
- The `MultipartFilter` parses multipart bodies before Spring Security, so the hidden field is readable by `CsrfFilter`. When the body exceeds `maxFileUploadSizeBytes` it is not parsed: a plain form then fails CSRF validation and `YadaUploadLimitAccessDeniedHandler` (installed by `YadaSecurityConfig`) answers 413 before any controller; a request with a valid CSRF header reaches the controller, where `YadaCommonsMultipartResolver.limitExceeded(request)` is true.
