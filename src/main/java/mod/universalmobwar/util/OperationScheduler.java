package mod.universalmobwar.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smart operation scheduler that prevents overlapping operations.
 * Ensures all operations have minimum delays and don't execute simultaneously.
 * ANTI-STARVATION: Limits queue depth and prioritizes older operations.
 */
public class OperationScheduler {
    
    // Track when each operation type last ran (per entity or globally)
    private static final Map<String, Long> lastOperationTime = new ConcurrentHashMap<>();
    
    // Track operation attempt counts (for priority/starvation prevention)
    private static final Map<String, Integer> operationAttempts = new ConcurrentHashMap<>();
    
    // Track current queue depth per operation type
    private static final AtomicInteger evolutionQueueDepth = new AtomicInteger(0);
    private static final AtomicInteger allianceQueueDepth = new AtomicInteger(0);
    private static final AtomicInteger targetingQueueDepth = new AtomicInteger(0);
    private static final AtomicInteger warlordQueueDepth = new AtomicInteger(0);
    
    // ANTI-STARVATION: Maximum queue depths (prevents infinite pileup)
    private static final int MAX_QUEUE_DEPTH = 10; // Only queue up to 10 of same operation type
    
    // ANTI-STARVATION: Maximum wait time before forcing execution (in milliseconds)
    private static final long MAX_WAIT_TIME = 2000; // 2 seconds max wait
    
    // BASE delays for each operation type (dynamically adjusted based on load)
    private static final long BASE_DELAY_EVOLUTION = 100; // Base 0.1s (scales up under load)
    private static final long BASE_DELAY_ALLIANCE = 150; // Base 0.15s
    private static final long BASE_DELAY_TARGETING = 200; // Base 0.2s
    private static final long BASE_DELAY_WARLORD_OPERATION = 250; // Base 0.25s
    
    // Maximum delays (when server is overloaded)
    private static final long MAX_DELAY_EVOLUTION = 500; // Max 0.5s
    private static final long MAX_DELAY_ALLIANCE = 600; // Max 0.6s
    private static final long MAX_DELAY_TARGETING = 800; // Max 0.8s
    private static final long MAX_DELAY_WARLORD_OPERATION = 1000; // Max 1.0s
    
    // Global cooldown to prevent ANY operations from overlapping (dynamically adjusted)
    private static long currentGlobalDelay = 50; // Starts at 50ms, scales with load
    private static final long MIN_GLOBAL_DELAY = 25; // Minimum 25ms (fast when quiet)
    private static final long MAX_GLOBAL_DELAY = 100; // Maximum 100ms (slow when busy)
    
    private static long lastGlobalOperation = 0;
    
    // OVERHEAD REDUCTION: Only recalculate delays every 50ms (not every check)
    private static long lastDelayAdjustment = 0;
    private static final long DELAY_ADJUSTMENT_INTERVAL = 50; // 50ms between adjustments
    
    /**
     * Calculates dynamic delay based on current queue depth (adapts to load).
     * More items in queue = longer delays to prevent overload.
     */
    private static long calculateDynamicDelay(long baseDelay, long maxDelay, int queueDepth) {
        // Scale delay based on queue depth: 0% full = base delay, 100% full = max delay
        float loadFactor = Math.min(1.0f, (float) queueDepth / MAX_QUEUE_DEPTH);
        long dynamicDelay = baseDelay + (long)((maxDelay - baseDelay) * loadFactor);
        
        return dynamicDelay;
    }
    
    /**
     * Adjusts global delay based on overall system load.
     * LOW OVERHEAD: Only runs every 50ms (not every operation check).
     */
    private static void adjustGlobalDelay(long currentTime) {
        // OVERHEAD REDUCTION: Skip if we adjusted recently
        if (currentTime - lastDelayAdjustment < DELAY_ADJUSTMENT_INTERVAL) {
            return; // Skip - we adjusted too recently
        }
        lastDelayAdjustment = currentTime;
        
        // Calculate total queue load across all operation types (single read per atomic)
        int totalQueueLoad = evolutionQueueDepth.get() + allianceQueueDepth.get() + 
                             targetingQueueDepth.get() + warlordQueueDepth.get();
        
        // If queues are building up, slow down. If empty, speed up.
        if (totalQueueLoad > MAX_QUEUE_DEPTH * 2) {
            // Heavy load - increase delay
            currentGlobalDelay = Math.min(MAX_GLOBAL_DELAY, currentGlobalDelay + 5);
        } else if (totalQueueLoad == 0) {
            // No load - decrease delay (be faster!)
            currentGlobalDelay = Math.max(MIN_GLOBAL_DELAY, currentGlobalDelay - 5);
        } else {
            // Moderate load - gradually return to middle
            long targetDelay = MIN_GLOBAL_DELAY + ((MAX_GLOBAL_DELAY - MIN_GLOBAL_DELAY) / 2);
            if (currentGlobalDelay > targetDelay) {
                currentGlobalDelay = Math.max(targetDelay, currentGlobalDelay - 2);
            } else if (currentGlobalDelay < targetDelay) {
                currentGlobalDelay = Math.min(targetDelay, currentGlobalDelay + 2);
            }
        }
    }
    
