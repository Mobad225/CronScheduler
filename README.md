# Cron Scheduler
### Brief description
A simple cron scheduler that accepts a runnable object (function) along with a unique identifier to it, a frequency for
scheduling it and the single-run expected interval. The scheduler will ensure that the jobs do not overrun and will
interrupt the execution of jobs that run for longer than specified.

The design is simple; there is the **Scheduler** which users should interact with, and there is **RunnableJob** 
which the **Scheduler** spawns for each job that is scheduled. The **Scheduler** will spawn the **RunnableJob**s in
separate threads in order to support concurrency. **RunnableJob** will take care of everything regarding a simple
recurring job (i.e. scheduling of the job and logging) while **Scheduler** will ensure that all scheduled jobs can
run concurrently and controls their spawning/killing.

### Trade-offs made
- **Scheduler** supports scheduling up to 128 jobs concurrently, and not more. This is done to prevent creating too
many threads and running out of memory.
- **RunnableJob** can only re-run the job when the previous run has completed (e.g. **RunnableJob** cannot schedule
a job that takes 1 hour to run every 30 minutes). This is done for simplicity of design; currently each 
**RunnableJob** runs on one thread, otherwise the design has to be changed to allow **RunnableJob** to spawn threads.

### Example usage
```
Scheduler scheduler = new Scheduler();
try {
    scheduler.scheduleJob(
            /* jobId= */ UUID.randomUUID(),
            /* job= */ doWork(),
            /* intervalMillis= */ 200,
            /* frequencyMillis= */ 500);
} catch (Exception e) {
    // Handle exception.
    e.printStackTrace();
}
```
A more comprehensive example that creates multiple jobs concurrently is shown in **Main.java**.

### Possible future improvements
- Allow **Scheduler** to pause jobs rather than just start or stop.
- Allow **RunnableJob** to schedule jobs before the last jobs are completed. (There still has to be a threshold)