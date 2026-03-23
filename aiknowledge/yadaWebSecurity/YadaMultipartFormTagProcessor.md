# `net.yadaframework.security.web.YadaMultipartFormTagProcessor`

| Method | Description |
|---|---|
| `doProcess` | Detects multipart POST forms and appends the current CSRF token to the rendered action URL when the token is not already present. |

## Notes

- The processor runs inside `YadaDialectWithSecurity`.
- It rebuilds the rendered action URL with `UriComponentsBuilder`, so existing query parameters stay in the query string and fragments such as `#done` remain at the end of the URL after the CSRF parameter is added.
- It is relevant for oversized multipart uploads because Spring Security can validate the CSRF token from the URL even when multipart body parsing fails before the hidden field is available.
