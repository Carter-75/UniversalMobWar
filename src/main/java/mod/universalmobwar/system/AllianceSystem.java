package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
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
    private static final long ALLIANCE_DURATION_MS = 60000; // 60 seconds
    
    /**
     * Updates alliances for a mob based on who they're fighting with.
     * Mobs attacking the same target become allies.
     * Does not trigger if target is a creative mode player.
     */
    public static void updateAlliances(MobEntity mob, ServerWorld world) {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        
        // Don't form alliances when attacking creative mode players
        if (target instanceof net.minecraft.server.network.ServerPlayerEntity player) {
            if (player.isCreative() || player.isSpectator()) return;
        }
        
        MobWarData mobData = MobWarData.get(mob);
        UUID mobUuid = mob.getUuid();
        UUID targetUuid = target.getUuid();
        
        // Update current target
        mobData.setCurrentTarget(targetUuid);
        
        // Find other mobs attacking the same target
        double range = 32.0; // Alliance detection range
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
            
            // Add bidirectional alliance
            mobData.addAlly(allyUuid);
            
            MobWarData allyData = MobWarData.get(ally);
            allyData.addAlly(mobUuid);
            MobWarData.save(ally, allyData);
            
            // Track alliance timestamp
            allianceTimestamps.computeIfAbsent(mobUuid, k -> new HashMap<>()).put(allyUuid, currentTime);
            allianceTimestamps.computeIfAbsent(allyUuid, k -> new HashMap<>()).put(mobUuid, currentTime);
        }
        
        MobWarData.save(mob, mobData);
    }
    
    /**
     * Finds a friendly mob that needs help (is being attacked).
     * Returns the attacker that the mob should help defend against.
     */
    public static LivingEntity findFriendToHelp(MobEntity mob, double range) {
        MobWarData mobData = MobWarData.get(mob);
        Set<UUID> allies = mobData.getAllies();
        
        if (allies.isEmpty()) return null;
        
        ServerWorld world = (ServerWorld) mob.getWorld();
        Box searchBox = mob.getBoundingBox().expand(range);
        
        // Find allied mobs in range
        List<MobEntity> nearbyAllies = world.getEntitiesByClass(
            MobEntity.class,
            searchBox,
            other -> other != mob && allies.contains(other.getUuid()) && other.isAlive()
        );
        
        // Check if any ally is being attacked and find their attacker
        for (MobEntity ally : nearbyAllies) {
            LivingEntity allyTarget = ally.getTarget();
            if (allyTarget != null && allyTarget.isAlive()) {
                // Check if this target is attacking our ally
                if (allyTarget instanceof MobEntity attacker && attacker.getTarget() == ally) {
                    return attacker; // Help defend friend
                }
                // Or if our ally is actively fighting someone, help them
                if (mob.canSee(allyTarget)) {
                    return allyTarget;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Cleans up expired alliances.
     */
    public static void cleanupExpiredAlliances(MobEntity mob) {
        MobWarData mobData = MobWarData.get(mob);
        UUID mobUuid = mob.getUuid();
        long currentTime = System.currentTimeMillis();
        
        Map<UUID, Long> mobAlliances = allianceTimestamps.get(mobUuid);
        if (mobAlliances == null) return;
        
        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : mobAlliances.entrySet()) {
            if (currentTime - entry.getValue() > ALLIANCE_DURATION_MS) {
                toRemove.add(entry.getKey());
            }
        }
        
        // Remove expired alliances
        for (UUID allyUuid : toRemove) {
            mobData.removeAlly(allyUuid);
            mobAlliances.remove(allyUuid);
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
        
        // Clean up timestamps
        UUID uuid1 = mob1.getUuid();
        UUID uuid2 = mob2.getUuid();
        
        Map<UUID, Long> alliances1 = allianceTimestamps.get(uuid1);
        if (alliances1 != null) alliances1.remove(uuid2);
        
        Map<UUID, Long> alliances2 = allianceTimestamps.get(uuid2);
        if (alliances2 != null) alliances2.remove(uuid1);
    }
}

