# Yada 0.7.8 → 0.7.9 Application Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring any web application built on Yada Framework from branch `0.7.8` to branch `0.7.9` so that it compiles, starts, and behaves correctly with CSRF protection and the hardened security defaults.

**Architecture:** The upgrade is an inventory-then-fix pass. Each task has a *Find* pattern that tells whether the application is affected, the *Change* to make, and a *Verify* check. Tasks are ordered so that the application compiles first (Tasks 1-4, whose last step also pulls forward the compile-relevant changes of Tasks 6 and 7), then works at runtime (Tasks 5-10), then is safe to deploy (Tasks 11-13). A *Verify* that needs compiled classes or a running application is never run before the compile gate (Task 4 Step 7). A task whose *Find* returns nothing is ticked off without changes.

**Tech Stack:** Java 21, Gradle 8.14.3, Spring 6.2, Spring Security 6.5, Hibernate 7.4, Thymeleaf 3.1.3, Flyway 13, Tomcat 11, jQuery + `yada.js` / `yada.ajax.js`.

**Spec:** this plan is derived from, and must be read together with:

- [upgradeDocs/yada079-security-changes.md](../upgradeDocs/yada079-security-changes.md): before/after code for every security change (sections referenced below as **SEC §n**)
- [YadaDocs/src/docs/asciidoc/en/upgrade.adoc](../YadaDocs/src/docs/asciidoc/en/upgrade.adoc), "Changes in 0.7.9": the full rationale
- [aiknowledge/bootstrap_views_and_mail_notes.md](../aiknowledge/bootstrap_views_and_mail_notes.md), [aiknowledge/ajax_csrf_notes.md](../aiknowledge/ajax_csrf_notes.md), [aiknowledge/workspace_build_notes.md](../aiknowledge/workspace_build_notes.md)
- `git diff 0.7.8 0.7.9` of the framework repository for anything not listed here

## Global Constraints

- Do not upgrade Thymeleaf beyond 3.1.3 (3.1.4+ rejects `@bean` references in fragment template names, which the new view-name pattern relies on), `vibur-dbcp` beyond 25.0, Spring Framework beyond 6.2.x or Spring Security beyond 6.5.x.
- Never re-disable CSRF or the security headers to make something work: fix the caller.
- Every request that changes state must be a POST carrying the CSRF token; GET is for reads only.
- View names of Bootstrap-specific framework views are never hardcoded with a version: resolve them through `YadaConfiguration` / `YadaWebUtil`.
- Database changes go through a new Flyway migration with the next free version number; existing migrations are never edited.
- Search with both English and Italian keywords; apply every *Find* pattern to Java, HTML templates, JavaScript/TypeScript and XML/properties configuration.

## Review Focus

Conditions that compile and start fine but fail for a real user; each one is pinned by a step in the task named in brackets.

1. A page whose `<head>` lacks the three CSRF meta tags: every ajax POST from it answers 403 [Task 5].
2. The CSRF token changes at login: a page that logs in through an ajax modal and stays open keeps a stale token, so its next ajax POST fails until the page is reloaded [Task 5].
3. A DataTables column that is not in the policy: the table shows no rows and no visible error, only `yadaError` in the JSON [Task 6].
4. A state-changing link still sent with GET: it keeps working, so nothing signals that it is unprotected [Task 5].
5. Pages embedded in an `<iframe>` of the same application, and public pages that relied on browser/proxy caching: both change behaviour because of `X-Frame-Options: DENY` and the no-cache headers [Task 8].

---

### Task 1: Baseline and inventory

- [ ] **Step 1:** On a clean working tree of the application, create a working branch. Record the framework commit in use (`git -C <yadaframework> rev-parse 0.7.9`).
- [ ] **Step 2:** Read the Spec documents above; SEC has a checklist table ("Applies if your application…") that tells which sections matter.
- [ ] **Step 3:** Run the application's compile task (for example `./gradlew compileJava`) and save the output as the list of compile errors to clear in Tasks 3, 4, 6 and 7, all of them by the compile gate (Task 4 Step 7). If the build stops in `sassCompile` before compiling Java because the Dart Sass it downloaded belongs to another operating system (a Windows download used from WSL, or the reverse), apply Task 2 Step 6 first instead of skipping the task with `-x sassCompile`.
- [ ] **Step 4:** List what the application copied or overrode from the framework, because these do not produce compile errors: templates with the same path as a framework view, templates copied under another name (image/entity sorter, notify modal, `ajaxRedirect.html`, login modal, `header.html`), classes excluded from the framework source set and redefined, bean methods overridden in `WebConfig` / `AppConfig` / `SecurityConfig`. Diff each against `git diff 0.7.8 0.7.9 -- <framework file>` and port the relevant changes in the task that owns the topic.

