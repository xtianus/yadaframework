# `net.yadaframework.security.persistence.repository.YadaUserMessageDao`

| Method | Description |
|---|---|
| `getLastDate` | Returns the most recent date of the message stack - which is the initial date if the message is not stackable |
| `markAsRead` | Marks the given messages as read. |
| `find` | Finds matching data. |
| `hasUnreadMessage` | Returns true if there exists at least one unread message for the user |
| `deleteBelongingTo` | Delete all messages that do not involve users other than the one specified (no other users as sender o recipient) |
| `createOrIncrement` | Save a message. |
