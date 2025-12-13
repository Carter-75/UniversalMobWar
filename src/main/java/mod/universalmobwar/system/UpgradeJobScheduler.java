package mod.universalmobwar.system;

import mod.universalmobwar.UniversalMobWarMod;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized async executor for upgrade jobs.
 */
public final class UpgradeJobScheduler {

    private static final UpgradeJobScheduler INSTANCE = new UpgradeJobScheduler();

    private final ScheduledThreadPoolExecutor executor;
    private final ConcurrentMap<UUID, Future<?>> activeJobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ScalingSystem.UpgradeJobResult> completedResults = new ConcurrentHashMap<>();
    private final AtomicInteger activeCount = new AtomicInteger();

    private UpgradeJobScheduler() {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "umw-upgrade-worker");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        this.executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(cores, factory);
        this.executor.setRemoveOnCancelPolicy(true);
    }

    public static UpgradeJobScheduler getInstance() {
        return INSTANCE;
    }

    public void submit(UUID mobId, Callable<ScalingSystem.UpgradeJobResult> callable) {
        if (mobId == null || callable == null) {
            return;
        }

        Future<?> future = executor.submit(() -> {
            try {
                ScalingSystem.UpgradeJobResult result = callable.call();
                if (result != null) {
                    completedResults.put(mobId, result);
                }
            } catch (Exception ex) {
                UniversalMobWarMod.LOGGER.error("[UpgradeJobScheduler] Upgrade job failed for {}: {}", mobId, ex.getMessage());
            } finally {
                activeJobs.remove(mobId);
                activeCount.decrementAndGet();
            }
        });

        Future<?> previous = activeJobs.put(mobId, future);
        if (previous != null) {
            previous.cancel(true);
        }
        activeCount.incrementAndGet();
    }

    public ScalingSystem.UpgradeJobResult pollResult(UUID mobId) {
        if (mobId == null) {
            return null;
        }
        return completedResults.remove(mobId);
    }

    public boolean isJobActive(UUID mobId) {
        if (mobId == null) {
            return false;
        }
        Future<?> future = activeJobs.get(mobId);
        return future != null && !future.isDone();
    }

    public boolean isIdle() {
        return activeCount.get() == 0 && completedResults.isEmpty();
    }

    public int getActiveJobCount() {
        return activeCount.get();
    }

    public void cancel(UUID mobId) {
        if (mobId == null) {
            return;
        }
        Future<?> future = activeJobs.remove(mobId);
        if (future != null) {
            future.cancel(true);
            activeCount.updateAndGet(value -> Math.max(0, value - 1));
        }
        completedResults.remove(mobId);
    }
}