### Task 2: Build, dependencies and IDE workspace

The framework repository is now one Gradle build: root `settings.gradle`, one wrapper (8.14.3), `shared.gradle.properties`, `loadSharedProperties.gradle` and `eclipseSetup.gradle` at the repository root. The per-module `settings.gradle` and wrappers are gone, and all framework tests moved to the `YadaTests` module.

- [ ] **Step 1 (source-linked applications):** the application `settings.gradle` keeps including the modules with `project(':YadaWeb').projectDir = …`; no change is needed there. Find and fix any application script that reads `YadaWeb/shared.gradle.properties` or applies `YadaWeb/loadSharedProperties.gradle` (both moved to the repository root), or that calls `:YadaWeb:testDbSchema` (now `:YadaTests:webTestDbSchema`, run from the framework root).
- [ ] **Step 2 (jar consumers):** set the `net.yadaframework` artifact versions to `0.7.9`. The `yadatools` buildscript classpath must be a published version; keep the working one unless a task from a newer version is needed.
- [ ] **Step 3: compare dependency versions.** The framework moved to the versions below. For each library that the application also declares, remove the declaration when the framework already provides it through `api`, or align it; never pin a lower version.

| Library | 0.7.8 | 0.7.9 |
|---|---|---|
| Spring Framework | 6.2.11 | 6.2.19 |
| Spring Security | 6.5.5 | 6.5.11 |
| Hibernate ORM | 7.1.1.Final | 7.4.13.Final |
| Hibernate Validator / Expressly (EL) | 8.0.3.Final / 5.0.0 | 9.1.4.Final / 6.0.0 |
| Flyway (`flyway-mysql`) | 11.13.1 | 13.10.0 |
| MySQL Connector/J | 9.4.0 | 26.7.0 |
| Jackson databind | 2.20.0 | 2.22.3 |
| Logback | 1.5.21 | 1.6.5 |
| Tomcat (servlet api, embedded) | 11.0.11 | 11.0.27 |
| Selenium (`compileOnly`) | 4.35.0 | 4.51.0 |
| commons-configuration2 / text / lang3 | 2.12.0 / 1.14.0 / 3.18.0 | 2.15.1 / 1.15.0 / 3.21.0 |
| commons-exec, guava, jsoup, metadata-extractor | 1.3, 33.5.0, 1.21.2, 2.19.0 | 1.6.0, 33.7.2, 1.23.2, 2.21.0 |
| MariaDB4j (`compileOnly`), google-api-client-gson, PayPal checkout-sdk (`compileOnly`) | 3.2.0, 2.8.1, 1.0.5 | 3.3.1, 2.9.1, 2.0.0 |
| Thymeleaf, vibur-dbcp | 3.1.3, 25.0 | unchanged, pinned |

  Points to check in the application build: `compileOnly` framework libraries that the application supplies at runtime (Selenium, embedded Tomcat, MariaDB4j, PayPal SDK) must match the new version; any auxiliary configuration that carries its own Jakarta EL or Hibernate tooling (schema generation) must still resolve with Validator 9 / Expressly 6; JUnit dependencies are unaffected because the framework no longer exports test libraries.
