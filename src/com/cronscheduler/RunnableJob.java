package com.cronscheduler;

import static java.lang.Thread.sleep;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/** Represents a job that should be scheduled. */
class RunnableJob implements Runnable {
    private static final Logger logger = Logger.getLogger(RunnableJob.class.getName());
    // Unique job identifier.
    private final UUID jobId;
    // Job to be scheduled.
    private final Runnable job;
    // A single run expected interval in milliseconds.
    // Used to stop overrunning jobs.
    private final long intervalMillis;
    // Scheduling frequency in milliseconds.
    // Jobs will cannot be scheduled mo WIP
    private final long frequencyMillis;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Creates a {@link RunnableJob} that runs periodically.
     *
     * @param jobId unique identifier for the {@link RunnableJob}.
     * @param job runnable job.
     * @param intervalMillis single run expected interval in millis.
     * @param frequencyMillis scheduling frequency.
     *
     * @throws IllegalStateException if the frequencyMillis is less than intervalMillis (i.e. when the RunnableJob is
     * expected have more than one job running at the same instant).
     */
    RunnableJob (UUID jobId, Runnable job, long intervalMillis, long frequencyMillis) throws IllegalStateException {
        if (frequencyMillis < intervalMillis) {
            throw new IllegalStateException("frequencyMillis cannot be less than intervalMillis");
        }

        this.jobId = jobId;
        this.job = job;
        this.intervalMillis = intervalMillis;
        this.frequencyMillis = frequencyMillis;
        configureLogger();
    }

    @Override
    public void run() {
        Date schedulingStartDateTime = new Date(System.currentTimeMillis());
        logger.info(prefixWithJobId(String.format("scheduling started: %tF %tT", schedulingStartDateTime,
                schedulingStartDateTime)));
        while (true) {
            long startTime = System.currentTimeMillis();
            Date startDateTime = new Date(startTime);
            logger.info(prefixWithJobId(String.format("started running at %tF %tT", startDateTime, startDateTime)));
            Future<?> future = executor.submit(job);
            try {
                future.get(/* timeout= */ intervalMillis, /* timeUnit= */ TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(/* mayInterruptIfRunning= */ true);
                logger.severe(
                        prefixWithJobId(String.format("stopped due to exceeding maximum execution time: %d millis",
                        intervalMillis)));
            } catch (Exception e) {
                logger.severe(prefixWithJobId("error running job"));
                String stackTraceMessage = Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));
                logger.severe(prefixWithJobId(String.format("exception stack trace: %s", stackTraceMessage)));
            }
            long jobExecutionTimeMillis = Math.max(0, System.currentTimeMillis() - startTime);
            long timeUntilNextExecutionMillis = frequencyMillis - jobExecutionTimeMillis;
            Date endDateTime = new Date();
            logger.info(prefixWithJobId(String.format("finished running at %tF %tT", endDateTime, endDateTime)));
            logger.info(prefixWithJobId(String.format("waiting for %d millis before next execution",
                    timeUntilNextExecutionMillis)));
            // Wait for the next execution.
            try {
                sleep(timeUntilNextExecutionMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    String prefixWithJobId(String logString) {
        return String.format("RunnableJob %s: %s", jobId, logString);
    }

    private static void configureLogger() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
    }
}
