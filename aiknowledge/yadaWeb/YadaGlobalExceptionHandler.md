# `net.yadaframework.web.YadaGlobalExceptionHandler`

| Method | Description |
|---|---|
| `globalErrorHandler` | `@ControllerAdvice` for exceptions leaving a controller. Logs the exception, then renders `/yada/ajaxError` (Ajax) or forwards to the configured error page. `errorDescription` and `yadaExceptionMessage` hold the generic HTTP reason phrase, never `e.getMessage()` (`yadaExceptionObject` is still in the model for server-side templates). On a committed response it renders no view, writes nothing and returns null: the client sees a truncated or aborted response. |