- [ ] **Step 4: Verify.** `./gradlew dependencies --configuration runtimeClasspath` shows no framework library forced below the versions above, and the configuration used by the schema generation task resolves (`./gradlew dependencies --configuration <that configuration>`). Running the schema generation itself needs compiled application classes, so it is done in Task 11 Step 1, after the compile gate.
- [ ] **Step 5 (Eclipse/Buildship):** a branch switch leaves stale Eclipse metadata, which shows up as many unrelated errors. Remove the framework projects from the workspace (keep contents), delete every module's `.settings/org.eclipse.buildship.core.prefs`, re-import the *application* root with *Existing Gradle Project*, then run `cleanEclipse` and `eclipse` as two separate invocations. The Java facet and compliance are now set by `eclipseSetup.gradle` from `java_version`.
- [ ] **Step 6 (suggested, applications using the `com.meiuwa.gradle.sass` plugin):** a `sass { download { output = "$rootDir/.sass" } }` folder holds the Dart Sass of the platform that downloaded it first, so the same checkout can't compile Sass from both Windows (Eclipse) and WSL/Linux, and builds on the other platform end up skipping `sassCompile`. Copy from `YadaExamples/build.gradle` (or `YadaTools/scripts/template.gradle`) the block that computes `sassOsName`, `sassArchName`, `sassPlatformKey` and `sassDownloadDir`, put it before the `sass {}` block, set `download { output = sassDownloadDir }`, and add `/.sass-*` to `.gitignore`. Each platform then gets its own `.sass-<os>-<arch>` folder. Keep the Dart Sass version unchanged so that the regenerated CSS does not differ. A previously tracked `.sass/` folder becomes unused; removing it from git is a separate decision. **Verify:** `./gradlew sassCompile` succeeds on every platform used for development, and `git status` shows no stylesheet changed without a source change.
- [ ] **Step 7 (source-linked applications that exclude framework classes):** an application that redefines framework classes and removes them from a framework module with `sourceSets { main { java { exclude … } } }` in its own build (for example its own `YadaArticle` / `YadaProduct`) shares that module's `build/` folder with the framework build. A build started from the framework root, which is now possible because the framework is one Gradle build, compiles the excluded classes into that folder and into the module jar, and the application build does not remove them: the duplicated entities make Hibernate fail at startup with "entity names must be distinct" (other classes fail as duplicate or wrong class versions), and a war built at that moment ships them. Do not work around it in application code. Give the framework modules built by the application their own output folder: in the application `subprojects {}` block that holds the exclusions, add `layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("framework/${project.name}"))`, so that they are built under the application `build/framework/<Module>` and never share outputs with a framework-root build; no framework file changes. Without that setting, run `./gradlew :<Module>:clean` from the application root after every framework-root build and before building a war. **Verify:** the framework jars of the application runtime classpath are under `build/framework/`, and `unzip -l` of each lists none of the excluded classes.

### Task 3: Bootstrap-versioned view names

The Bootstrap version of a framework view is now a folder (`/yada/b3|b4|b5/…`, `/yada/form/b3|b4|b5/…`, `/yadacms/b3|b4/…`), selected by `<bootstrapVersion>` in the configuration. Old names fail at runtime with a template-not-found error, or at compile time for the `YadaViews` constants.

- [ ] **Step 1: Find** `modalConfirm`, `modalGeneric`, `modalNotify`, `/yada/form/`, `/yadacms/`, `YadaViews.AJAX_NOTIFY_B`, `YadaViews.CONFIRM_B`, `getForB3B4B5`, `notificationModalView`.
- [ ] **Step 2: Change** templates:

| Before | After |
|---|---|
| `~{/yada/modalConfirmB3 ::` (also `B4`, `B5`, unversioned) | `~{${@yadaWebUtil.modalConfirmViewName} ::` |
| `~{/yada/modalGenericB3 ::` (also `B4`, `B5`, unversioned `/yada/modalGeneric`) | `~{${@yadaWebUtil.modalGenericViewName} ::` |
| `~{/yada/modalNotifyB3 ::` (also `B4`, `B5`) | `~{${@config.notifyModalView} ::` |
| `~{/yada/form/b4/text ::`, `~{/yada/form/b5/text ::` and every other versioned `/yada/form/b<n>/<name>` | `~{${@yadaWebUtil.getFormFragment('text')} ::` |
| `~{/yada/form/text ::` and every other unversioned `/yada/form/<name>` | see the availability rule below |
| `/yadacms/imageSorter`, `/yadacms/entitySorter`, `/yadacms/imageCropper` | `/yadacms/b3/…` (the former unversioned views); `/yadacms/b4/imageSorter`, `/yadacms/b4/entitySorter` and `/yadacms/b4/imageCropper` already existed in 0.7.8 and keep their names |

  The unversioned form fragments of 0.7.8 were the Bootstrap 3 markup and are now, unchanged, under `/yada/form/b3/`. Not every fragment exists for every version:

| Folder | Fragments |
|---|---|
| `/yada/form/b3/` | `boolean`, `checklist`, `enum`, `fileUpload`, `list`, `list-objects`, `list-values`, `password`, `radio-values`, `range`, `text` |
| `/yada/form/b4/` | `boolean`, `checklist`, `fileUpload`, `list`, `radio-values`, `text` |
| `/yada/form/b5/` | `boolean`, `checklist`, `enum`, `fileUpload`, `list`, `radio-values`, `range`, `text` |

  Use `getFormFragment('<name>')` only when the fragment exists in the folder of the configured `<bootstrapVersion>`; otherwise it selects a missing template and the page fails to render. A fragment missing for that version (`password`, `list-objects`, `list-values` in Bootstrap 4 and 5; also `enum` and `range` in Bootstrap 4) keeps the 0.7.8 markup through the fixed name `/yada/form/b3/<name>`, or uses an application copy. In a Bootstrap 4/5 application, `getFormFragment` also switches a fragment that was unversioned from Bootstrap 3 to version-specific markup: check how each such form looks. Legacy unwrapped fragment expressions (`th:replace="/yada/form/text::field(…)"`) must be wrapped in `~{…}` at the same time, since the name is now an expression. The even older form that passes parameters to the template name without a selector (`th:replace="/yada/form/fileUpload(fieldName=…)"`) also needs the selector added: `~{${@yadaWebUtil.getFormFragment('fileUpload')} :: field(fieldName=…)}`.
