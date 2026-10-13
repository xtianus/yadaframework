# YadaWebSecurity Code Dictionary

## `net.yadaframework.security.components`

| Class | Description |
|---|---|
| [YadaSecurityEmailService](yadaWebSecurity/YadaSecurityEmailService.md) | Builds security-specific emails such as registration, password reset, and email change confirmations. Use when authentication flows must send bearer-token links. |
| [YadaSecurityUtil](yadaWebSecurity/YadaSecurityUtil.md) | Collects higher-level security helpers for access checks, password changes, login state, and controller error parameters. Use from security-aware services and controllers. |
| [YadaSecurityTokenCodec](yadaWebSecurity/YadaSecurityTokenCodec.md) | Generates 256-bit token secrets, digests them with SHA-256 per purpose, and strictly parses `<id>-<secret>` links. Use for any new bearer-token link. |
| [YadaTokenHandler](yadaWebSecurity/YadaTokenHandler.md) | Creates and parses opaque bearer-token links for autologin and registration requests; links can be built only at issue time and the autologin action must be a safe local path. The link parameters are not signed: expiry, purpose and single use are enforced by the DAOs. |

## `net.yadaframework.security.persistence.entity`

| Class | Description |
|---|---|
| [YadaUserCredentials](yadaWebSecurity/YadaUserCredentials.md) | Represents login credentials, roles, and linked social accounts for a user. Use as the persisted authentication record. |
| [YadaUserMessage](yadaWebSecurity/YadaUserMessage.md) | Represents a user-visible message or ticket message with hashing, stack, attachment, and relative-time helpers. Use as the base model for in-app messages. |
| [YadaUserProfile](yadaWebSecurity/YadaUserProfile.md) | Represents the application profile attached to a user account. Use when business data must be linked to credentials and role checks. |

## `net.yadaframework.security.persistence.repository`

| Class | Description |
|---|---|
| [YadaAutoLoginTokenDao](yadaWebSecurity/YadaAutoLoginTokenDao.md) | Issues, validates and atomically consumes single-use autologin tokens (digest only, mandatory expiration, enabled user). |
| [YadaRegistrationRequestDao](yadaWebSecurity/YadaRegistrationRequestDao.md) | Issues registration, password recovery and email change requests with expiring token digests, and consumes them once per purpose under a row lock together with the change they authorize. |
| [YadaSocialCredentialsDao](yadaWebSecurity/YadaSocialCredentialsDao.md) | Handles persistence and query operations for social credentials. Use when services need social credentials-specific lookups or updates. |
| [YadaTicketDao](yadaWebSecurity/YadaTicketDao.md) | Handles persistence and query operations for ticket. Use when services need ticket-specific lookups or updates. |
| [YadaTicketMessageDao](yadaWebSecurity/YadaTicketMessageDao.md) | Handles persistence and query operations for ticket message. Use when services need ticket message-specific lookups or updates. |
| [YadaUserCredentialsDao](yadaWebSecurity/YadaUserCredentialsDao.md) | Handles persistence and query operations for user credentials. Use when services need user credentials-specific lookups or updates. |
| [YadaUserMessageDao](yadaWebSecurity/YadaUserMessageDao.md) | Handles persistence and query operations for user message. Use when services need user message-specific lookups or updates. |
| [YadaUserProfileDao](yadaWebSecurity/YadaUserProfileDao.md) | Handles persistence and query operations for user profile. Use when services need user profile-specific lookups or updates. |

## `net.yadaframework.security.web`

| Class | Description |
|---|---|
| [YadaMultipartFormTagProcessor](yadaWebSecurity/YadaMultipartFormTagProcessor.md) | Removes a CSRF token written in the action query of multipart POST forms; the token travels in the hidden field or the Ajax header, never in a URL. Runs automatically in the Yada security dialect. |
| [YadaRegistrationController](yadaWebSecurity/YadaRegistrationController.md) | Exposes registration, password reset, and username change endpoints. Reuse or subclass it when enabling the built-in registration flows; restrict registration form binding with `setRegistrationAllowedFields`. |
