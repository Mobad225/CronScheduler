package com.cronscheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static  com.cronscheduler.CronSchedulerTestHelper.getJobFinishedSuccessfullyLog;
import static  com.cronscheduler.CronSchedulerTestHelper.jobErrorLog;
import static  com.cronscheduler.CronSchedulerTestHelper.jobStackTraceLog;
import static  com.cronscheduler.CronSchedulerTestHelper.jobOverranLog;
import static  com.cronscheduler.CronSchedulerTestHelper.jobStartedOut;
import static  com.cronscheduler.CronSchedulerTestHelper.jobInterruptedOut;
import static  com.cronscheduler.CronSchedulerTestHelper.jobFinishedSuccessfullyOut;
import static  com.cronscheduler.CronSchedulerTestHelper.jobScheduleStartLog;
import static  com.cronscheduler.CronSchedulerTestHelper.logContains;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

public class RunnableJobTest {
    private final static int THREAD_SLEEP_MILLIS = 2000;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private Logger logger;
    private List<LogRecord> logRecords;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        logger = Logger.getLogger(RunnableJob.class.getName());
        logRecords = new ArrayList<>();
        Handler customHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };
        logger.addHandler(customHandler);
    }

    @AfterEach
    public void tearDown() {
        // Remove the custom handler after each test
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            logger.removeHandler(handler);
        }
    }

    @Test
    public void createRunnableJob_invalidState_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> new RunnableJob(UUID.randomUUID(),
                        createTestJob(/* timeToCompleteMillis= */ 100), 110, 50));
    }

    @Test
    public void testRunnableJob_jobThrows_jobHaltsAndLogs() throws InterruptedException {
        Thread jobThread = new Thread(
                new RunnableJob(UUID.randomUUID(),
                        () ->
                        {
                            System.out.println(jobStartedOut);
                            // Simulate some work.
                            try {
                                Thread.sleep(/* millis= */ 250);
                            } catch (InterruptedException e) {
                                return;
                            }
                            // Simulating a condition where an exception is thrown but not caught.
                            throw new RuntimeException("Unhandled exception in the Runnable.");
                        },
                        /* intervalMillis= */ 500,
                        /* frequencyMillis= */ 5000));

        jobThread.start();
        // Let the main thread sleep so that the job runs for some time.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        // Job started.
        assertTrue(outContent.toString().contains(jobStartedOut));
        // Job started logs.
        assertTrue(logContains(logRecords, jobScheduleStartLog));
        // Job failed and stack trace is logged.
        assertTrue(logContains(logRecords, jobErrorLog));
        assertTrue(logContains(logRecords, jobStackTraceLog));

        // Stop the job thread.
        jobThread.interrupt();
        jobThread.join();
    }

    @Test
    public void testRunnableJob_jobOverruns_jobHaltsAndLogs() throws InterruptedException {
        Thread jobThread = new Thread(
                new RunnableJob(UUID.randomUUID(),
                        createTestJob(/* timeToCompleteMillis= */ 250),
                        /* intervalMillis= */ 50,
                        /* frequencyMillis= */ 500));

        jobThread.start();
        // Let the main thread sleep so that the job runs for some time.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        // Job started, got interrupted and has never finished.
        assertTrue(outContent.toString().contains(jobStartedOut));
        assertTrue(outContent.toString().contains(jobInterruptedOut));
        assertFalse(outContent.toString().contains(jobFinishedSuccessfullyOut));
        // Job started logs.
        assertTrue(logContains(logRecords, jobScheduleStartLog));
        // Job overran and was stopped logs.
        assertTrue(logContains(logRecords, jobOverranLog));

        // Stop the job thread.
        jobThread.interrupt();
        jobThread.join();
    }

    @Test
    public void testValidRunnableJob_jobRunsSuccessfully() throws InterruptedException {
        int jobFrequencyMillis = 500;
        Thread jobThread = new Thread(
                new RunnableJob(UUID.randomUUID(),
                        createTestJob(/* timeToCompleteMillis= */ 100),
                        /* intervalMillis= */ 200,
                        /* frequencyMillis= */ jobFrequencyMillis));

        jobThread.start();
        // Let the main thread sleep so that the job runs for some time.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        // Job started.
        assertTrue(outContent.toString().contains(jobStartedOut));
        assertTrue(outContent.toString().contains(jobFinishedSuccessfullyOut));
        // Job started logs.
        assertTrue(logContains(logRecords, jobScheduleStartLog));
        // Job completed successfully.
        assertTrue(logContains(logRecords, getJobFinishedSuccessfullyLog));

        // Stop the job thread.
        jobThread.interrupt();
        jobThread.join();
    }

    static Runnable createTestJob(long timeToCompleteMillis) {
        return CronSchedulerTestHelper.createTestJob(timeToCompleteMillis,
                jobStartedOut,
                jobInterruptedOut,
                jobFinishedSuccessfullyOut);
    }
}