- [ ] **Step 3: Change** Java:

| Before | After |
|---|---|
| `YadaViews.AJAX_NOTIFY_B3/B4/B5`, `return "/yada/modalNotifyB3"` | `config.getNotifyModalView()` |
| `YadaViews.CONFIRM_B3/B4/B5` | `yadaWebUtil.getModalConfirmViewName()` |
| `config.getForB3B4B5(view3, view4, view5)` used for view names | `config.getBootstrapView("modalConfirm")` |
| hand-built `"/yada/form/b" + version + "/" + name` | `config.getBootstrapFormView("text")`, only for fragments available in that version (see the table in Step 2) |
| `config.getForB3B4B5(class3, class4, class5)` used for CSS classes | unchanged |

  `YadaViews.AJAX_NOTIFY`, `CONFIRM` and `MODAL_GENERIC` are now view *names* (`"modalNotify"`), not paths: never return them from a controller directly. A configured `config/paths/notificationModalView` is still honoured by `getNotifyModalView()`; controllers that return an application-owned notify view by a hardcoded name should return `config.getNotifyModalView()` so that the configuration is the single source.
- [ ] **Step 4: Verify.** The *Find* patterns of Step 1 return only the new forms. A search restricted to retired names avoids false hits: `rg -nP '/yada/modal|/yada/form/(?!b[345]/)|/yadacms/(?!b[34]/)' <views folder>` must return nothing, while a broad `"/yada/` pattern also matches unversioned views that did not change, such as `/yada/dataTableCrud` and `/yada/ajaxResponseData`. Check every remaining fixed `/yada/b<n>/`, `/yada/form/b<n>/` and `/yadacms/b<n>/` name separately: it must be a deliberate exception (a fragment missing for the configured version, a page that deliberately includes another version). Application views whose names look like framework ones (for example an application-owned `/modalNotifyB3`) are not framework views and are not renamed. Also check that every `getFormFragment` / `getBootstrapFormView` name exists in the folder of the configured Bootstrap version. Rendering is checked in the browser in Task 13 Step 3, because the application compiles only after the compile gate and the ajax modals need the CSRF changes of Task 5.

### Task 4: Small API and behaviour changes

- [ ] **Step 1:** If `WebConfig` redefines `mailPreviewTemplateResolver()`, remove the override: the framework resolver now uses a `classpath:` prefix that works in a war, on the embedded Tomcat and in Eclipse WTP.
- [ ] **Step 2:** Find `replaceWith(` in handlers of `yada.ajax()` responses: a handler that replaced an element with the whole response must use `.replaceWith(response.children())`. Handlers that already select inside the response (`response.find(…)`) are not affected.
- [ ] **Step 3: Pagination.** Find `appendSort(` / `prependSort(` with a direction inside the string: `appendSort("publishDate,desc")` becomes `appendSort("publishDate").desc()`, because every comma-separated token is read as a column name. Generated urls with `sort=publishDate,desc` need no change: `YadaPageRequest` joins the direction that Spring split off back to its column, and urls that already encode the separator as `%252C` keep working.
  `appendSort` / `prependSort` now accumulate instead of replacing, and `prependSort` really prepends: re-check code that called them more than once. `getOutOfRows()` throws unless the `YadaPageRows` was built with an explicit total.
