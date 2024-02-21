package org.greenplum.pxf.plugins.jdbc.writercallable;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class TimeoutFixedThreadPoolExecutor extends ThreadPoolExecutor {
    private final long timeout;
    private final TimeUnit timeoutUnit;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<Runnable, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    public TimeoutFixedThreadPoolExecutor(int poolSize, long timeout, TimeUnit timeoutUnit) {
        super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    @Override
    public void shutdown() {
        timeoutExecutor.shutdown();
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> terminatedTimeoutTasks = timeoutExecutor.shutdownNow();
        if (!terminatedTimeoutTasks.isEmpty()) {
            log.warn("The following timeout tasks were terminated due to shutdownNow: {}", terminatedTimeoutTasks);
        }
        return super.shutdownNow();
    }

    @Override
    protected void beforeExecute(Thread executingThread, Runnable r) {
        super.beforeExecute(executingThread, r);

        final ScheduledFuture<?> scheduled = timeoutExecutor.schedule(() -> {
            log.warn("Thread {} has timed out after {} {}", executingThread.getName(), timeout, timeoutUnit);
            executingThread.interrupt();
        }, timeout, timeoutUnit);
        log.trace("Thread {} will time out in {} {}", executingThread.getName(), timeout, timeoutUnit);
        runningTasks.put(r, scheduled);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        Optional.ofNullable(runningTasks.remove(r))
                .ifPresent(task -> task.cancel(false));
    }
}
