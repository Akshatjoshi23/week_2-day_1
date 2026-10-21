import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadLifecycleDemo {
    private static final long TIMEOUT_SECONDS = 3;

    public static void main(String[] args) throws InterruptedException {
        Thread newThread = new Thread(() -> { });
        System.out.println("NEW: " + newThread.getState());

        AtomicBoolean releaseRunnable = new AtomicBoolean(false);
        CountDownLatch runnableStarted = new CountDownLatch(1);
        Thread runnableThread = new Thread(() -> {
            runnableStarted.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            while (!releaseRunnable.get() && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
        }, "runnable-demo");
        runnableThread.setDaemon(true);
        runnableThread.start();
        awaitLatch(runnableStarted, "RUNNABLE worker did not start");
        awaitState(runnableThread, Thread.State.RUNNABLE);
        System.out.println("RUNNABLE: " + runnableThread.getState());
        releaseRunnable.set(true);
        joinAndCheck(runnableThread);

        Object lock = new Object();
        CountDownLatch lockAttempted = new CountDownLatch(1);
        Thread blockedThread = new Thread(() -> {
            lockAttempted.countDown();
            synchronized (lock) {
                // Acquiring the monitor is the whole demonstration.
            }
        }, "blocked-demo");
        blockedThread.setDaemon(true);
        synchronized (lock) {
            blockedThread.start();
            awaitLatch(lockAttempted, "BLOCKED worker did not start");
            awaitState(blockedThread, Thread.State.BLOCKED);
            System.out.println("BLOCKED: " + blockedThread.getState());
        }
        joinAndCheck(blockedThread);

        CountDownLatch releaseWaiting = new CountDownLatch(1);
        Thread waitingThread = new Thread(() -> {
            try {
                releaseWaiting.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "waiting-demo");
        waitingThread.setDaemon(true);
        waitingThread.start();
        awaitState(waitingThread, Thread.State.WAITING);
        System.out.println("WAITING: " + waitingThread.getState());
        releaseWaiting.countDown();
        joinAndCheck(waitingThread);

        CountDownLatch timedSleepStarted = new CountDownLatch(1);
        Thread timedWaitingThread = new Thread(() -> {
            timedSleepStarted.countDown();
            try {
                Thread.sleep(800);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "timed-waiting-demo");
        timedWaitingThread.setDaemon(true);
        timedWaitingThread.start();
        awaitLatch(timedSleepStarted, "TIMED_WAITING worker did not start");
        awaitState(timedWaitingThread, Thread.State.TIMED_WAITING);
        System.out.println("TIMED_WAITING: " + timedWaitingThread.getState());
        joinAndCheck(timedWaitingThread);

        Thread terminatedThread = new Thread(() -> { }, "terminated-demo");
        terminatedThread.start();
        joinAndCheck(terminatedThread);
        System.out.println("TERMINATED: " + terminatedThread.getState());
    }

    private static void awaitLatch(CountDownLatch latch, String message) throws InterruptedException {
        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException(message);
        }
    }

    private static void awaitState(Thread thread, Thread.State expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (thread.getState() != expected && System.nanoTime() < deadline) {
            Thread.yield();
        }
        if (thread.getState() != expected) {
            throw new IllegalStateException("Expected " + expected + " but saw " + thread.getState());
        }
    }

    private static void joinAndCheck(Thread thread) throws InterruptedException {
        thread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        if (thread.isAlive()) {
            thread.interrupt();
            throw new IllegalStateException(thread.getName() + " did not finish in time");
        }
    }
}