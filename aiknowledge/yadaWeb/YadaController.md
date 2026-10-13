# `net.yadaframework.web.YadaController`

| Method | Description |
|---|---|
| `yadaError` | Error page handler for servlet error dispatches (`/yadaError`). The detailed message and stacktrace are only logged. An uncommitted response keeps the original HTTP status and forwards to the configured error page; the page model (`yadaHttpStatus`, `yadaHttpMessage`) and the Ajax `errorDescription` carry only the standard HTTP reason phrase ("Server Error" for unknown statuses), never exception or servlet error text. A committed response is only flushed and no view is returned: nothing is written and it is not closed with a fake complete ending, so the client sees a truncated or aborted response. |
