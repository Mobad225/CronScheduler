import com.cronscheduler.Scheduler;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        for (int i = 1 ; i <= 3 ; i++) {
            try {
                scheduler.scheduleJob(UUID.randomUUID(),
                        createRunnable(/* logPrefix= */ String.format("[JOB%d]", i)),
                        /* intervalMillis= */ 200,
                        /* frequencyMillis= */ 500);
            } catch (Exception e) {
                // Handle exception.
                e.printStackTrace();
            }
        }
    }

    static Runnable createRunnable(String logPrefix) {
        return () -> {
            System.out.println(logPrefix + "Job started");
            // Simulate some work.
            try {
                Thread.sleep(/* millis= */ 100);
            } catch (InterruptedException e) {
                System.out.println(logPrefix +"Job interrupted");
                return;
            }
            System.out.println(logPrefix +"Job finished");
        };
    }
}