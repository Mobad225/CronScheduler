package com.cronscheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Schedules jobs execution periodically.
 *
 * <p>Since the scheduled jobs run in concurrently on different threads, each instance of {@link Scheduler} is limited
 * to only 128 concurrent jobs.
 */
public final class Scheduler {
    final static int MAX_CONCURRENT_THREADS = 128;
    private final Map<UUID, ThreadAndRunnableJob> scheduledJobs = new HashMap<>();

    /**
     * Schedules a job to run periodically.
     *
     * @param jobId unique identifier for the {@link RunnableJob}.
     * @param job runnable job.
     * @param intervalMillis single run expected interval in millis.
     * @param frequencyMillis scheduling frequency.
     *
     * @throws IllegalStateException if the frequencyMillis is less than intervalMillis (i.e. when the job is expected
     * run more than once at the same instant).
     * @throws IllegalStateException if the number of scheduled jobs (threads) is more than the threshold (128).
     * @throws IllegalArgumentException if the job identifier is already used (e.g. job is already running or repeated
     * identifier).
     */
    public void scheduleJob(UUID jobId, Runnable job, long intervalMillis, long frequencyMillis)
            throws IllegalStateException, IllegalArgumentException {
        // Check if job is required to run more than once at any instant.
        if (frequencyMillis < intervalMillis) {
            throw new IllegalStateException("frequencyMillis cannot be less than intervalMillis");
        }
        // Check if the job is already scheduled.
        if (scheduledJobs.containsKey(jobId)) {
            throw new IllegalArgumentException(String.format("Job with id %s is already scheduled", jobId));
        }
        // Check the number of concurrent jobs will not exceed 128.
        if (scheduledJobs.size() == MAX_CONCURRENT_THREADS) {
            throw new IllegalStateException("Cannot schedule more than 128 concurrent jobs in one instance");
        }

        // Create new instance of RunnableJob and start it.
        RunnableJob runnableJob = new RunnableJob(jobId, job, intervalMillis, frequencyMillis);
        Thread thread = new Thread(runnableJob);
        thread.start();
        scheduledJobs.put(jobId, new ThreadAndRunnableJob(thread, runnableJob));
    }

    public void stopJob(UUID jobId) throws NoSuchElementException {
        // Check if the job is scheduled
        if (!scheduledJobs.containsKey(jobId)) {
            throw new NoSuchElementException(String.format("Job with id %s is not scheduled.", jobId));
        }

        // Cancel the job and remove it from the scheduledJobs map
        ThreadAndRunnableJob threadAndRunnableJob = scheduledJobs.get(jobId);
        threadAndRunnableJob.thread().interrupt();
        scheduledJobs.remove(jobId);
    }
}

/**
 * Wrapper around {@link Thread} and {@link RunnableJob}.
 */
record ThreadAndRunnableJob(Thread thread, RunnableJob runnableJob) {}
