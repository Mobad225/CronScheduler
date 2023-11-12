package com.cronscheduler;

import java.util.List;
import java.util.UUID;
import java.util.logging.LogRecord;

class CronSchedulerTestHelper {
    static final String jobScheduleStartLog = "scheduling started:";
    static final String jobErrorLog = "error running job";
    static final String jobStackTraceLog = "exception stack trace:";
    static final String jobOverranLog = "stopped due to exceeding maximum execution time:";
    static final String getJobFinishedSuccessfullyLog = "finished running";
    static final String jobStartedOut = "Started job execution.";
    static final String jobInterruptedOut = "Job interrupted.";
    static final String jobFinishedSuccessfullyOut = "Finished job execution successfully.";

    /** Returns whether the logs contain logMessage parameter */
    static boolean logContains(List<LogRecord> logRecords, String logMessage) {
        return logRecords.stream()
                .map(LogRecord::getMessage)
                .anyMatch(message -> message.contains(logMessage));
    }

    static boolean logContains(List<LogRecord> logRecords, String logMessage, UUID jobUuid) {
        return logRecords.stream()
                .map(LogRecord::getMessage)
                .filter(message -> message.contains(jobUuid.toString()))
                .anyMatch(message -> message.contains(logMessage));
    }

    static long logsCountForJob(List<LogRecord> logRecords, UUID jobUuid) {
        return logRecords.stream()
                .map(LogRecord::getMessage)
                .filter(message -> message.contains(jobUuid.toString()))
                .count();
    }

    /**
     * Creates a simple job.
     *
     * @param timeToCompleteMillis time for the job to be completed.
     * */
    static Runnable createTestJob(long timeToCompleteMillis,
                                  String jobStartedOut,
                                  String jobInterruptedOut,
                                  String jobFinishedSuccessfullyOut) {
        return () -> {
            System.out.println(jobStartedOut);
            // Simulate some work.
            try {
                Thread.sleep(timeToCompleteMillis);
            } catch (InterruptedException e) {
                System.out.println(jobInterruptedOut);
                return;
            }
            System.out.println(jobFinishedSuccessfullyOut);
        };
    }
}
