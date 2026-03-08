# YadaWeb Code Dictionary

## `net.yadaframework.components`

| Class | Description |
|---|---|
| [YadaEmailBuilder](yadaWeb/YadaEmailBuilder.md) | Builds templated email payloads with recipients, model data, inline resources, and attachments. Use before handing the message to YadaEmailService. |
| [YadaEmailService](yadaWeb/YadaEmailService.md) | Loads Thymeleaf mail templates and sends HTML or text emails. Use when application code needs links, batching, and Yada mail conventions. |
| [YadaFileManager](yadaWeb/YadaFileManager.md) | Keeps attached files, derived image variants, and temporary uploads consistent on disk and in the database. Use for upload, clone, move, rename, and cleanup flows. |
| [YadaJobManager](yadaWeb/YadaJobManager.md) | Starts, pauses, resumes, and completes persistent background jobs tracked in the database. Use when long-running work must survive requests and expose state. |
| [YadaKeyRateLimiter](yadaWeb/YadaKeyRateLimiter.md) | Applies per-key rate limiting backed by logged timestamps. Use when requests must be throttled independently for each identifier. |
| [YadaLongRunningExclusive](yadaWeb/YadaLongRunningExclusive.md) | Serializes long-running work so only one invocation with the same lock can execute at a time. Use around expensive tasks that must not overlap. |
| [YadaNotify](yadaWeb/YadaNotify.md) | Builds notification-modal responses and page actions returned to the browser. Use from controllers when Ajax flows must show feedback or trigger client behavior. |
| [YadaNotifyData](yadaWeb/YadaNotifyData.md) | Stores one notification entry inside a Yada modal response. Use through YadaNotify to add messages, redirects, or follow-up actions. |
| [YadaSimpleRateLimiter](yadaWeb/YadaSimpleRateLimiter.md) | Enforces a fixed request rate without sleeping the caller. Use when you need a fast pass-or-fail throttle. |
| [YadaSleepingRateLimiter](yadaWeb/YadaSleepingRateLimiter.md) | Enforces a fixed request rate by delaying the caller until it is safe to continue. Use when work should be smoothed instead of rejected. |
| [YadaUtil](yadaWeb/YadaUtil.md) | Collects low-level helpers for strings, dates, files, JSON, reflection, localization, shell calls, and entity cloning. Use when Yada code needs reusable non-web utilities. |
| [YadaWebUtil](yadaWeb/YadaWebUtil.md) | Collects web-layer helpers for URLs, redirects, uploads, downloads, cookies, fragments, modals, and request inspection. Use from controllers, views, and web support code. |

## `net.yadaframework.core`

| Class | Description |
|---|---|
| [YadaConfiguration](yadaWeb/YadaConfiguration.md) | Exposes framework configuration values resolved from properties and setup data. Use when code needs environment-specific Yada settings. |
| [YadaRegistrationType](yadaWeb/YadaRegistrationType.md) | Enumerates the purposes of a YadaRegistrationRequest, such as registration, password recovery, email change, and social signup. Use it to distinguish security-link workflows. |

## `net.yadaframework.persistence`

| Class | Description |
|---|---|
| [YadaDataTableDao](yadaWeb/YadaDataTableDao.md) | Executes paged queries and converts the results to DataTables JSON payloads. Use behind server-side Yada DataTable endpoints. |
| [YadaMoney](yadaWeb/YadaMoney.md) | Wraps money amounts as integer minor units and exposes safe arithmetic helpers. Use instead of floating-point math for prices and totals. |
| [YadaSql](yadaWeb/YadaSql.md) | Builds SQL or JPQL fragments incrementally with conditional joins, filters, grouping, ordering, unions, and parameters. Use when dynamic queries are clearer than Criteria or manual string concatenation. |

## `net.yadaframework.persistence.entity`

