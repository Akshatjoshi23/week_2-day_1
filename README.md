# Java Internship Practice: Week 2, Day 1

## Learning Objectives

This practice covers thread lifecycle states, the Java Memory Model (JMM), race conditions, synchronization, `volatile`, and happens-before. The examples are small standalone programs that can be compiled with a JDK and run from this folder.

## Thread Lifecycle

`Thread.getState()` reports a thread's state at the instant it is called. It is useful for observation, but it is not a way to control a thread.

- **NEW**: The thread has been created but `start()` has not been called.
- **RUNNABLE**: The thread is running or ready to run. Java does not report a separate state for waiting to receive CPU time.
- **BLOCKED**: The thread is waiting to acquire a monitor lock held by another thread.
- **WAITING**: The thread is waiting indefinitely for another thread's action, such as a latch being released.
- **TIMED_WAITING**: The thread is waiting for a limited time, for example during `Thread.sleep`.
- **TERMINATED**: The thread has finished executing.

`ThreadLifecycleDemo` coordinates each observation with a latch, a held monitor, or `join()`. State checks have time limits so an unexpected result fails instead of waiting forever.

## Java Memory Model

The JMM defines how threads interact through memory: when one thread's writes become visible to another, and which instruction reorderings are allowed. Without a defined communication mechanism, one thread should not assume that another thread will promptly see its updates.

`JavaMemoryModelDemo` uses a `CountDownLatch`. The writer stores a message before calling `countDown()`. The reader returns from `await()` before reading the message. The latch establishes a happens-before relationship, making that write visible to the reader.

## Race Conditions and `count++`

A race condition occurs when the result depends on the timing or interleaving of operations on shared mutable state. `RaceConditionDemo` starts two threads that increment the same unsynchronized counter and joins both before it prints the result.

`count++` is not one indivisible operation. It reads the current value, adds one, then writes the result. Two threads can read the same value and both write the same incremented value, losing one update. A mismatch is possible, but scheduling and runtime behavior mean it is not guaranteed on every run. The program prints the real result; it does not force a mismatch.

## Synchronization

`SynchronizationSolution` makes the counter's increment method `synchronized`. Only one thread at a time can execute that method on the same counter object. This mutual exclusion prevents overlapping read-modify-write operations. Exiting a synchronized method also makes its writes visible to a later thread that acquires the same monitor. The demo checks that the final count equals the expected value and fails otherwise.

## `volatile` Versus `synchronized`

A volatile read sees the most recently published volatile write under the JMM. In `VolatileDemo`, the worker repeatedly checks a volatile stop flag, and the main thread sets it to stop the worker.

`volatile` provides visibility and ordering guarantees for that variable; it does not make a compound operation such as `count++` atomic and does not provide mutual exclusion. Use synchronization (or an atomic counter) when multiple threads must safely update a shared count.

## Happens-Before

Happens-before is a JMM ordering and visibility guarantee. If action A happens-before action B, B can observe the effects of A. It does not mean A and B must run at a particular speed. Examples include:

- An unlock of a monitor happens-before a later lock of that same monitor.
- A `Thread.start()` call happens-before actions in the started thread.
- All actions in a thread happen-before another thread successfully returns from joining it.
- A `CountDownLatch.countDown()` happens-before a successful return from a corresponding `await()`.
- A write to a volatile variable happens-before a subsequent read of that variable that observes it.

## Compile and Run

Install a JDK and open a terminal in this folder. To compile all five programs together:

```powershell
javac ThreadLifecycleDemo.java RaceConditionDemo.java SynchronizationSolution.java VolatileDemo.java JavaMemoryModelDemo.java
```

Run each program:

```powershell
java ThreadLifecycleDemo
java RaceConditionDemo
java SynchronizationSolution
java VolatileDemo
java JavaMemoryModelDemo
```

You can also compile one source at a time with `javac ClassName.java`, then run it with `java ClassName`. The examples use only the Java standard library and require Java 9 or newer (`Thread.onSpinWait()` is used in two demos).

## Illustrative Output

The following is an output shape, not a transcript captured from every run. In particular, the race condition's actual count varies and can equal the expected count.

```text
NEW: NEW
RUNNABLE: RUNNABLE
BLOCKED: BLOCKED
WAITING: WAITING
TIMED_WAITING: TIMED_WAITING
TERMINATED: TERMINATED

Expected count: 2000000
Actual count:   <value printed by this run>
A mismatch is possible, but not guaranteed on every run.

Expected count: 2000000
Actual count:   2000000
Result:          correct

Worker observed stopRequested: true
Main thread set stopRequested: true
Worker terminated: true

Reader received: Message written before countDown
Both threads completed.
```

## Key Learnings and Conclusion

- Thread states can be observed reliably when the demo coordinates the conditions that produce them.
- Unsynchronized compound updates can lose work, but a race is not guaranteed to show a mismatch on every execution.
- Synchronization protects a critical section and provides memory-visibility guarantees.
- `volatile` is useful for simple visibility and signaling, not as a replacement for atomic updates or mutual exclusion.
- A happens-before relationship gives a defined path for safe communication between threads.

Use the evidence checklist in [evidence/README.md](evidence/README.md) to record real program output and the repository page after upload. No screenshots are included until they are captured.
