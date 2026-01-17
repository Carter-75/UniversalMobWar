package mod.universalmobwar.system;

import mod.universalmobwar.UniversalMobWarMod;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Centralized async executor for upgrade jobs.
 */
public final class UpgradeJobScheduler {

    private static final UpgradeJobScheduler INSTANCE = new UpgradeJobScheduler();

    private final ScheduledThreadPoolExecutor executor;
    private final ConcurrentMap<UUID, Future<?>> activeJobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ScalingSystem.UpgradeJobResult> completedResults = new ConcurrentHashMap<>();

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

        // Use a FutureTask so we can register it in the map before it can run.
        // This avoids races where a job finishes before tracking is established,
        // and ensures old jobs can't remove newer jobs for the same mob.
        final FutureTask<Void>[] holder = new FutureTask[1];
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                ScalingSystem.UpgradeJobResult result = callable.call();
                if (result != null) {
                    completedResults.put(mobId, result);
                }
            } catch (Exception ex) {
                UniversalMobWarMod.LOGGER.error("[UpgradeJobScheduler] Upgrade job failed for {}: {}", mobId, ex.getMessage());
            } finally {
                FutureTask<Void> self = holder[0];
                if (self != null) {
                    activeJobs.remove(mobId, self);
                } else {
                    activeJobs.remove(mobId);
                }
            }
            return null;
        });
        holder[0] = task;

        Future<?> previous = activeJobs.put(mobId, task);
        if (previous != null) {
            previous.cancel(true);
        }

        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            activeJobs.remove(mobId, task);
            UniversalMobWarMod.LOGGER.error("[UpgradeJobScheduler] Upgrade job rejected for {}: {}", mobId, ex.getMessage());
        }
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
        return activeJobs.isEmpty() && completedResults.isEmpty();
    }

    public int getActiveJobCount() {
        return activeJobs.size();
    }

    public void cancel(UUID mobId) {
        if (mobId == null) {
            return;
        }
        Future<?> future = activeJobs.remove(mobId);
        if (future != null) {
            future.cancel(true);
        }
        completedResults.remove(mobId);
    }
}
