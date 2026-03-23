# `org.springframework.web.multipart.commons.YadaCommonsMultipartResolver`

| Method | Description |
|---|---|
| `resolveMultipart` | Wraps the request with a Yada multipart request and stores a request attribute when the configured upload size is exceeded. |
| `limitExceeded` | Checks whether the current request hit the configured multipart upload limit. |

## Notes

- When multipart parsing fails because the request exceeds the configured limit, the resolver keeps the request flowing to controller code instead of failing the whole dispatch.
- The fallback wrapper exposes query-string parameters and an empty multipart file map, so controller code can still detect the overflow and return an explicit response.
