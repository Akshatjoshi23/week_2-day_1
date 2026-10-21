import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VolatileDemo {
    private static volatile boolean stopRequested;

    public static void main(String[] args) throws InterruptedException {
        stopRequested = false;
        CountDownLatch pollingStarted = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            pollingStarted.countDown();
            while (!stopRequested) {
                Thread.onSpinWait();
            }
            System.out.println("Worker observed stopRequested: " + stopRequested);
        }, "volatile-worker");
        worker.setDaemon(true);
        worker.start();

        if (!pollingStarted.await(2, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Worker did not start polling");
        }
        stopRequested = true;
        worker.join(TimeUnit.SECONDS.toMillis(2));
        if (worker.isAlive()) {
            stopRequested = true;
            worker.interrupt();
            throw new IllegalStateException("Volatile worker did not stop in time");
        }
        System.out.println("Main thread set stopRequested: " + stopRequested);
        System.out.println("Worker terminated: " + !worker.isAlive());
    }
}