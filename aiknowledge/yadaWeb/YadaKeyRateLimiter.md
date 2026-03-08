# `net.yadaframework.components.YadaKeyRateLimiter`

| Method | Description |
|---|---|
| `validateRate` | Records one event for the key and returns whether it is still within the configured rate limit. |
| `getCurrentRate` | Returns how many events the key has accumulated in the current time window. |
