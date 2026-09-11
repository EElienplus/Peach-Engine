package net.meowsers.Peach.Threading;

import net.meowsers.Peach.Threading.PeachThread;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadManager {
    private final ScheduledExecutorService executorPool;
    private final Map<String, PeachThread> activeLoops = new ConcurrentHashMap<>();

    public ThreadManager(int threadPoolSize) {
        AtomicInteger threadCount = new AtomicInteger(1);

        // Custom ThreadFactory to assign clear engine thread names and uncaught exception handling
        ThreadFactory customFactory = runnable -> {
            Thread t = new Thread(runnable, "PeachWorker-" + threadCount.getAndIncrement());
            t.setDaemon(true); // Don't block JVM exit if main engine loop terminates
            t.setUncaughtExceptionHandler((thread, throwable) -> {
                System.err.printf("[PeachEngine Thread Error] Thread '%s' crashed: %s%n",
                        thread.getName(), throwable.getMessage());
                throwable.printStackTrace();
            });
            return t;
        };

        this.executorPool = Executors.newScheduledThreadPool(threadPoolSize, customFactory);
    }

    public void runAsync(Runnable task) {
        executorPool.submit(wrapSafety(task));
    }

    /** Submits an async tasks and returns a result */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executorPool.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /** Start a repeating named loop running at a fixed millisecond interval. */
    public synchronized PeachThread runLoop(String loopName, long intervalMillis, Runnable task) {
        // Cancel existing loop with the same name if present
        stopLoop(loopName);

        ScheduledFuture<?> future = executorPool.scheduleAtFixedRate(
                wrapSafety(task),
                0,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );

        PeachThread handle = new PeachThread(loopName, future);
        activeLoops.put(loopName, handle);
        return handle;
    }

    public synchronized void stopLoop(String loopName) {
        PeachThread handle = activeLoops.remove(loopName);
        if (handle != null) {
            handle.stop();
        }
    }

    public boolean isLoopRunning(String loopName) {
        PeachThread handle = activeLoops.get(loopName);
        return handle != null && handle.isRunning();
    }

    public void shutdownAll() {
        System.out.println("[ThreadManager] Shutting down all engine threads...");
        activeLoops.values().forEach(PeachThread::stop);
        activeLoops.clear();
        executorPool.shutdownNow();
    }

    private Runnable wrapSafety(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                System.err.println("[PeachEngine Task Error] " + t.getMessage());
                t.printStackTrace();
            }
        };
    }
}