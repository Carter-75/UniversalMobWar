package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.util.OperationScheduler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.*;

/**
 * Manages mob alliances and friend-based combat assistance.
 * Mobs attacking the same target become temporary allies.
 */
public class AllianceSystem {
    
    // Tracks when alliances were formed (for expiration)
    private static final Map<UUID, Map<UUID, Long>> allianceTimestamps = new HashMap<>();
    
    // Weak alliances (different species)
    private static final long ALLIANCE_DURATION_MS = 5000; // 5 seconds - very temporary
    private static final double ALLIANCE_BREAK_CHANCE = 0.3; // 30% chance to break alliance early
    
    // Strong alliances (same species, when same-species combat is disabled)
    private static final long SAME_SPECIES_ALLIANCE_DURATION_MS = 20000; // 20 seconds - much stronger
    private static final double SAME_SPECIES_ALLIANCE_BREAK_CHANCE = 0.05; // Only 5% chance to refuse
    
    // Tracks species-based alliance strength
    private static final Map<UUID, Map<UUID, Boolean>> strongAlliances = new HashMap<>();
    
    /**
     * Updates alliances for a mob based on who they're fighting with.
     * OPTIMIZED: Smart scheduled with 0.3s minimum delay, prevents operation overlaps.
     * ANTI-STARVATION: Won't queue if queue is full (drops operation - mob will retry later).
     * Same-species alliances are STRONG (when same-species combat is disabled).
     * Different-species alliances are WEAK and temporary.
     * Alliances break immediately when target dies or changes.
     */
    public static void updateAlliances(MobEntity mob, ServerWorld world) {
        String mobId = mob.getUuid().toString();
        
        // SMART SCHEDULING: Only process if not overlapping with other operations
        if (!OperationScheduler.canExecuteAlliance(mobId)) {
            // ANTI-STARVATION: Queue is either full or operation on cooldown
            // Queue for later - schedule 300ms in the future
            OperationScheduler.incrementAllianceQueue(); // Track queue depth
            world.getServer().execute(() -> {
                try {
                    Thread.sleep(300); // 0.3s delay
                    updateAlliancesInternal(mob, world);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    OperationScheduler.decrementAllianceQueue(); // Done - free queue slot
                }
            });
            return;
        }
        
        // Execute immediately if allowed
        updateAlliancesInternal(mob, world);
        OperationScheduler.markAllianceExecuted(mobId);
    }
    
    /**
     * Internal alliance update processing.
     */
    private static void updateAlliancesInternal(MobEntity mob, ServerWorld world) {
        LivingEntity target = mob.getTarget();
        MobWarData mobData = MobWarData.get(mob);
        UUID mobUuid = mob.getUuid();
        
        // If no target, clear all alliances
        if (target == null || !target.isAlive()) {
            clearAllAlliances(mob);
            return;
        }
        
        UUID targetUuid = target.getUuid();
        
        // If target changed, break old alliances
        if (mobData.getCurrentTarget() != null && !mobData.getCurrentTarget().equals(targetUuid)) {
            clearAllAlliances(mob);
        }
        
        // Update current target
        mobData.setCurrentTarget(targetUuid);
        
        // Check if same-species combat is allowed (chaos mode)
        boolean ignoreSameSpecies = world.getGameRules().getBoolean(
            mod.universalmobwar.UniversalMobWarMod.IGNORE_SAME_SPECIES_RULE
        );
        
        // Find other mobs attacking the same target
        double range = 16.0; // Alliance detection range
        Box searchBox = mob.getBoundingBox().expand(range);
        
        List<MobEntity> nearbyMobs = world.getEntitiesByClass(
            MobEntity.class, 
            searchBox, 
            other -> other != mob && other.isAlive() && other.getTarget() == target
        );
        
        // Form alliances with mobs attacking the same target
        long currentTime = System.currentTimeMillis();
        for (MobEntity ally : nearbyMobs) {
            UUID allyUuid = ally.getUuid();
            
            // Check if same species
            boolean sameSpecies = mob.getType() == ally.getType();
            
            // Determine alliance strength
            boolean isStrongAlliance = sameSpecies && ignoreSameSpecies;
            
            // If chaos mode (same-species can fight), no special bonding
            double breakChance = isStrongAlliance ? SAME_SPECIES_ALLIANCE_BREAK_CHANCE : ALLIANCE_BREAK_CHANCE;
            
            // Random chance to not ally with this specific mob (trust issues)
            if (Math.random() < breakChance) continue;
            
            // Add bidirectional alliance
            mobData.addAlly(allyUuid);
            
            MobWarData allyData = MobWarData.get(ally);
            allyData.addAlly(mobUuid);
            MobWarData.save(ally, allyData);
            
            // Track alliance timestamp
            allianceTimestamps.computeIfAbsent(mobUuid, k -> new HashMap<>()).put(allyUuid, currentTime);
            allianceTimestamps.computeIfAbsent(allyUuid, k -> new HashMap<>()).put(mobUuid, currentTime);
            
            // Track if this is a strong alliance
            if (isStrongAlliance) {
                strongAlliances.computeIfAbsent(mobUuid, k -> new HashMap<>()).put(allyUuid, true);
                strongAlliances.computeIfAbsent(allyUuid, k -> new HashMap<>()).put(mobUuid, true);
            }
        }
        
        MobWarData.save(mob, mobData);
    }
    