    /**
     * Checks if an operation can run now, considering both its specific cooldown,
     * global overlap prevention, ANTI-STARVATION rules, and DYNAMIC DELAYS.
     * 
     * @param operationKey Unique key for this operation (e.g., "evolution_mobUUID")
     * @param baseDelay Base delay for this operation type (before load scaling)
     * @return true if operation can run now
     */
    public static boolean canExecute(String operationKey, long baseDelay) {
        long currentTime = System.currentTimeMillis();
        
        // ANTI-STARVATION: Check if operation has been waiting too long
        Long firstAttemptTime = getFirstAttemptTime(operationKey);
        if (firstAttemptTime != null && (currentTime - firstAttemptTime) > MAX_WAIT_TIME) {
            // FORCE EXECUTE: Waited > 2 seconds, ignore normal rules
            adjustGlobalDelay(currentTime); // Adjust delays based on load (low overhead)
            return true;
        }
        
        // Check global overlap prevention first (uses DYNAMIC delay)
        if (currentTime - lastGlobalOperation < currentGlobalDelay) {
            incrementAttempts(operationKey); // Track that we tried
            return false; // Too soon after any other operation
        }
        
        // Check specific operation cooldown (uses BASE delay, not scaled)
        Long lastTime = lastOperationTime.get(operationKey);
        if (lastTime != null && (currentTime - lastTime) < baseDelay) {
            incrementAttempts(operationKey); // Track that we tried
            return false; // This specific operation is on cooldown
        }
        
        adjustGlobalDelay(currentTime); // Adjust delays based on current load (low overhead - only every 50ms)
        return true;
    }
    
    /**
     * Tracks when an operation first attempted to run (for starvation prevention).
     */
    private static Long getFirstAttemptTime(String operationKey) {
        String attemptKey = operationKey + "_first_attempt";
        Long firstTime = lastOperationTime.get(attemptKey);
        
        if (firstTime == null) {
            // First attempt - record it
            firstTime = System.currentTimeMillis();
            lastOperationTime.put(attemptKey, firstTime);
        }
        
        return firstTime;
    }
    
    /**
     * Increments attempt counter for an operation.
     */
    private static void incrementAttempts(String operationKey) {
        operationAttempts.merge(operationKey, 1, Integer::sum);
    }
    
    /**
     * Clears attempt tracking for an operation (after successful execution).
     */
    private static void clearAttempts(String operationKey) {
        operationAttempts.remove(operationKey);
        lastOperationTime.remove(operationKey + "_first_attempt");
    }
    
    /**
     * Marks an operation as executed, updating both specific and global timers.
     * ANTI-STARVATION: Clears attempt tracking.
     */
    public static void markExecuted(String operationKey) {
        long currentTime = System.currentTimeMillis();
        lastOperationTime.put(operationKey, currentTime);
        lastGlobalOperation = currentTime;
        clearAttempts(operationKey); // Clear starvation tracking
    }
    
    /**
     * Attempts to execute an operation with smart scheduling.
     * Returns true if executed, false if delayed.
     */
    public static boolean tryExecute(String operationKey, long minDelay, Runnable operation) {
        if (canExecute(operationKey, minDelay)) {
            operation.run();
            markExecuted(operationKey);
            return true;
        }
        return false;
    }
    
    /**
     * Evolution operation - DYNAMIC delay (0.1s base, up to 0.5s under load)
     * ANTI-STARVATION: Checks queue depth to prevent pileup.
     */
    public static boolean canExecuteEvolution(String entityId) {
        // ANTI-STARVATION: Check if queue is too full
        if (evolutionQueueDepth.get() >= MAX_QUEUE_DEPTH) {
            return false; // Queue full - drop this operation (mob will retry naturally)
        }
        
        // SMART DELAY: Calculate based on current queue depth
        long dynamicDelay = calculateDynamicDelay(BASE_DELAY_EVOLUTION, MAX_DELAY_EVOLUTION, evolutionQueueDepth.get());
        
        return canExecute("evolution_" + entityId, dynamicDelay);
    }
    
    public static void markEvolutionExecuted(String entityId) {
        markExecuted("evolution_" + entityId);
    }
    
    /**
     * Increments evolution queue depth (call when queuing delayed operation).
     */
    public static void incrementEvolutionQueue() {
        evolutionQueueDepth.incrementAndGet();
    }
    