- [ ] **Step 4: Error text.** Error pages and ajax error modals now show only the HTTP reason phrase; `errorDescription`, `yadaExceptionMessage` and `yadaHttpMessage` no longer contain exception messages, and the error page keeps the original status code instead of always 500. Remove template logic that depended on either.
- [ ] **Step 5: Embedded Tomcat.** `YadaTomcatServer.configure(String, String, boolean, YadaConfiguration)` became `configure(String, String, boolean, boolean, YadaConfiguration)` with the new `eclipse` flag: adapt any subclass. The command line changed from `<acroenv> <webappFolder> [<baseDir> [dev]]` to `<acroenv> <webappFolder> [<baseDir>] [--dev] [--eclipse]`: the old fourth argument `dev` is now read as the base dir, replacing the real one with a relative `dev` folder, and startup normally fails with "The baseDir dev must exist and be writable". Find the server main class in Eclipse `.launch` files, Gradle `run` / `JavaExec` tasks, shell and batch scripts and service units, and replace `dev` with `--eclipse` when launched from Eclipse (the old `dev` behaviour: HTTPS connector plus the `bin/main` classes) or with `--dev` elsewhere (HTTPS connector only). Two-argument launches still start in Eclipse developer mode, and a base dir without options still starts in non-developer mode. Extension points that are now available instead of copying code: `YadaAppConfig.getFlywayLocations()`, `createEmailTemplateResolver()`, `configureEmailTemplateResolver()`.
- [ ] **Step 6:** Framework relations `YadaAutoLoginToken.yadaUserCredentials`, `YadaRegistrationRequest.yadaUserCredentials` and `.trattamentoDati` are `@ManyToOne` (were `@OneToOne`). Apply the same review to application entities that are many-to-one in real data before regenerating the schema (Task 11).
- [ ] **Step 7: Compile gate.** The compile errors left from Task 1 Step 3 come from removed DataTables and token APIs: clear them now by applying Task 6 Steps 1-2 and Task 7 Step 1, plus the override signatures of Task 7 Step 3, then repeat until `compileJava` and `compileTestJava` succeed. Every later check that needs compiled classes or a running application assumes this gate. Until the migration of Task 11 the database keeps the old token columns, so registration, password recovery, autologin and email change are exercised only after Task 11.

### Task 5: CSRF protection

`YadaSecurityConfig.configure()` no longer disables CSRF. Spring Security now rejects with 403 every POST/PUT/PATCH/DELETE without a valid token, and `/logout` only answers to POST.

- [ ] **Step 1: Meta tags.** In the shared `<head>` fragment of every layout, add the block shown in `YadaExamples/src/main/webapp/WEB-INF/views/header.html`: `_csrf`, `_csrf_header` and `_csrf_parameter` meta tags inside `<th:block th:if="${_csrf != null}">`. `yada.ajax.js` reads them at script load and adds the header to every same-origin jQuery ajax request with an unsafe method. **Verify:** view the source of one page per layout (public, back office, standalone pages such as login or error).
- [ ] **Step 2: Forms.** Find `<form`. A `method="post"` form with `th:action` gets the hidden token automatically. Fix: forms with a static `action` attribute pointing to the application (use `th:action`, or add the hidden input `th:name="${_csrf.parameterName}" th:value="${_csrf.token}"`); forms posting to an external site need nothing.
- [ ] **Step 3: Multipart forms** (SEC §7). The token is no longer appended to the action url. Use `th:action="@{/…}"` or `yada:actionUpload="@{/…}"` with an absolute local path. Keep `MultipartFilter` before the Spring Security filter. A plain (non-ajax) upload above `maxFileUploadSizeBytes` now gets HTTP 413 before the controller: add a 413 `<error-page>` to `web.xml` or upload through ajax.
- [ ] **Step 4: Writes sent with GET.** Find every controller method that inserts, updates, deletes, sends mail or changes session/security state and is mapped with `@RequestMapping` without a method or with `@GetMapping`. Change it to `@PostMapping` and change each caller:

| Caller | Change |
|---|---|
| ajax link or button (`yada:ajax`, class `yadaAjax`) | add `yada:method="POST"` |
| ajax form without `method` | add `method="post"` |
| `yada.dataTableCrud` (`yada.datatablesLegacy.js`) delete definition and extra buttons | add `"method": "POST"` |
| Java-configured table button (`yada.datatables.js`) | add `.dtMethod("POST")` |
| plain link that navigates | a POST form with `th:action`, or `yada.postNavigate(url, data)` |
| `yada.ajax(url, data, handler)` from script | pass `"POST"` as the method argument |
| own `$.ajax` / `fetch` / `XMLHttpRequest` | jQuery calls get the header automatically; for others send the header from `yada.csrf.header` / `yada.csrf.token` |

  Form *display* endpoints stay GET; only the submit is POST. Some callers do not show their method where they are declared, for example child forms that submit a parent form through `data-yadaParentForm` (as in the image sorter): observe the method actually sent in the browser network panel instead of assuming it, and add `method="post"` where a request still goes out as GET.