| Class | Description |
|---|---|
| [YadaAttachedFile](yadaWeb/YadaAttachedFile.md) | Represents a persisted attachment and the operations needed to place, move, rename, and order its physical files. Use as the model for Yada-managed uploads. |
| [YadaJob](yadaWeb/YadaJob.md) | Represents job data with the small behaviors attached to that record. Use as the persisted model for job. |
| [YadaManagedFile](yadaWeb/YadaManagedFile.md) | Represents managed file data with the small behaviors attached to that record. Use as the persisted model for managed file. |
| [YadaPersistentEnum](yadaWeb/YadaPersistentEnum.md) | Represents persistent enum data with the small behaviors attached to that record. Use as the persisted model for persistent enum. |

## `net.yadaframework.persistence.repository`

| Class | Description |
|---|---|
| [YadaAttachedFileDao](yadaWeb/YadaAttachedFileDao.md) | Handles persistence and query operations for attached file. Use when services need attached file-specific lookups or updates. |
| [YadaBrowserIdDao](yadaWeb/YadaBrowserIdDao.md) | Handles persistence and query operations for browser ID. Use when services need browser ID-specific lookups or updates. |
| [YadaClauseDao](yadaWeb/YadaClauseDao.md) | Handles persistence and query operations for clause. Use when services need clause-specific lookups or updates. |
| [YadaFileManagerDao](yadaWeb/YadaFileManagerDao.md) | Handles persistence and query operations for file manager. Use when services need file manager-specific lookups or updates. |
| [YadaJobDao](yadaWeb/YadaJobDao.md) | Handles persistence and query operations for job. Use when services need job-specific lookups or updates. |
| [YadaJobSchedulerDao](yadaWeb/YadaJobSchedulerDao.md) | Handles persistence and query operations for job scheduler. Use when services need job scheduler-specific lookups or updates. |
| [YadaLocaleDao](yadaWeb/YadaLocaleDao.md) | Handles persistence and query operations for locale. Use when services need locale-specific lookups or updates. |
| [YadaPersistentEnumDao](yadaWeb/YadaPersistentEnumDao.md) | Handles persistence and query operations for persistent enum. Use when services need persistent enum-specific lookups or updates. |

## `net.yadaframework.raw`

| Class | Description |
|---|---|
| [YadaHttpUtil](yadaWeb/YadaHttpUtil.md) | Provides HTTP-oriented helpers for cookies, redirects, compressed payloads, and URL parsing. Use when working below the MVC layer. |
| [YadaIntDimension](yadaWeb/YadaIntDimension.md) | Represents image or rectangle dimensions with comparison helpers. Use when sizing, cropping, or comparing media bounds. |
| [YadaLookupTable](yadaWeb/YadaLookupTable.md) | Stores values behind a single-key lookup without repeated map boilerplate. Use when one computed key should resolve to one cached value. |
| [YadaLookupTableFive](yadaWeb/YadaLookupTableFive.md) | Stores values behind a five-key lookup without nested map boilerplate. Use when combinations of five keys need fast retrieval. |
| [YadaLookupTableFour](yadaWeb/YadaLookupTableFour.md) | Stores values behind a four-key lookup without nested map boilerplate. Use when combinations of four keys need fast retrieval. |
| [YadaLookupTableSix](yadaWeb/YadaLookupTableSix.md) | Stores values behind a six-key lookup without nested map boilerplate. Use when combinations of six keys need fast retrieval. |
| [YadaLookupTableThree](yadaWeb/YadaLookupTableThree.md) | Stores values behind a three-key lookup without nested map boilerplate. Use when combinations of three keys need fast retrieval. |
| [YadaNetworkUtil](yadaWeb/YadaNetworkUtil.md) | Finds the machine's public network addresses. Use in diagnostics or environment discovery. |
| [YadaRegexUtil](yadaWeb/YadaRegexUtil.md) | Caches patterns and applies regional regex extraction and replacement helpers. Use when repeated regex work needs better performance or bounded replacements. |

