import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SynchronizationSolution {
    private static final int INCREMENTS_PER_THREAD = 1_000_000;

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        CountDownLatch startTogether = new CountDownLatch(1);
        Runnable incrementTask = () -> {
            try {
                startTogether.await();
                for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                    counter.increment();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };

        Thread first = new Thread(incrementTask, "counter-1");
        Thread second = new Thread(incrementTask, "counter-2");
        first.setDaemon(true);
        second.setDaemon(true);
        first.start();
        second.start();
        startTogether.countDown();
        joinAndCheck(first);
        joinAndCheck(second);

        long expected = 2L * INCREMENTS_PER_THREAD;
        System.out.println("Expected count: " + expected);
        System.out.println("Actual count:   " + counter.count);
        if (counter.count != expected) {
            throw new IllegalStateException("Synchronized counter did not reach the expected value");
        }
        System.out.println("Result:          correct");
    }

    private static void joinAndCheck(Thread thread) throws InterruptedException {
        thread.join(TimeUnit.SECONDS.toMillis(5));
        if (thread.isAlive()) {
            thread.interrupt();
            throw new IllegalStateException(thread.getName() + " did not finish in time");
        }
    }

    private static final class Counter {
        private int count;

        private synchronized void increment() {
            count++;
        }
    }
}