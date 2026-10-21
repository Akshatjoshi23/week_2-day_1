import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JavaMemoryModelDemo {
    public static void main(String[] args) throws InterruptedException {
        MessageBox box = new MessageBox();
        CountDownLatch published = new CountDownLatch(1);
        CountDownLatch readerFinished = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            box.message = "Message written before countDown";
            published.countDown();
        }, "writer");
        Thread reader = new Thread(() -> {
            try {
                if (!published.await(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for the published message");
                }
                System.out.println("Reader received: " + box.message);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                readerFinished.countDown();
            }
        }, "reader");

        writer.setDaemon(true);
        reader.setDaemon(true);
        reader.start();
        writer.start();
        writer.join(TimeUnit.SECONDS.toMillis(3));
        if (writer.isAlive()) {
            writer.interrupt();
            throw new IllegalStateException("Writer did not finish in time");
        }
        if (!readerFinished.await(3, TimeUnit.SECONDS)) {
            reader.interrupt();
            throw new IllegalStateException("Reader did not finish in time");
        }
        reader.join(TimeUnit.SECONDS.toMillis(1));
        if (reader.isAlive()) {
            reader.interrupt();
            throw new IllegalStateException("Reader did not terminate");
        }
        System.out.println("Both threads completed.");
    }

    private static final class MessageBox {
        private String message;
    }
}