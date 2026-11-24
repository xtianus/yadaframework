# Yada AI Knowledge Index

| File | Read this when you need... |
|---|---|
| [yada_dictionary.md](yada_dictionary.md) | the entry point for the generated Yada code dictionary |
| [yadaWeb_dictionary.md](yadaWeb_dictionary.md) | the public YadaWeb utility, persistence, and DataTables API surface |
| [yadaWebSecurity_dictionary.md](yadaWebSecurity_dictionary.md) | the YadaWebSecurity controllers, helpers, and DAO surface |
| [yadaWebCMS_dictionary.md](yadaWebCMS_dictionary.md) | the YadaWebCMS helpers and persistence API surface |
| [yadaAI_dictionary.md](yadaAI_dictionary.md) | the YadaAI helper and Claude request-builder API surface |
| [ajax_csrf_notes.md](ajax_csrf_notes.md) | Ajax CSRF meta-tag requirements for Yada pages using Spring Security, and how to force the HTTP method of an ajax link, button or legacy datatable command so that a write is protected |
| [workspace_build_notes.md](workspace_build_notes.md) | module linkage, Eclipse/Gradle workspace build behavior, the YadaExamples test setup, and why some libraries are pinned to older versions |
| [yada_tests_notes.md](yada_tests_notes.md) | the YadaTests project: suites, test application, embedded database and Tomcat harness, fixtures and framework quirks found by the tests |
| [bootstrap_views_and_mail_notes.md](bootstrap_views_and_mail_notes.md) | where the Bootstrap version of a view lives, how to resolve a versioned view name, Mailpit capture in development, and mail preview templates |
| [security_tokens_notes.md](security_tokens_notes.md) | the bearer-token contract of autologin, registration, password recovery and email change links: digest storage, issue-time links, purpose/expiry checks, atomic consumption, autologin confirmation, log redaction and the V002 migrations |
| [security_web_defaults_notes.md](security_web_defaults_notes.md) | the response headers, generic login failure and lockout signalling, error response contents, redirect-target and impersonation rules that every `YadaSecurityConfig` application inherits |
| [client_address_notes.md](client_address_notes.md) | how the client/audit IP address is computed, `X-Forwarded-For` trust, the `security/trustedProxies` setting and Tomcat `RemoteIpValve` |
