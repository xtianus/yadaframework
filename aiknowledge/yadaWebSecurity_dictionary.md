# YadaWebSecurity Code Dictionary

## `net.yadaframework.security.components`

| Class | Description |
|---|---|
| [YadaSecurityEmailService](yadaWebSecurity/YadaSecurityEmailService.md) | Builds security-specific emails such as registration, password reset, and email change confirmations. Use when authentication flows must send signed links. |
| [YadaSecurityUtil](yadaWebSecurity/YadaSecurityUtil.md) | Collects higher-level security helpers for access checks, password changes, login state, and controller error parameters. Use from security-aware services and controllers. |
| [YadaTokenHandler](yadaWebSecurity/YadaTokenHandler.md) | Creates and parses signed autologin and one-time security links. Use when flows need expiring links tied to a user and purpose. |

## `net.yadaframework.security.persistence.entity`

| Class | Description |
|---|---|
| [YadaUserCredentials](yadaWebSecurity/YadaUserCredentials.md) | Represents login credentials, roles, and linked social accounts for a user. Use as the persisted authentication record. |
| [YadaUserMessage](yadaWebSecurity/YadaUserMessage.md) | Represents a user-visible message or ticket message with hashing, stack, attachment, and relative-time helpers. Use as the base model for in-app messages. |
| [YadaUserProfile](yadaWebSecurity/YadaUserProfile.md) | Represents the application profile attached to a user account. Use when business data must be linked to credentials and role checks. |

## `net.yadaframework.security.persistence.repository`

| Class | Description |
|---|---|
| [YadaAutoLoginTokenDao](yadaWebSecurity/YadaAutoLoginTokenDao.md) | Handles persistence and query operations for auto login token. Use when services need auto login token-specific lookups or updates. |
| [YadaRegistrationRequestDao](yadaWebSecurity/YadaRegistrationRequestDao.md) | Handles persistence and query operations for registration request. Use when services need registration request-specific lookups or updates. |
| [YadaSocialCredentialsDao](yadaWebSecurity/YadaSocialCredentialsDao.md) | Handles persistence and query operations for social credentials. Use when services need social credentials-specific lookups or updates. |
| [YadaTicketDao](yadaWebSecurity/YadaTicketDao.md) | Handles persistence and query operations for ticket. Use when services need ticket-specific lookups or updates. |
| [YadaTicketMessageDao](yadaWebSecurity/YadaTicketMessageDao.md) | Handles persistence and query operations for ticket message. Use when services need ticket message-specific lookups or updates. |
| [YadaUserCredentialsDao](yadaWebSecurity/YadaUserCredentialsDao.md) | Handles persistence and query operations for user credentials. Use when services need user credentials-specific lookups or updates. |
| [YadaUserMessageDao](yadaWebSecurity/YadaUserMessageDao.md) | Handles persistence and query operations for user message. Use when services need user message-specific lookups or updates. |
| [YadaUserProfileDao](yadaWebSecurity/YadaUserProfileDao.md) | Handles persistence and query operations for user profile. Use when services need user profile-specific lookups or updates. |

## `net.yadaframework.security.web`

| Class | Description |
|---|---|
| [YadaMultipartFormTagProcessor](yadaWebSecurity/YadaMultipartFormTagProcessor.md) | Rewrites multipart form actions so the CSRF token is available in the request URL before servlet multipart parsing. Use it through the Yada security dialect on multipart upload forms. |
| [YadaRegistrationController](yadaWebSecurity/YadaRegistrationController.md) | Exposes registration, password reset, and username change endpoints. Reuse or subclass it when enabling the built-in registration flows. |
