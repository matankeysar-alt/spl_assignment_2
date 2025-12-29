import scheduling.TiredThread;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class threadTest {

    @Test
    public void testBasicExecutionAndFatigue() throws InterruptedException {
        double fatigueFactor = 1.5;
        TiredThread worker = new TiredThread(1, fatigueFactor);
        worker.start();

        CountDownLatch latch = new CountDownLatch(1);
        long sleepTimeMs = 100;

        worker.newTask(() -> {
            try {
                Thread.sleep(sleepTimeMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);

        long timeUsed = worker.getTimeUsed();
        assertTrue(timeUsed >= TimeUnit.MILLISECONDS.toNanos(sleepTimeMs));

        double expectedFatigue = timeUsed * fatigueFactor;
        assertEquals(expectedFatigue, worker.getFatigue(), 0.0001);

        worker.shutdown();
    }

    @Test
    public void testIdleTimeCalculation() throws InterruptedException {
        TiredThread worker = new TiredThread(2, 1.0);
        worker.start();

        long waitTimeMs = 50;
        Thread.sleep(waitTimeMs);

        CountDownLatch latch = new CountDownLatch(1);
        worker.newTask(latch::countDown);
        latch.await();

        long idleTime = worker.getTimeIdle();
        assertTrue(idleTime >= TimeUnit.MILLISECONDS.toNanos(waitTimeMs - 10));

        worker.shutdown();
    }

    @Test
    public void testBusyStateLimit() throws InterruptedException {
        TiredThread worker = new TiredThread(3, 1.0);
        worker.start();
        CountDownLatch latch = new CountDownLatch(1);

        worker.newTask(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
            latch.countDown();
        });

        worker.newTask(() -> {});

        assertThrows(IllegalStateException.class, () -> worker.newTask(() -> {}));

        latch.await();
        worker.shutdown();
    }

    @Test
    public void testCompareToLogic() {
        TiredThread t1 = new TiredThread(10, 1.0);
        TiredThread t2 = new TiredThread(20, 2.0);

        assertEquals(0, t1.compareTo(t2));

        t1.shutdown();
        t2.shutdown();
    }

    @Test
    public void testShutdown() throws InterruptedException {
        TiredThread worker = new TiredThread(4, 1.0);
        worker.start();

        worker.shutdown();
        worker.join(1000);

        assertFalse(worker.isAlive());

        assertThrows(IllegalStateException.class, () -> worker.newTask(() -> {}));
    }

    @Test
    public void testTaskExceptionBehavior() throws InterruptedException {
        TiredThread worker = new TiredThread(6, 1.0);
        worker.start();

        worker.newTask(() -> {
            throw new RuntimeException("Oops! Calculation failed");
        });

        worker.join(500);

        assertFalse(worker.isAlive());
        
        if (worker.isAlive()) worker.shutdown();
    }

    @Test
    public void testZeroFatigueFactor() throws InterruptedException {
        TiredThread worker = new TiredThread(7, 0.0);
        worker.start();

        CountDownLatch latch = new CountDownLatch(1);
        worker.newTask(() -> {
            try { Thread.sleep(50); } catch (Exception e) {}
            latch.countDown();
        });

        latch.await();

        assertEquals(0.0, worker.getFatigue());

        worker.shutdown();
    }

    @Test
    public void testDoubleShutdown() {
        TiredThread worker = new TiredThread(8, 1.0);
        worker.start();

        worker.shutdown();
        
        assertDoesNotThrow(() -> worker.shutdown());
    }
}