    /**
     * Decrements evolution queue depth (call when operation completes).
     */
    public static void decrementEvolutionQueue() {
        evolutionQueueDepth.decrementAndGet();
    }
    
    /**
     * Alliance operation - DYNAMIC delay (0.15s base, up to 0.6s under load)
     * ANTI-STARVATION: Checks queue depth to prevent pileup.
     */
    public static boolean canExecuteAlliance(String entityId) {
        if (allianceQueueDepth.get() >= MAX_QUEUE_DEPTH) {
            return false; // Queue full - drop this operation
        }
        
        // SMART DELAY: Calculate based on current queue depth
        long dynamicDelay = calculateDynamicDelay(BASE_DELAY_ALLIANCE, MAX_DELAY_ALLIANCE, allianceQueueDepth.get());
        
        return canExecute("alliance_" + entityId, dynamicDelay);
    }
    
    public static void markAllianceExecuted(String entityId) {
        markExecuted("alliance_" + entityId);
    }
    
    public static void incrementAllianceQueue() {
        allianceQueueDepth.incrementAndGet();
    }
    
    public static void decrementAllianceQueue() {
        allianceQueueDepth.decrementAndGet();
    }
    
    /**
     * Targeting operation - DYNAMIC delay (0.2s base, up to 0.8s under load)
     * ANTI-STARVATION: Checks queue depth to prevent pileup.
     */
    public static boolean canExecuteTargeting(String entityId) {
        if (targetingQueueDepth.get() >= MAX_QUEUE_DEPTH) {
            return false; // Queue full - drop this operation
        }
        
        // SMART DELAY: Calculate based on current queue depth
        long dynamicDelay = calculateDynamicDelay(BASE_DELAY_TARGETING, MAX_DELAY_TARGETING, targetingQueueDepth.get());
        
        return canExecute("targeting_" + entityId, dynamicDelay);
    }
    
    public static void markTargetingExecuted(String entityId) {
        markExecuted("targeting_" + entityId);
    }
    
    public static void incrementTargetingQueue() {
        targetingQueueDepth.incrementAndGet();
    }
    
    public static void decrementTargetingQueue() {
        targetingQueueDepth.decrementAndGet();
    }
    
    /**
     * Warlord operation - DYNAMIC delay (0.25s base, up to 1.0s under load)
     * ANTI-STARVATION: Checks queue depth to prevent pileup.
     */
    public static boolean canExecuteWarlordOp(String entityId, String opType) {
        if (warlordQueueDepth.get() >= MAX_QUEUE_DEPTH) {
            return false; // Queue full - drop this operation
        }
        
        // SMART DELAY: Calculate based on current queue depth
        long dynamicDelay = calculateDynamicDelay(BASE_DELAY_WARLORD_OPERATION, MAX_DELAY_WARLORD_OPERATION, warlordQueueDepth.get());
        
        return canExecute("warlord_" + opType + "_" + entityId, dynamicDelay);
    }
    
    public static void markWarlordOpExecuted(String entityId, String opType) {
        markExecuted("warlord_" + opType + "_" + entityId);
    }
    
    public static void incrementWarlordQueue() {
        warlordQueueDepth.incrementAndGet();
    }
    
    public static void decrementWarlordQueue() {
        warlordQueueDepth.decrementAndGet();
    }
    
    /**
     * Cleanup old entries (call periodically to prevent memory buildup)
     * ANTI-STARVATION: Also cleans up old attempt tracking.
     */
    /**
     * Cleanup old entries from maps to prevent memory leaks.
     * LOW OVERHEAD: Only cleans if maps are large, runs every 5 seconds.
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 10000; // Remove entries older than 10 seconds
        
        // OVERHEAD REDUCTION: Only clean if maps are getting large (> 50 entries)
        if (lastOperationTime.size() > 50) {
            lastOperationTime.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > maxAge
            );
        }
        
        // Clean up old attempt counters (prevent memory leak) - only if map is large
        if (operationAttempts.size() > 50) {
            operationAttempts.entrySet().removeIf(entry -> entry.getValue() > 100); // Remove if >100 failed attempts
        }
        
        // Reset queue depths if they're negative (shouldn't happen but safety check)
        // FAST: Direct comparison without method calls
        int evol = evolutionQueueDepth.get();
        int allia = allianceQueueDepth.get();
        int targ = targetingQueueDepth.get();
        int warl = warlordQueueDepth.get();
        
        if (evol < 0) evolutionQueueDepth.set(0);
        if (allia < 0) allianceQueueDepth.set(0);
        if (targ < 0) targetingQueueDepth.set(0);
        if (warl < 0) warlordQueueDepth.set(0);
    }
}