    /**
     * Clears all alliances for a mob (when target dies or changes).
     */
    private static void clearAllAlliances(MobEntity mob) {
        MobWarData mobData = MobWarData.get(mob);
        UUID mobUuid = mob.getUuid();
        
        // Break alliances with all allies
        for (UUID allyUuid : new HashSet<>(mobData.getAllies())) {
            mobData.removeAlly(allyUuid);
        }
        
        mobData.clearTarget();
        MobWarData.save(mob, mobData);
        
        // Clean up timestamps and strong alliance markers
        allianceTimestamps.remove(mobUuid);
        strongAlliances.remove(mobUuid);
    }
    
    /**
     * Finds a friendly mob that needs help (is being attacked).
     * Returns the attacker that the mob should help defend against.
     * Behavior varies based on alliance strength:
     * - Weak (different species): 70% chance to ignore, very self-focused
     * - Strong (same species): 20% chance to ignore, more cooperative
     */
    public static LivingEntity findFriendToHelp(MobEntity mob, double range) {
        MobWarData mobData = MobWarData.get(mob);
        Set<UUID> allies = mobData.getAllies();
        UUID mobUuid = mob.getUuid();
        
        if (allies.isEmpty()) return null;
        
        ServerWorld world = (ServerWorld) mob.getWorld();
        
        // Check for strong alliances first (same-species bonds)
        Map<UUID, Boolean> strongAllyMap = strongAlliances.get(mobUuid);
        boolean hasStrongAllies = strongAllyMap != null && !strongAllyMap.isEmpty();
        
        // Determine help probability and range based on alliance strength
        double ignoreChance;
        double searchRange;
        if (hasStrongAllies) {
            ignoreChance = 0.2; // 20% chance to ignore (much more helpful)
            searchRange = range * 0.8; // 80% of normal range
        } else {
            ignoreChance = 0.7; // 70% chance to ignore (very selfish)
            searchRange = range * 0.5; // Half range for helping
        }
        
        // Random chance to not help at all
        if (Math.random() < ignoreChance) return null;
        
        Box searchBox = mob.getBoundingBox().expand(searchRange);
        
        // Find allied mobs in range
        List<MobEntity> nearbyAllies = world.getEntitiesByClass(
            MobEntity.class,
            searchBox,
            other -> other != mob && allies.contains(other.getUuid()) && other.isAlive()
        );
        
        // Prioritize helping strong allies (same species)
        for (MobEntity ally : nearbyAllies) {
            UUID allyUuid = ally.getUuid();
            boolean isStrongAlly = strongAllyMap != null && strongAllyMap.getOrDefault(allyUuid, false);
            
            LivingEntity allyTarget = ally.getTarget();
            if (allyTarget != null && allyTarget.isAlive()) {
                // Strong allies: help even with different targets
                if (isStrongAlly && mob.canSee(allyTarget) && Math.random() < 0.8) {
                    return allyTarget; // 80% chance to help same-species ally
                }
                
                // Weak allies: only help if fighting same target
                if (!isStrongAlly && allyTarget == mob.getTarget() && Math.random() < 0.5) {
                    return allyTarget; // 50% chance to coordinate with different-species
                }
            }
        }
        
        return null;
    }
    
