# `net.yadaframework.core.YadaEmbeddedTomcatTestServer`

| Method | Description |
|---|---|
| `start` | Starts an embedded HTTP-only Tomcat instance on an ephemeral port and mounts the supplied webapp folder. |
| `resolve` | Resolves an application-relative path against the running server base URI. |
| `getBaseUri` | Returns the base URI of the embedded server. |
| `getPort` | Returns the dynamically assigned HTTP port. |
| `close` | Stops and destroys the embedded server and deletes its temporary base directory. |

## Notes

- By default the server infers the target module from the nearest `build` ancestor of `webappDir` and mounts only classpath directories from the same module and matching source set into `/WEB-INF/classes`.
- Constructors also accept explicit extra classpath roots, which are appended after the inferred defaults for nonstandard test layouts.
- The helper hides `WebApplicationInitializer` classes found only in excluded classpath directories from the Tomcat parent classloader, so unrelated test initializers on the shared Gradle test classpath do not affect startup order.
- The helper is intended for integration tests that need a real servlet container lifecycle without calling `await()` or `System.exit()`.
