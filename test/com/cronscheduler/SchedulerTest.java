package com.cronscheduler;

import static com.cronscheduler.CronSchedulerTestHelper.*;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.*;

public class SchedulerTest {
    private final static int THREAD_SLEEP_MILLIS = 3000;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private List<LogRecord> logRecords;
    private Logger runnableJobLogger;
    // Under test.
    private Scheduler scheduler;

    @BeforeEach
    public void setUp() {
        scheduler = new Scheduler();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        runnableJobLogger = Logger.getLogger(RunnableJob.class.getName());
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
        runnableJobLogger.addHandler(customHandler);
    }

    @AfterEach
    public void tearDown() {
        // Remove the custom handler after each test
        Handler[] handlers = runnableJobLogger.getHandlers();
        for (Handler handler : handlers) {
            runnableJobLogger.removeHandler(handler);
        }
    }


    @Test
    public void scheduleJob_invalidState_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> {
            scheduler.scheduleJob(UUID.randomUUID(), createTestJob(), 110, 50);
        });
    }

    @Test
    public void scheduleJobs_moreThanThreshold_throwsIllegalStateException() {
        for (int i = 0 ; i < Scheduler.MAX_CONCURRENT_THREADS ; i++) {
            scheduler.scheduleJob(UUID.randomUUID(), createTestJob(), 200, 500);
        }
        assertThrows(IllegalStateException.class,
                () -> {
                    scheduler.scheduleJob(UUID.randomUUID(), createTestJob(), 200, 500);
                });
    }

    @Test
    public void scheduleJob_repeatedId_throwsIllegalArgumentException() {
        UUID uuid = UUID.randomUUID();
        scheduler.scheduleJob(uuid, createTestJob(), 200, 500);
        assertThrows(IllegalArgumentException.class,
                () -> {
                    scheduler.scheduleJob(uuid, createTestJob(),200, 500);
                });
    }

    @Test
    public void stopJob_doesNotExist_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> scheduler.stopJob(UUID.randomUUID()));
    }

    @Test
    public void scheduleJob_stopJob_jobStops() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        scheduler.scheduleJob(uuid, createTestJob(), 200, 500);

        // Let the main thread sleep so that the job runs for some time.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        // Job started.
        assertTrue(outContent.toString().contains(jobStartedOut));
        // Job started logs.
        assertTrue(logContains(logRecords, jobScheduleStartLog));

        // Stop job.
        scheduler.stopJob(uuid);
        // Wait for thread interrupt to take place.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        int logRecordsLen = logRecords.size();
        String outContentString = outContent.toString();
        // Wait to ensure that the job stopped running.
        Thread.sleep(THREAD_SLEEP_MILLIS * 3);
        int newLogRecordsLen = logRecords.size();
        String newOutContentString = outContent.toString();

        // Assert job has stopped running by ensuring that its prints and logs are stopped.
        assertEquals(logRecordsLen, newLogRecordsLen);
        assertEquals(outContentString, newOutContentString);
    }

    @Test
    public void scheduleMultipleJobs_scheduledSuccessfully() throws InterruptedException {
        UUID job1Id = UUID.randomUUID();
        UUID job2Id = UUID.randomUUID();
        scheduler.scheduleJob(job1Id,
                CronSchedulerTestHelper.createTestJob(100,
                        /* jobStartedOut= */ "Job 1 started",
                        /* jobInterruptedOut= */ "Job 1 interrupted",
                        /* jobFinishedSuccessfullyOut= */ "Job 1 finished successfully"),
                200, 500);
        scheduler.scheduleJob(job2Id,
                CronSchedulerTestHelper.createTestJob(125,
                        /* jobStartedOut= */ "Job 2 started",
                        /* jobInterruptedOut= */ "Job 2 interrupted",
                        /* jobFinishedSuccessfullyOut= */ "Job 2 finished successfully"),
                250, 500);

        // Let the main thread sleep so that the jobs run for some time.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        // Jobs started.
        assertTrue(outContent.toString().contains("Job 1 started"));
        assertTrue(outContent.toString().contains("Job 2 started"));
        assertTrue(outContent.toString().contains("Job 1 finished successfully"));
        assertTrue(outContent.toString().contains("Job 2 finished successfully"));
        // Jobs started logs.
        assertTrue(logContains(logRecords, jobScheduleStartLog, job1Id));
        assertTrue(logContains(logRecords, jobScheduleStartLog, job2Id));
        // Jobs completed successfully.
        assertTrue(logContains(logRecords, getJobFinishedSuccessfullyLog, job1Id));
        assertTrue(logContains(logRecords, getJobFinishedSuccessfullyLog, job2Id));
    }

    @Test
    public void scheduleMultipleJobs_stopOneJob_otherJobsContinueSuccessfully() throws InterruptedException {
        UUID job1Id = UUID.randomUUID();
        UUID job2Id = UUID.randomUUID();
        scheduler.scheduleJob(job1Id,
                CronSchedulerTestHelper.createTestJob(100,
                        /* jobStartedOut= */ "Job 1 started",
                        /* jobInterruptedOut= */ "Job 1 interrupted",
                        /* jobFinishedSuccessfullyOut= */ "Job 1 finished successfully"),
                200, 500);
        scheduler.scheduleJob(job2Id,
                CronSchedulerTestHelper.createTestJob(125,
                        /* jobStartedOut= */ "Job 2 started",
                        /* jobInterruptedOut= */ "Job 2 interrupted",
                        /* jobFinishedSuccessfullyOut= */ "Job 2 finished successfully"),
                250, 500);

        // Let the main thread sleep so that the job runs for some time.
        Thread.sleep(THREAD_SLEEP_MILLIS);

        // Stop job.
        scheduler.stopJob(job1Id);
        // Wait for thread interrupt to take place.
        Thread.sleep(THREAD_SLEEP_MILLIS * 3);

        // Create local copy of logRecords because we will iterate on it while job 2 is still concurrently modifying it.
        List<LogRecord> logRecordsCopy = new ArrayList<>(logRecords);
        long job1LogRecordsLen = logsCountForJob(logRecordsCopy, job1Id);
        long job2LogRecordsLen = logsCountForJob(logRecordsCopy, job2Id);
        // Wait to ensure that the job stopped running.
        Thread.sleep(THREAD_SLEEP_MILLIS * 3);
        // Create local copy of logRecords because we will iterate on it while job 2 is still concurrently modifying it.
        logRecordsCopy.clear();
        logRecordsCopy = new ArrayList<>(logRecords);
        long newJob1LogRecordsLen = logsCountForJob(logRecordsCopy, job1Id);
        long newJob2LogRecordsLen = logsCountForJob(logRecordsCopy, job2Id);

        // Assert job 1 has stopped running, but job 2 hasn't.
        assertEquals(job1LogRecordsLen, newJob1LogRecordsLen);
        assertTrue(newJob2LogRecordsLen > job2LogRecordsLen);
    }

    static Runnable createTestJob() {
        return CronSchedulerTestHelper.createTestJob(/* timeToCompleteMillis= */ 100,
                jobStartedOut,
                jobInterruptedOut,
                jobFinishedSuccessfullyOut);
    }
}