- [ ] **Step 5: Logout.** Replace every `<a th:href="@{/logout}">` with a POST form (`th:action="@{/logout}" method="post"` with a submit button styled like the old link; the styling goes in the application stylesheet sources, regenerated in Task 12). **Verify:** clicking it ends the session.
- [ ] **Step 6: Redirect after a write.** A command that wrote and returned `"redirect:/somewhere"` is now an ajax POST, so the controller returns the navigation in the response:

| Purpose | Controller returns |
|---|---|
| navigate without a message | `model.addAttribute(YadaViews.AJAX_REDIRECT_URL_RELATIVE, "/somewhere"); return YadaViews.AJAX_REDIRECT;` |
| show a message, then navigate | `return yadaNotify.title(...).ok().message(...).redirectOnClose("/somewhere").add();` |

  `RedirectAttributes` no longer helps there: without a real redirect the flash attributes are not saved, while `redirectOnClose` keeps both message and destination. Normal (non-ajax) POST forms can keep returning `yadaWebUtil.redirectString(...)`.
- [ ] **Step 7: Token rotation.** The token is renewed at login and after a password change. Check every flow that logs in without a full page load (ajax login modal, session-timeout modal): the success handler must reload or navigate, otherwise the page keeps the old token. If the application sets its own `CsrfTokenRepository`, expose it as a bean.
- [ ] **Step 8: Verify** with the browser network panel: an ajax POST carries the CSRF header; a GET to a converted endpoint gets 405. For the negative check use a logged-in session with the required role and a throwaway record: the write sent with the session cookie but without the token (neither header nor form field) gets 403 and leaves the record unchanged, then the same request with the token from the meta tags succeeds. An anonymous request is not enough on a protected url, because a login redirect from the role check looks the same whether CSRF is active or not.

### Task 6: DataTables query policy (SEC §6)

- [ ] **Step 1: Find** `getConvertedJsonPage(`, `getJsonPage(`, `addExtraJsonAttribute(` for custom endpoints, and `yadaDataTableFactory.getSingleton(` / `dtEntityClass(` for tables configured in Java and served by the default `/yadaDataTableData` endpoint.
- [ ] **Step 2: Change custom endpoints.** Each custom endpoint passes a `YadaDataTableQueryPolicy` as fourth argument, built once with `YadaDataTableQueryPolicy.forEntity(Entity.class).column(path)…build()`. Rules: the declared path must equal the `data`/`name` that the browser sends, character for character; a localized map column is one path per language (`title.en`, `title.it`), so declare one column for each configured locale; getters and computed values are `column(path, false, false)`; values the script needs without a column are `extraOutput(path)`; a table showing subclass attributes builds the policy for the subclass; credential-like names and whole entities are refused. `getJsonPage` is removed: use `getConvertedJsonPage`. This applies to the endpoints of legacy `yada.dataTableCrud` pages too: `getConvertedJsonPage` has no variant without a policy, so keeping the legacy table does not avoid it.
  Paths are validated against the *declared* Java type of each field, segment by segment. An attribute that exists only in a subclass and is reached through an association declared with the superclass type (for example `product.productName` where `product` is declared `YadaProduct` and only the subclass `Product` has `productName`) makes `build()` fail, and declaring it `column(path, false, false)` does not help unless the declared type has a matching getter. When the application owns the entity, declare the association with the subclass type; with single-table inheritance this does not change the foreign key (confirm with the schema diff of Task 11 Step 1) and only needs the data to contain no row of another type. Otherwise the column can only be output-only (no search, no sort). A policy built in a Spring bean constructor that fails this way prevents the application from starting.
- [ ] **Step 3: Review Java-configured tables.** They compile unchanged, but their policy is derived from the column definitions: a column whose path is not a persistent field of the `dtEntityClass()` class (a getter, a computed value, an attribute that exists only in a subclass) becomes output-only. The browser still offers sorting on it unless told otherwise, and a sort request on it, whether from a header click or from the initial order of the table, returns `yadaError` with no rows; a global search silently skips it. For each such column, set the subclass as `dtEntityClass()` when the attribute lives in a subclass; otherwise add `dtOrderableOff()` (and `dtSearchableOff()`), and move any initial order (`dtOrderAsc()` / `dtOrderDesc()` on the column, `dtOrder(...)` on the options) to a persistent column. A path to a credential, a whole entity or an entity collection fails when the policy is first built, at the first data request of the table.
- [ ] **Step 4: Test.** `build()` validates paths by reflection, with no database: add a unit test that builds every explicit policy of the application, so that a wrong path fails in the test and not as an empty table. Java-configured tables are checked in Step 5 through the log.
- [ ] **Step 5: Verify** each table in the browser: rows appear on the first load (initial order included), global search works (also with columns of converted types such as money, which pass validation but are searched at runtime: if the search fails, declare the column not searchable), each column that offers sorting sorts, and the log shows no `is not a persistent field … output only` warning for a column that still offers sorting. Sorting on a `YadaPersistentEnum` column, which failed on MySQL in 0.7.8 with "ORDER BY clause is not in SELECT list … incompatible with DISTINCT", works in 0.7.9: check it too where a table has one.

