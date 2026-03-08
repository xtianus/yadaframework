# `net.yadaframework.security.persistence.entity.YadaUserMessage`

| Method | Description |
|---|---|
| `getTimestampAsRelative` | Returns the timestamp formatted as a relative time from now, in the recipient's timezone |
| `computeHash` | Computes the message hash used to detect duplicates. |
| `incrementStack` | Increments the stack counter and stores the new timestamp. |
| `addAttachment` | Adds a new attachment to this message |
