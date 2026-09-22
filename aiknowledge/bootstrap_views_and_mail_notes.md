# Bootstrap-version view layout and development mail capture

## Where the Bootstrap version lives

The Bootstrap version of a Yada view is carried by the **folder** and never by the file name.

| Kind | Layout | Example |
|---|---|---|
| Modals | `net/yadaframework/views/yada/b3\|b4\|b5/` | `/yada/b3/modalConfirm`, `/yada/b5/modalNotify`, `/yada/b4/modalGeneric` |
| Form fragments | `net/yadaframework/views/yada/form/b3\|b4\|b5/` | `/yada/form/b3/text`, `/yada/form/b5/fileUpload` |
| CMS components | `net/yadaframework/views/yadacms/b3\|b4/` | `/yadacms/b3/imageSorter` |

Views that are not version-specific stay at the top level: `ajax*.html`, `dataTable.html`, `dataTableCrud.html`,
`empty.html`, `httpError.html`, `messagesProperties.html`. `feedbackMessages.html` is also unversioned because it
switches its dismiss button on `${@config.bootstrapVersion}` inside the template.

Resolve a versioned view name at runtime rather than hardcoding a folder:

- `YadaConfiguration.getBootstrapView(name)` → `/yada/b<N>/<name>`; the names are the `YadaViews` constants
  `CONFIRM`, `AJAX_NOTIFY` and `MODAL_GENERIC`, which hold a view *name*, not a path.
- `YadaConfiguration.getBootstrapFormView(name)` → `/yada/form/b<N>/<name>`; `YadaWebUtil.getFormFragment(name)`
  delegates to it and is the form used from html:
  `th:replace="~{${@yadaWebUtil.getFormFragment('text')} :: field(...)}"`.
- `YadaWebUtil.getModalConfirmViewName()` and `YadaConfiguration.getNotifyModalView()` return already-resolved names.
  `getNotifyModalView()` still honours `config/paths/notificationModalView` when set.

`getForB3B4B5` remains for css classes and similar values; it is not used for view names.

The Thymeleaf resolvable patterns `/yada/*` and `/yadacms/*` of `YadaWebConfig.yadaTemplateResolver()` match
sub-folders, because Thymeleaf pattern `*` is not path-segment bound.

## Development mail capture with Mailpit

`YadaConfiguration` selects the SMTP section through a single private method, so the six email getters
(`getEmailHost`, `getEmailPort`, `getEmailProtocol`, `getEmailUsername`, `getEmailPassword`, `getEmailProperties`)
always read from the same place. The application configuration may declare a second section beside `<smtpserver>`:

```xml
<smtpserver-mailpit enabled="${usemailpit}">
    <host>localhost</host>
    <port>1025</port>
    ...
</smtpserver-mailpit>
```

It is selected only when all of these hold: the environment is development, the section exists, and its `enabled`
attribute equals `true` ignoring case. In every other case `<smtpserver>` is used, so COL and PROD are unaffected by
the flag. The attribute is read as a String and compared, never with `getBoolean`, because Commons Configuration
leaves an unresolved `${usemailpit}` in place as literal text and `getBoolean` would throw on it. Selecting the
mailpit section logs one INFO line at startup.

`configuration.xml` lists `<system/>` and `<env/>`, so the variable can be an exported environment variable
(inherited by the application JVM) or a system property; a Gradle `-Dusemailpit=true` reaches the application only
when the `run` JavaExec task forwards it explicitly.

`YadaAppConfig.javaMailSender()` needs no change: it already reads the getters.

## Mail preview templates

`YadaWebConfig.mailPreviewTemplateResolver()` uses the prefix `"classpath:" + YadaConstants.EMAIL_TEMPLATES_PREFIX`.
A `classpath:` location resolves both from a deployed war and from a plain classpath, so pages that include an email
template (for example a newsletter preview including `~{/email/newsletter :: body}`) also work on the embedded Tomcat
and under Eclipse WTP, where `/WEB-INF/classes/` does not exist. `YadaAppConfig.emailTemplateResolver()` is a
`ClassLoaderTemplateResolver` and keeps its prefix without the `classpath:` scheme.

## Relations mapped as @OneToOne

An `@OneToOne` makes Hibernate generate a unique constraint on the foreign key column. Three framework relations
were declared `@OneToOne` while being many-to-one in real data, which only became visible when the schema was
regenerated with Hibernate 7 and the constraint could not be applied to an existing database:
`YadaAutoLoginToken.yadaUserCredentials`, `YadaRegistrationRequest.yadaUserCredentials` and
`YadaRegistrationRequest.trattamentoDati` are `@ManyToOne`. Check application entities the same way before
regenerating a schema.

## Pagination

`YadaPageRequest.appendSort` and `prependSort` accumulate sort parameters: before 0.7.9 each call created a new
`YadaPageSort` and discarded what had already been requested, and `prependSort` appended instead of prepending.

`appendSort("publishDate,desc")` does **not** mean "publishDate descending": `addSortParameters` reads every
comma-separated token as a property name. The direction is set with the fluent API, `.desc()` or `.asc()`. The
`property,direction` syntax belongs to the request parameter, which `YadaPageSort.add()` parses.

In a generated url the separator between property and direction must be written double-encoded (`%252C`): Spring
splits a `List<String>` request parameter on commas, so `sort=publishDate,desc` would arrive as two values and
`YadaPageSort` would reject the lone direction token. `%252C` arrives as the literal text `%2C`, which Spring does
not split and `add()` understands.

`getOutOfRows()` throws unless the `YadaPageRows` was built with an explicit total, so the count query is only
needed where a page actually displays a total.

## Exceptions and response status

`YadaGlobalExceptionHandler` rethrows an exception when it carries `@ResponseStatus`, so the framework
can set the status. Spring 6 replaced `@ResponseStatus` on its own web exceptions with the
`ErrorResponse` interface, which carries `getStatusCode()`: `NoHandlerFoundException` implements it and
has no annotation. Without also honouring `ErrorResponse` the exception fell through to the error-page
forward and every unmapped url answered **500 instead of 404** — same body, wrong status. The handler
therefore checks both.

## DataTables

`yada.dataTableCrud` lives in `yada.datatablesLegacy.js` and drives the unversioned `views/yada/dataTableCrud.html`
(fragment name `fragment`). The newer `yada.datatables.js` has a different API and does not define it, so an
application using the legacy API must load `yada.datatablesLegacy.js`.