### Task 7: Security tokens, registration, password reset, login (SEC §2-5)

- [ ] **Step 1: Find** `getToken(`, `setToken(`, `makeLink(`, `parseLink(`, `findByIdAndTokenOrderByTimestampDesc`, `yadaRegistrationRequestDao.save`, `yadaAutoLoginTokenDao.save`, `makeAutologinLink`. Replace as in SEC §2: issue with `issue(...)` and build the link from the returned instance, read with `findValid(parsed, type, class)`, consume with `consume(...)`. `parseLink` returns a `ParsedSecurityToken` or null. An autologin `action` must be a local path, encoded once.
- [ ] **Step 2: Registration** (SEC §3): the controller that binds the registration form declares an `@InitBinder` for its model attribute calling `YadaRegistrationController.setRegistrationAllowedFields(binder, <subclass fields>)`.
- [ ] **Step 3: Password reset** (SEC §4): bind `YadaPasswordResetForm` instead of `YadaRegistrationRequest` in controller and template; `/yadaPasswordResetPost` and `/passwordChangeAfterRequest` are POST-only. Overrides of `YadaLoginController.autologin(...)` or `YadaRegistrationController.passwordChangeAfterRequest(...)` must follow the new signatures.
- [ ] **Step 4: Login template** (SEC §5): remove `usernameNotFound` and `${password}`; show one generic "invalid email or password" message for `passwordError`; keep `userDisabled` and the lockout message. Remove Java references to `REQUESTATTR_USERNAMENOTFOUNDFLAG` and `REQUESTATTR_PASSWORD`.
- [ ] **Step 5: Autologin** links now open a confirmation page and log in on POST. Optionally translate `yada.autologin.confirm.title`, `.message`, `.button`, `.cancel`.

### Task 8: Security configuration, headers, redirects, impersonation (SEC §8-11)

- [ ] **Step 1:** A custom `accessDeniedHandler` is wrapped in `YadaUploadLimitAccessDeniedHandler`; a custom authentication failure handler treats `LockedException` like `TooManyFailedAttemptsException`.
- [ ] **Step 2: Headers.** Default Spring Security headers are back, plus `Referrer-Policy: same-origin`. Find `<iframe`, `X-Frame-Options`, `Cache-Control`, `Strict-Transport-Security` in templates, filters and server configuration. Pages framed by the application itself need `frameOptions(sameOrigin)` after `super.configure(http)`; headers that the application or its proxy already added must not be sent twice; decide explicitly whether public dynamic pages may be cached, since they now answer no-cache. Static resources served by the `YadaWebConfig` handlers keep their `max-age`, except in the development environment, where `YadaWebConfig.setCachePeriod()` sends `Cache-Control: no-store` by design: check `max-age` on a test or production environment.
- [ ] **Step 3: Redirect targets.** Any redirect to a location supplied by the client goes through `"redirect:" + yadaWebUtil.safeLocalRedirect(target)`, never `redirectString`. `setDefaultTargetUrlAjaxRequest("/yadaLoginSuccess?targetUrl=…")` must use a local path.
- [ ] **Step 4: Impersonation and user administration** copied from the example (SEC §9-10): POST-only mappings, `yada.impersonate(url, id, "POST")`, deimpersonation banner as a POST form, `contextRelativeLocation` helper, field allowlist on the user edit binder.
- [ ] **Step 5:** A copied `ajaxRedirect.html` escapes its values with `YadaWebUtil.escapeJsonString` (SEC §11).

### Task 9: Client address and logging (SEC §12)

- [ ] **Step 1:** `getClientAddress()` / `getClientIp()` return the socket address unless the peer is trusted. Behind an HTTP reverse proxy configure Tomcat `RemoteIpValve` (preferred) or `<security><trustedProxies>`. Behind Apache with `mod_jk` (AJP) neither is needed: AJP passes the client address and the secure flag of the original request to Tomcat, so verify after deploy that the audit log shows the real client address and that HTTPS pages carry `Strict-Transport-Security`, unless another proxy sits in front of Apache; add `%X{forwardedFor}` to log patterns if the raw header is wanted.
- [ ] **Step 2:** Add `<logger name="org.springframework.web.servlet.PageNotFound" level="ERROR"/>` to every production `logback.xml`. Mask the last path segment of `/autologin/`, `/registrationConfirmation/`, `/passwordReset/` and `/changeEmailConfirm/` in container and proxy access logs.
- [ ] **Step 3:** Token secrets are hidden in the application log, so links can no longer be copied from it during development: use Mailpit (Task 10).

