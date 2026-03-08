# `net.yadaframework.persistence.repository.YadaJobDao`

| Method | Description |
|---|---|
| `delete` | Deletes a job |
| `getJobState` | Get the current job state from the database |
| `stateChangeFromTo` | Invoked by the scheduler classes when starting a job. |
| `getJobStartTime` | Returns the start time of a job |
| `isJobGroupPaused` | Checks if the job group has been paused. |
| `findByJobGroupAndState` | Returns all jobs for the given group that are in the given state |
| `findRunning` | Returns the running job for the given group if any |
| `countByJobGroupAndStates` | Returns the number of jobs for the given group, that are in one of the given states |
| `findByJobGroupAndStates` | Returns all jobs for the given group, that are in one of the given states |
| `save` | Saves the current data. |
| `findById` | Finds by ID. |