## `net.yadaframework.selenium`

| Class | Description |
|---|---|
| [YadaSeleniumUtil](yadaWeb/YadaSeleniumUtil.md) | Wraps common Selenium actions and wait patterns used in Yada browser tests. Use in test code to write shorter, more reliable browser interactions. |

## `net.yadaframework.web`

| Class | Description |
|---|---|
| [YadaCropImage](yadaWeb/YadaCropImage.md) | Stores one pending crop operation, including the source upload, target file metadata, and desktop, mobile, or PDF crop settings. Use it inside a YadaCropQueue during upload flows. |
| [YadaCropQueue](yadaWeb/YadaCropQueue.md) | Keeps the session-scoped queue of images that still need cropping, together with the crop and destination redirects. Use it to drive multi-image crop workflows across requests. |
| [YadaDatatablesRequest](yadaWeb/YadaDatatablesRequest.md) | Represents the server-side request parameters sent by DataTables, including paging, ordering, search, and extra JSON attributes. Use it in controller or DAO code that serves Yada tables. |
| [YadaPageRequest](yadaWeb/YadaPageRequest.md) | Represents a pageable request with page, size, sort, and load-previous options. Use it to drive Yada pagination and bookmark-friendly scrolling. |
| [YadaPublicSuffix](yadaWeb/YadaPublicSuffix.md) | Parses public-suffix data to split hostnames into registrable domains and subdomains. Use when cookies or multi-tenant domains depend on the effective top-level domain. |

## `net.yadaframework.web.datatables`

| Class | Description |
|---|---|
| [YadaDataTable](yadaWeb/YadaDataTable.md) | Builds the root server-side DataTables configuration object. Use as the entry point for Yada's fluent DataTables API. |

## `net.yadaframework.web.datatables.config`

| Class | Description |
|---|---|
| [YadaDataTableButton](yadaWeb/YadaDataTableButton.md) | Builds data table button metadata for a Yada DataTable. Use while composing the table structure and actions. |
| [YadaDataTableColumn](yadaWeb/YadaDataTableColumn.md) | Builds data table column metadata for a Yada DataTable. Use while composing the table structure and actions. |
| [YadaDataTableConfirmDialog](yadaWeb/YadaDataTableConfirmDialog.md) | Builds data table confirm dialog metadata for a Yada DataTable. Use while composing the table structure and actions. |
| [YadaDataTableHTML](yadaWeb/YadaDataTableHTML.md) | Builds data table HTML metadata for a Yada DataTable. Use while composing the table structure and actions. |
| [YadaDataTableLanguage](yadaWeb/YadaDataTableLanguage.md) | Builds data table language metadata for a Yada DataTable. Use while composing the table structure and actions. |

## `net.yadaframework.web.datatables.options`

| Class | Description |
|---|---|
| [YadaDTBreakpoint](yadaWeb/YadaDTBreakpoint.md) | Builds the DT breakpoint section of a DataTables configuration. Use while composing a Yada DataTable. |
| [YadaDTColumnDef](yadaWeb/YadaDTColumnDef.md) | Builds the DT column def section of a DataTables configuration. Use while composing a Yada DataTable. |
| [YadaDTColumns](yadaWeb/YadaDTColumns.md) | Builds the DT columns section of a DataTables configuration. Use while composing a Yada DataTable. |
| [YadaDTOptions](yadaWeb/YadaDTOptions.md) | Builds the DT options section of a DataTables configuration. Use while composing a Yada DataTable. |
| [YadaDTResponsive](yadaWeb/YadaDTResponsive.md) | Builds the DT responsive section of a DataTables configuration. Use while composing a Yada DataTable. |
| [YadaDTResponsiveDetails](yadaWeb/YadaDTResponsiveDetails.md) | Builds the DT responsive details section of a DataTables configuration. Use while composing a Yada DataTable. |