### Task 10: Optional improvements

- [ ] **Step 1: Mailpit.** Add beside `<smtpserver>` the section `<smtpserver-mailpit enabled="${usemailpit}">` with `host` `localhost`, `port` `1025`, `protocol` `smtp`. It is used only in the development environment when `usemailpit=true` is an environment variable or system property of the application JVM. Mailpit is a standalone executable that must be downloaded and running on the development machine.
- [ ] **Step 2:** Optional settings: `security/registration/expirationHours` (24), `security/passwordReset/expirationMinutes` (30), `security/emailChange/expirationMinutes` (30), `<database><dialect>` for MariaDB reached through MySQL Connector/J.

### Task 11: Database migration (SEC §1)

Applies to every application whose `persistence.xml` lists `YadaRegistrationRequest` or `YadaAutoLoginToken`, even if it never uses them.

- [ ] **Step 1:** Regenerate the schema from the entities with the application schema generation task, which also confirms that it runs with Validator 9 / Expressly 6 (Task 2 Step 4), and diff it against the previous one. Expected differences: `token` replaced by `tokenHash varchar(64) not null` in both tables, `expiration` added to `YadaRegistrationRequest`, unique constraints on `yadaUserCredentials_id` / `trattamentoDati_id` replaced by plain indexes. The generated schema is not the database: those unique constraints appear only in schemas generated by Hibernate 6.2 or later, so a database built by Flyway migrations written from an older schema never had them. Any other difference comes from the Hibernate 7.1 → 7.4 change or from application entities and must be understood before it is migrated.
- [ ] **Step 2: Check the live databases first.** On every environment (development, test, production; read-only), list the indexes of the two tables, for example from `information_schema.STATISTICS` or with `SHOW INDEX FROM YadaAutoLoginToken; SHOW INDEX FROM YadaRegistrationRequest;`. MySQL already created an index named after each foreign key constraint when the foreign key was added without a unique index, so on such a database the example's `ADD INDEX FK…` fails with a duplicate key name, after its `DELETE` statements have already run (MySQL can't roll back DDL).
- [ ] **Step 3:** Write `V<next>__hashed_security_tokens.sql` following `YadaExamples/src/main/resources/database/V002__hashed_security_tokens.sql`: delete the rows of JOINED subclasses first, then of the two tables, then change the columns. Index statements depend on Step 2: if no environment has the unique constraints, write no index statement at all (smallest database impact); if every environment has them, add the plain indexes before dropping the unique ones, with the names found; if the environments differ, make each index statement conditional on `information_schema` (a prepared statement, or a Spring `JavaMigration` bean, which `YadaAppConfig` registers with Flyway).
- [ ] **Step 4: Verify** on a copy of the production schema, and on a copy of each other index layout found in Step 2: Flyway applies the migration and Hibernate starts without schema errors.
- [ ] **Step 5: Rollout notes** for the release: back up the token tables, stop every old node before migrating, all outstanding autologin / registration / password recovery / email change links stop working.

### Task 12: Rebuild generated assets

- [ ] **Step 1:** Recompile Sass that imports framework stylesheets, and the application stylesheets changed in Task 5 (logout button), on a platform where `sassCompile` works (see Task 2 Step 6), and regenerate any minified bundle that includes `yada*.js`; a bundle built from 0.7.8 scripts has no CSRF support. Commit regenerated files only where the project tracks them.

### Task 13: Final verification

- [ ] **Step 1:** Compile and run the application's automated tests.
- [ ] **Step 2:** Start the application and check the startup log: no Flyway, Hibernate schema, Thymeleaf or policy errors.
- [ ] **Step 3:** Smoke test as an anonymous user and as each role: login (wrong password, unknown user, correct), logout, one ajax form, one upload, each table (list, search, sort, delete), one page per modal kind and one form per fragment kind (Task 3), one notification modal, one confirm modal, registration and password recovery if present, one paginated list with a sort parameter.
- [ ] **Step 4:** Inspect the response headers of one dynamic page and one static resource, and confirm the choices made in Task 8.
- [ ] **Step 5:** Update the application knowledge base with the durable facts (CSRF conventions, table policies, view-name resolution) and commit.
