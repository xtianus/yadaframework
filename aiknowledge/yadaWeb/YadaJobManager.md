# `net.yadaframework.components.YadaJobManager`

| Method | Description |
|---|---|
| `startJob` | Activate the job so that it becomes available for the scheduler to start it. |
| `isJobGroupPaused` | Checks if the job group has been paused |
| `pauseJobGroup` | Pause all jobs in the given group |
| `resumeJobGroup` | Resume jobs of the given group |
| `deleteJob` | Removes the job from the database. |
| `countActiveOrRunningJobs` | Returns the number of jobs for the given group that are in the ACTIVE/RUNNING state |
| `getAllActiveOrRunningJobs` | Returns all jobs for the given group that are in the ACTIVE/RUNNING state, ordered by state and scheduled time. |
| `disableAndInterruptJobs` | Sets a list of jobs to DISABLED then interrupt the threads. |
| `completeJob` | Sets a job to COMPLETED |
| `toggleDisabledAndPaused` | Toggle between paused and disabled. |
| `disableAndInterruptJob` | Sets a job to DISABLED then interrupt its thread |
| `pauseAndInterruptJob` | Sets a job to PAUSED then interrupt its thread |
| `changeJobPriority` | Changes job priority. |
| `reschedule` | Reschedules a job to a new execution time. |
| `getJobInstance` | Returns an instance of a YadaJob. |
| `replaceWithCached` | Call this method after fetching a job instance from the database to replace it with a cached running instance if any. |
