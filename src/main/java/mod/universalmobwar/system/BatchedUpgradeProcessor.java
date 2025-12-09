package mod.universalmobwar.system;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.MobConfig;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.*;

/**
 * BATCHED UPGRADE PROCESSOR
 * 
 * Implements skilltree.txt requirement:
 * - Collects all mobs that need upgrading
 * - Processes them all at once using multithreading
 * - Completes within configurable time (default 5 seconds)
 * - Applies equipment AFTER all calculations
 */
public class BatchedUpgradeProcessor {
    
    private static final Map<UUID, PendingUpgrade> pendingUpgrades = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ExecutorService workerPool = null;
    private static ScheduledFuture<?> processingTask = null;
    
    // Configuration (from ModConfig)
    private static int targetProcessingTimeMs = 5000; // 5 seconds default
    private static boolean isProcessing = false;
    
    static class PendingUpgrade {
        final MobEntity mob;
        final ServerWorld world;
        final MobWarData data;
        final PowerProfile profile;
        final MobConfig config;
        final long addedTime;
        
        PendingUpgrade(MobEntity mob, ServerWorld world, MobWarData data, PowerProfile profile, MobConfig config) {
            this.mob = mob;
            this.world = world;
            this.data = data;
            this.profile = profile;
            this.config = config;
            this.addedTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Add a mob to the upgrade queue
     * Called from EvolutionSystem when a mob needs upgrading
     */
    public static void queueMobForUpgrade(MobEntity mob, ServerWorld world, MobWarData data, PowerProfile profile) {
        String mobName = ArchetypeClassifier.getMobName(mob);
        MobConfig config = MobConfig.load(mobName);
        if (config == null) return;
        
        UUID mobId = mob.getUuid();
        pendingUpgrades.put(mobId, new PendingUpgrade(mob, world, data, profile, config));
        
        // Schedule processing if not already scheduled
        scheduleProcessing();
        
        if (ModConfig.getInstance().debugUpgradeLog) {
            UniversalMobWarMod.LOGGER.info("Queued mob for upgrade: {} (Queue size: {})", mobName, pendingUpgrades.size());
        }
    }
    
    /**
     * Schedule the batch processing task
     * Ensures we process all mobs within the configured time window
     */
    private static synchronized void scheduleProcessing() {
        if (processingTask != null && !processingTask.isDone()) {
            return; // Already scheduled
        }
        
        // Read config
        ModConfig config = ModConfig.getInstance();
        targetProcessingTimeMs = config.upgradeProcessingTimeMs;
        
        // Schedule processing after target time
        processingTask = scheduler.schedule(() -> {
            processBatch();
        }, targetProcessingTimeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Process all queued mobs using multithreading
     * Distributes work dynamically to meet time target
     */
    private static synchronized void processBatch() {
        if (isProcessing) return;
        isProcessing = true;
        
        long startTime = System.currentTimeMillis();
        ModConfig config = ModConfig.getInstance();
        
        try {
            // Take snapshot of pending upgrades
            Map<UUID, PendingUpgrade> batch = new HashMap<>(pendingUpgrades);
            pendingUpgrades.clear();
            
            if (batch.isEmpty()) {
                if (config.debugLogging) {
                    UniversalMobWarMod.LOGGER.info("No mobs to process in batch");
                }
                return;
            }
            
            int mobCount = batch.size();
            int threadCount = Math.min(Runtime.getRuntime().availableProcessors(), mobCount);
            
            if (config.debugLogging) {
                UniversalMobWarMod.LOGGER.info("Processing batch: {} mobs using {} threads (target: {}ms)", 
                    mobCount, threadCount, targetProcessingTimeMs);
            }
            
            // Create thread pool if needed
            if (workerPool == null || workerPool.isShutdown()) {
                workerPool = Executors.newFixedThreadPool(threadCount);
            }
            
            // Phase 1: Calculate all upgrades (parallel)
            List<Future<UpgradeResult>> futures = new ArrayList<>();
            for (PendingUpgrade pending : batch.values()) {
                Future<UpgradeResult> future = workerPool.submit(() -> {
                    return calculateUpgrades(pending);
                });
                futures.add(future);
            }
            
            // Wait for all calculations (with timeout)
            List<UpgradeResult> results = new ArrayList<>();
            long calcDeadline = startTime + (targetProcessingTimeMs - 500); // Reserve 500ms for application
            
            for (Future<UpgradeResult> future : futures) {
                try {
                    long remainingTime = calcDeadline - System.currentTimeMillis();
                    if (remainingTime > 0) {
                        UpgradeResult result = future.get(remainingTime, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            results.add(result);
                        }
                    } else {
                        future.cancel(true);
                        UniversalMobWarMod.LOGGER.warn("Upgrade calculation timeout - cancelling remaining tasks");
                        break;
                    }
                } catch (TimeoutException e) {
                    future.cancel(true);
                    UniversalMobWarMod.LOGGER.warn("Upgrade calculation timeout for mob");
                } catch (Exception e) {
                    UniversalMobWarMod.LOGGER.error("Error calculating upgrades", e);
                }
            }
            
            // Phase 2: Apply all equipment/effects (on main thread via scheduler)
            for (UpgradeResult result : results) {
                // Schedule on server thread
                result.pending.world.getServer().execute(() -> {
                    applyUpgradeResult(result);
                });
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (config.debugLogging) {
                UniversalMobWarMod.LOGGER.info("Batch processing complete: {} mobs processed in {}ms (target: {}ms)", 
                    results.size(), elapsed, targetProcessingTimeMs);
            }
            
        } finally {
            isProcessing = false;
        }
    }
    
    static class UpgradeResult {
        final PendingUpgrade pending;
        final Map<String, Integer> upgradeLevels;
        final double pointsSpent;
        
        UpgradeResult(PendingUpgrade pending, Map<String, Integer> levels, double spent) {
            this.pending = pending;
            this.upgradeLevels = levels;
            this.pointsSpent = spent;
        }
    }
    
    /**
     * Calculate upgrades for a single mob (runs in worker thread)
     * Implements 80%/20% buy/save logic
     */
    private static UpgradeResult calculateUpgrades(PendingUpgrade pending) {
        Random random = new Random(pending.mob.getUuid().getMostSignificantBits());
        double availablePoints = pending.profile.totalPoints;
        double startingPoints = availablePoints;
        
        Map<String, Integer> levels = new HashMap<>(pending.profile.specialSkills);
        
        int iterations = 0;
        int maxIterations = 10000;
        
        while (availablePoints > 0 && iterations++ < maxIterations) {
            // Collect all affordable upgrades
            List<String> affordable = new ArrayList<>();
            List<Integer> costs = new ArrayList<>();
            
            // Universal upgrades
            for (String upgradeName : pending.config.universalUpgrades.keySet()) {
                int currentLevel = levels.getOrDefault(upgradeName, 0);
                int cost = pending.config.getUniversalUpgradeCost(upgradeName, currentLevel);
                if (cost > 0 && cost <= availablePoints) {
                    affordable.add("universal:" + upgradeName);
                    costs.add(cost);
                }
            }
            
            // Skill tree upgrades
            for (String treeId : pending.config.assignedTrees) {
                Map<String, List<MobConfig.UpgradeCost>> tree = pending.config.skillTrees.get(treeId);
                if (tree != null) {
                    for (String skillName : tree.keySet()) {
                        int currentLevel = levels.getOrDefault(skillName, 0);
                        int cost = pending.config.getSkillTreeUpgradeCost(treeId, skillName, currentLevel);
                        if (cost > 0 && cost <= availablePoints) {
                            affordable.add("tree:" + treeId + ":" + skillName);
                            costs.add(cost);
                        }
                    }
                }
            }
            
            if (affordable.isEmpty()) break;
            
            // 20% chance to SAVE and stop
            if (random.nextDouble() < 0.2) {
                break;
            }
            
            // 80% chance to BUY
            int index = random.nextInt(affordable.size());
            String upgrade = affordable.get(index);
            int cost = costs.get(index);
            
            // Parse and apply upgrade
            if (upgrade.startsWith("universal:")) {
                String name = upgrade.substring(10);
                levels.put(name, levels.getOrDefault(name, 0) + 1);
            } else if (upgrade.startsWith("tree:")) {
                String[] parts = upgrade.split(":");
                String skillName = parts[2];
                levels.put(skillName, levels.getOrDefault(skillName, 0) + 1);
            }
            
            availablePoints -= cost;
        }
        
        double spent = startingPoints - availablePoints;
        return new UpgradeResult(pending, levels, spent);
    }
    
    /**
     * Apply calculated upgrade result to mob (runs on main thread)
     */
    private static void applyUpgradeResult(UpgradeResult result) {
        try {
            // Update profile
            result.pending.profile.specialSkills = result.upgradeLevels;
            result.pending.data.setSpentPoints(result.pointsSpent);
            
            // Apply to mob using UpgradeSystem
            UpgradeSystem.applyToMob(result.pending.mob, result.pending.profile, result.pending.config);
            
            // Save
            result.pending.data.setSkillData(result.pending.profile.writeNbt());
            MobWarData.save(result.pending.mob, result.pending.data);
            
            if (ModConfig.getInstance().debugUpgradeLog) {
                String mobName = ArchetypeClassifier.getMobName(result.pending.mob);
                UniversalMobWarMod.LOGGER.info("Applied upgrades to {}: {} points spent, {} levels purchased",
                    mobName, result.pointsSpent, result.upgradeLevels.size());
            }
        } catch (Exception e) {
            UniversalMobWarMod.LOGGER.error("Error applying upgrade result", e);
        }
    }
    
    /**
     * Cleanup on server stop
     */
    public static void shutdown() {
        if (processingTask != null) {
            processingTask.cancel(false);
        }
        if (workerPool != null) {
            workerPool.shutdown();
        }
        pendingUpgrades.clear();
    }
}
