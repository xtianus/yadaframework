# `net.yadaframework.components.YadaEmailBuilder`

| Method | Description |
|---|---|
| `instance` | Creates a new email builder. |
| `from` | Sets from. |
| `to` | Sets to. |
| `replyTo` | Specify a "reply-to" field in the email. |
| `subjectParams` | Assigns values to subject parameters as {0, {1 etc. |
| `addModelAttribute` | Thymeleaf model attribute to use in the template. |
| `modelAttributes` | Thymeleaf model attributes to use in the template - can be null. |
| `addInlineResources` | Adds an inline image to be used with src="cid:somekey" |
| `inlineResources` | Key-value pairs of inline images to be used with "cid:somekey" as src. |
| `addAttachment` | Adds a file attachment. |
| `attachments` | Key-value pairs of filename-File to send as attachment. |
| `addTimestamp` | Adds a timestamp to the subject - defaults to false. |
| `batch` | Choose whether to send this email to all recipients as a batch of distinct emails |
| `send` | Send the email. |
