package net.meowsers.Peach.Threading;

import java.util.concurrent.ScheduledFuture;

public class PeachThread {
    private final String name;
    private final ScheduledFuture<?> future;

    public PeachThread(String name, ScheduledFuture<?> future) {
        this.name = name;
        this.future = future;
    }

    /**
     * Stop or cancel this thread's execution immediately.
     */
    public void stop() {
        if (future != null) {
            future.cancel(true); // Sends interrupt signal if currently executing
        }
    }

    /**
     * Returns true if the thread/loop is actively running.
     */
    public boolean isRunning() {
        return future != null && !future.isDone() && !future.isCancelled();
    }

    public String getName() {
        return name;
    }
}