    /**
     * Cleans up expired alliances.
     * Strong alliances (same species) last longer than weak alliances.
     */
    public static void cleanupExpiredAlliances(MobEntity mob) {
        MobWarData mobData = MobWarData.get(mob);
        UUID mobUuid = mob.getUuid();
        long currentTime = System.currentTimeMillis();
        
        Map<UUID, Long> mobAlliances = allianceTimestamps.get(mobUuid);
        if (mobAlliances == null) return;
        
        Map<UUID, Boolean> strongAllyMap = strongAlliances.get(mobUuid);
        
        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : mobAlliances.entrySet()) {
            UUID allyUuid = entry.getKey();
            long allianceAge = currentTime - entry.getValue();
            
            // Check if this is a strong alliance
            boolean isStrongAlliance = strongAllyMap != null && strongAllyMap.getOrDefault(allyUuid, false);
            
            // Use appropriate duration based on alliance strength
            long maxDuration = isStrongAlliance ? SAME_SPECIES_ALLIANCE_DURATION_MS : ALLIANCE_DURATION_MS;
            
            if (allianceAge > maxDuration) {
                toRemove.add(allyUuid);
            }
        }
        
        // Remove expired alliances
        for (UUID allyUuid : toRemove) {
            mobData.removeAlly(allyUuid);
            mobAlliances.remove(allyUuid);
            
            // Clean up strong alliance marker
            if (strongAllyMap != null) {
                strongAllyMap.remove(allyUuid);
            }
        }
        
        if (!toRemove.isEmpty()) {
            MobWarData.save(mob, mobData);
        }
    }
    
    /**
     * Checks if two mobs are allies.
     */
    public static boolean areAllies(MobEntity mob1, MobEntity mob2) {
        MobWarData data1 = MobWarData.get(mob1);
        return data1.isAlly(mob2.getUuid());
    }
    
    /**
     * Breaks alliance between two mobs.
     */
    public static void breakAlliance(MobEntity mob1, MobEntity mob2) {
        MobWarData data1 = MobWarData.get(mob1);
        MobWarData data2 = MobWarData.get(mob2);
        
        data1.removeAlly(mob2.getUuid());
        data2.removeAlly(mob1.getUuid());
        
        MobWarData.save(mob1, data1);
        MobWarData.save(mob2, data2);
        
        // Clean up timestamps and strong alliance markers
        UUID uuid1 = mob1.getUuid();
        UUID uuid2 = mob2.getUuid();
        
        Map<UUID, Long> alliances1 = allianceTimestamps.get(uuid1);
        if (alliances1 != null) alliances1.remove(uuid2);
        
        Map<UUID, Long> alliances2 = allianceTimestamps.get(uuid2);
        if (alliances2 != null) alliances2.remove(uuid1);
        
        Map<UUID, Boolean> strongAllies1 = strongAlliances.get(uuid1);
        if (strongAllies1 != null) strongAllies1.remove(uuid2);
        
        Map<UUID, Boolean> strongAllies2 = strongAlliances.get(uuid2);
        if (strongAllies2 != null) strongAllies2.remove(uuid1);
    }
}

