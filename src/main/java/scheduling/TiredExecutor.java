package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger; 


public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            double fatigueFactor = 0.5 + Math.random(); // Example: random fatigue factor between 0.5 and 1.5
            TiredThread worker = new TiredThread(i, fatigueFactor);
            workers[i] = worker;
            idleMinHeap.add(worker);
            worker.start();
        }
    }

    public void submit(Runnable task) {
        try {
            TiredThread worker = idleMinHeap.take(); // Blocking call
            inFlight.incrementAndGet();
            worker.newTask(() -> {
                try {
                    task.run();
                } finally {
                    int remaining = inFlight.decrementAndGet();
                    if (remaining == 0) {
                        synchronized (this) {
                            notifyAll(); // Wake up the main thread waiting in submitAll
                        }
                    }
                    idleMinHeap.add(worker);
                }
            });
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to submit task", e);
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // Submit all tasks to the pool
        for (Runnable task : tasks) {
            submit(task);
        }

        // Block until all tasks are finished
        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    wait(); // Releases lock and waits for notifyAll() from a worker
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for tasks to complete", e);  
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        String report = "";
        for (TiredThread worker : workers) {
            report += String.format("Worker %d: Time Used = %d ns, Time Idle = %d ns, Fatigue = %.2f\n",
                    worker.getWorkerId(),
                    worker.getTimeUsed(),
                    worker.getTimeIdle(),
                    worker.getFatigue());
        }
        return report;
    }

    public double getFairness() {
        double totalFatigue = 0.0;
        for (TiredThread worker : workers) {
            totalFatigue += worker.getFatigue();
        }
        double averageFatigue = totalFatigue / workers.length;

        double fairness = 0.0;
        for (TiredThread worker : workers) {
            fairness += Math.pow(worker.getFatigue() - averageFatigue, 2);
        }

        return fairness;
    }

}
