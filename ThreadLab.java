import java.util.concurrent.atomic.AtomicInteger;

// Simple account class i made for testing sync
class MyBankAccount {
    private int balance;

    MyBankAccount(int startingBalance) {
        this.balance = startingBalance;
    }

    public synchronized void deposit(int amt) {
        balance = balance + amt;
    }

    public synchronized void withdraw(int amt) {
        if (balance >= amt) {
            balance = balance - amt;
        }
    }

    public synchronized int getBalance() {
        return balance;
    }
}

// to check race condition
class CounterTest {
    private int normalCount = 0;
    private int syncCount = 0;

    public void incrementNormal() {
        normalCount++; // not safe
    }

    public synchronized void incrementSync() {
        syncCount++; // safe one
    }

    public int getNormalCount() {
        return normalCount;
    }

    public int getSyncCount() {
        return syncCount;
    }
}

public class ThreadLab {

    private static volatile boolean keepRunning = true; // for visibility test

    public static void main(String[] args) throws InterruptedException {

        System.out.println("--- 1. Thread State Check ---");
        Thread t1 = new Thread(() -> {
            System.out.println("Worker thread started: " + Thread.currentThread().getName());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Worker got interrupted");
                return;
            }
            System.out.println("Worker finished its work");
        }, "Worker-1");

        System.out.println("State before start: " + t1.getState());
        t1.start();
        System.out.println("State after start: " + t1.getState());
        t1.join();
        System.out.println("State after completion: " + t1.getState());

        System.out.println("\n--- 2. Volatile Flag Test ---");
        Thread watcher = new Thread(() -> {
            while (keepRunning) {
                // waiting for flag to change
            }
            System.out.println("Watcher saw flag change, stopping");
        });
        watcher.start();
        Thread.sleep(200);
        keepRunning = false;
        watcher.join();
        System.out.println("Volatile test done");

        System.out.println("\n--- 3. Race Condition Demo ---");
        CounterTest myCounter = new CounterTest();

        Runnable job = () -> {
            for (int i = 0; i < 100000; i++) {
                myCounter.incrementNormal();
                myCounter.incrementSync();
            }
        };

        Thread th1 = new Thread(job, "Count-Thread-1");
        Thread th2 = new Thread(job, "Count-Thread-2");
        th1.start();
        th2.start();
        th1.join();
        th2.join();

        System.out.println("Unsafe should be 200000, got: " + myCounter.getNormalCount());
        System.out.println("Safe should be 200000, got: " + myCounter.getSyncCount());

        System.out.println("\n--- 4. Bank Account Sync Test ---");
        MyBankAccount acc = new MyBankAccount(1000);

        Thread dep = new Thread(() -> {
            for (int i = 0; i < 1000; i++) acc.deposit(10);
        });
        Thread wit = new Thread(() -> {
            for (int i = 0; i < 500; i++) acc.withdraw(10);
        });

        dep.start();
        wit.start();
        dep.join();
        wit.join();

        System.out.println("Final balance expected 6000, actual: " + acc.getBalance());

        System.out.println("\n--- 5. Atomic Integer Test ---");
        AtomicInteger atomicNum = new AtomicInteger(0);

        Runnable atomicJob = () -> {
            for (int i = 0; i < 100000; i++) atomicNum.incrementAndGet();
        };

        Thread at1 = new Thread(atomicJob);
        Thread at2 = new Thread(atomicJob);
        at1.start();
        at2.start();
        at1.join();
        at2.join();

        System.out.println("Atomic final value: " + atomicNum.get());
        System.out.println("\nDone with all tests");
    }
}