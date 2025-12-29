
import scheduling.TiredExecutor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class executorTest {

    @Test
    public void testBasicExecution() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        final AtomicInteger counter = new AtomicInteger(0);
        
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(counter::incrementAndGet);
        
        executor.submitAll(tasks);
        
        assertEquals(1, counter.get(), "Single task executed successfully");
        
        executor.shutdown();
    }

    @Test
    public void testSubmitAllAndWait() throws InterruptedException {
        int numThreads = 4;
        int numTasks = 20;
        TiredExecutor executor = new TiredExecutor(numThreads);
        
        List<Runnable> tasks = new ArrayList<>();
        Set<Integer> completedTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            tasks.add(() -> {
                try {
                    Thread.sleep(10);
                    completedTasks.add(taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        long start = System.currentTimeMillis();
        
        executor.submitAll(tasks);
        
        long duration = System.currentTimeMillis() - start;
        
        assertEquals(numTasks, completedTasks.size(), "All tasks should be completed");
        assertTrue(duration >= 50, "Main thread should have waited");
        
        executor.shutdown();
    }

    @Test
    public void testLoadBalancing() throws InterruptedException {
        int numThreads = 3;
        TiredExecutor executor = new TiredExecutor(numThreads);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                double x = 0;
                for(int j=0; j<10000; j++) x += Math.sin(j);
            });
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        
        assertFalse(report.contains("Time Used = 0 ns"), "Work distributed among workers");
        
        executor.shutdown();
    }

    @Test
    public void testEmptyTaskList() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        List<Runnable> emptyList = new ArrayList<>();
        
        assertDoesNotThrow(() -> executor.submitAll(emptyList), "Handled empty list gracefully");
        
        executor.shutdown();
    }
    
    @Test
    public void testShutdownBehavior() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        
        executor.submitAll(Collections.singletonList(() -> {}));
        
        assertDoesNotThrow(() -> executor.shutdown(), "Shutdown completed without hanging");
    }

    @Test
    public void testErrorReporting() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> { throw new RuntimeException("Boom!"); });
        tasks.add(() -> { try { Thread.sleep(50); } catch(Exception e){} });
        
        assertDoesNotThrow(() -> executor.submitAll(tasks), "Executor survived task exception");
        
        executor.shutdown();
    }
}