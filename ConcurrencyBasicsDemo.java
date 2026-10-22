
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyBasicsDemo {

    // Shared variables used in the demonstrations.
    private static volatile boolean running = true;
    private static int result = 0;

    private static int unsafeCount = 0;
    private static int synchronizedCount = 0;
    private static final AtomicInteger atomicCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {

        System.out.println("==================================");
        System.out.println(" JAVA CONCURRENCY BASICS DEMO");
        System.out.println("==================================");

        demonstrateThreadLifecycle();

        demonstrateJavaMemoryModel();

        demonstrateRaceCondition();

        System.out.println("\nAll demonstrations completed.");
    }

    // --------------------------------------------------
    // PART 1: THREAD LIFECYCLE
    // --------------------------------------------------

    private static void demonstrateThreadLifecycle()
            throws InterruptedException {

        System.out.println("\n--- 1. THREAD LIFECYCLE ---");

        // The worker will wait until the main thread releases it.
        CountDownLatch releaseWorker = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            try {
                System.out.println(
                    "Worker started: "
                    + Thread.currentThread().getName()
                );

                releaseWorker.await();

                System.out.println("Worker is finishing.");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Lifecycle-Worker");

        // State before start().
        System.out.println("Before start(): " + worker.getState());

        // Start a separate thread of execution.
        worker.start();

        // Wait until the worker reaches WAITING, if possible.
        boolean waitingObserved = waitForState(
            worker, Thread.State.WAITING
        );

        System.out.println(
            "Waiting state observed: " + waitingObserved
        );
        System.out.println(
            "Current worker state: " + worker.getState()
        );

        // Release the worker so it can finish.
        releaseWorker.countDown();

        // Wait for the worker to terminate.
        worker.join();

        System.out.println(
            "After join(): " + worker.getState()
        );
    }

    private static boolean waitForState(
            Thread thread, Thread.State expected)
            throws InterruptedException {

        long deadline = System.nanoTime()
                + 2_000_000_000L;

        while (System.nanoTime() < deadline) {
            if (thread.getState() == expected) {
                return true;
            }

            if (thread.getState() == Thread.State.TERMINATED) {
                return false;
            }

            Thread.sleep(1);
        }

        return thread.getState() == expected;
    }

    // --------------------------------------------------
    // PART 2: JAVA MEMORY MODEL (JMM)
    // --------------------------------------------------

    private static void demonstrateJavaMemoryModel()
            throws InterruptedException {

        System.out.println("\n--- 2. JAVA MEMORY MODEL ---");

        // Example A: visibility through volatile.
        running = true;

        CountDownLatch workerStarted = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            workerStarted.countDown();

            // The volatile read allows this thread to
            // observe the main thread's update to running.
            while (running) {
                Thread.yield();
            }

            System.out.println(
                "Worker observed running = false"
            );
        }, "Visibility-Worker");

        worker.start();

        // Ensure the worker has begun its task.
        workerStarted.await();

        // Volatile write: the worker can observe this change.
        running = false;

        worker.join();

        // Example B: visibility through join().
        result = 0;

        Thread resultWorker = new Thread(() -> {
            result = 42;
            System.out.println("Worker wrote result = 42");
        }, "Result-Worker");

        resultWorker.start();

        // The worker's actions happen-before this thread
        // continues after a successful join().
        resultWorker.join();

        System.out.println(
            "Main thread reads result = " + result
        );
    }

    // --------------------------------------------------
    // PART 3: RACE CONDITIONS
    // --------------------------------------------------

    private static void demonstrateRaceCondition()
            throws InterruptedException {

        System.out.println("\n--- 3. RACE CONDITIONS ---");

        demonstrateLostUpdate();

        demonstrateSynchronizedCounter();

        demonstrateAtomicCounter();
    }

    // Example A: force a lost update in a controlled way.
    private static void demonstrateLostUpdate()
            throws InterruptedException {

        unsafeCount = 0;

        CountDownLatch bothRead = new CountDownLatch(2);
        CountDownLatch allowWrite = new CountDownLatch(1);

        Runnable unsafeIncrement = () -> {

            // Both threads read the old value first.
            int oldValue = unsafeCount;

            bothRead.countDown();

            try {
                // Do not allow either thread to write until
                // both have read the same old value.
                allowWrite.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Both threads write oldValue + 1.
            unsafeCount = oldValue + 1;
        };

        Thread t1 = new Thread(unsafeIncrement, "Unsafe-1");
        Thread t2 = new Thread(unsafeIncrement, "Unsafe-2");

        t1.start();
        t2.start();

        // Wait until both threads have read the old value.
        bothRead.await();

        // Now allow both threads to write.
        allowWrite.countDown();

        t1.join();
        t2.join();

        System.out.println("Expected unsafe count: 2");
        System.out.println("Actual unsafe count:   " + unsafeCount);
    }

    // Example B: synchronized protects the increment.
    private static synchronized void incrementSynchronized() {
        synchronizedCount++;
    }

    private static void demonstrateSynchronizedCounter()
            throws InterruptedException {

        synchronizedCount = 0;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                incrementSynchronized();
            }
        }, "Sync-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                incrementSynchronized();
            }
        }, "Sync-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println(
            "Expected synchronized count: 200000"
        );
        System.out.println(
            "Actual synchronized count:   " + synchronizedCount
        );
    }

    // Example C: AtomicInteger provides atomic increments.
    private static void demonstrateAtomicCounter()
            throws InterruptedException {

        atomicCount.set(0);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                atomicCount.incrementAndGet();
            }
        }, "Atomic-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                atomicCount.incrementAndGet();
            }
        }, "Atomic-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println(
            "Expected atomic count: 200000"
        );
        System.out.println(
            "Actual atomic count:   " + atomicCount.get()
        );
    }
}
