package mod.universalmobwar.system;

import mod.universalmobwar.config.MobConfig;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.Map;

/**
 * CLEAN EVOLUTION SYSTEM
 * Uses individual mob JSON configs from mob_configs/
 * 
 * Trigger: On ANY spawning, if at least 1 day since last attempt (or first spawn)
 */
public class EvolutionSystem {

    /**
     * Called when mob spawns - calculates points and applies upgrades
     */
    public static void onMobSpawn(MobEntity mob, ServerWorld world) {
        if (!ModConfig.getInstance().scalingEnabled) return;
        
        // Get mob config
        String mobName = ArchetypeClassifier.getMobName(mob);
        MobConfig config = MobConfig.load(mobName);
        if (config == null) return; // Mob not configured
        
        // Get mob data
        MobWarData data = MobWarData.get(mob);
        PowerProfile profile = data.getPowerProfile();
        if (profile == null) {
            profile = new PowerProfile();
        }
        
        // Check upgrade trigger: at least 1 day since last attempt
        long currentDay = world.getTimeOfDay() / 24000L;
        long lastUpgradeDay = profile.lastUpdateTick / 24000L;
        boolean shouldUpgrade = (profile.lastUpdateTick == 0) || (currentDay > lastUpgradeDay);
        
        if (!shouldUpgrade) {
            // Not time yet, just apply current state
            UpgradeSystem.applyUpgrades(mob, profile);
            return;
        }
        
        // Calculate total points
        double dayPoints = calculateDayPoints(config, currentDay);
        
        // Get kill scaling from point system
        double killScaling = 1.0; // Default
        if (config.pointSystem.containsKey("kill_scaling")) {
            Object killScalingObj = config.pointSystem.get("kill_scaling");
            if (killScalingObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> killScalingMap = (Map<String, Object>) killScalingObj;
                if (killScalingMap.containsKey("points_per_player_kill")) {
                    Object pointsPerKill = killScalingMap.get("points_per_player_kill");
                    if (pointsPerKill instanceof Number) {
                        killScaling = ((Number) pointsPerKill).doubleValue();
                    }
                }
            }
        }
        
        double killPoints = data.getKillCount() * killScaling;
        
        dayPoints *= ModConfig.getInstance().dayScalingMultiplier;
        killPoints *= ModConfig.getInstance().killScalingMultiplier;
        
        double totalPoints = dayPoints + killPoints;
        
        // Initialize base stats if first time
        if (profile.baseHealth == 0) {
            profile.baseHealth = mob.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
            var attackAttr = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            profile.baseDamage = (attackAttr != null) ? attackAttr.getBaseValue() : 0;
        }
        
        // Update profile
        profile.totalPoints = totalPoints;
        profile.lastUpdateTick = world.getTime();
        data.setSkillPoints(totalPoints);
        
        // Queue for batched upgrade processing (uses multithreading, completes in 5 seconds)
        if (ModConfig.getInstance().enableBatching && ModConfig.getInstance().enableAsyncTasks) {
            BatchedUpgradeProcessor.queueMobForUpgrade(mob, world, data, profile);
        } else {
            // Fallback: immediate processing (old behavior)
            UpgradeSystem.applyUpgrades(mob, profile);
            data.setSkillData(profile.writeNbt());
            MobWarData.save(mob, data);
        }
    }

    /**
     * Called when mob kills a player - adds kill points
     */
    public static void onMobKill(MobEntity killer, LivingEntity victim) {
        if (!(victim instanceof PlayerEntity)) return; // Only player kills count
        
        MobWarData data = MobWarData.get(killer);
        data.addKill();
        MobWarData.save(killer, data);
        
        // Trigger re-evaluation immediately
        if (killer.getWorld() instanceof ServerWorld serverWorld) {
            onMobSpawn(killer, serverWorld);
        }
    }
    
    /**
     * Calculate day-based points from mob's JSON config
     * Uses daily_scaling_map with explicit day keys
     */
    private static double calculateDayPoints(MobConfig config, long currentDay) {
        double totalPoints = 0.0;
        
        Object dailyMapObj = config.pointSystem.get("daily_scaling_map");
        if (!(dailyMapObj instanceof Map)) return 0.0;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> dailyMap = (Map<String, Object>) dailyMapObj;
        
        for (long day = 0; day <= currentDay; day++) {
            String key = (day >= 31) ? "31+" : String.valueOf(day);
            
            if (dailyMap.containsKey(key)) {
                Object pointsObj = dailyMap.get(key);
                if (pointsObj instanceof Number) {
                    totalPoints += ((Number) pointsObj).doubleValue();
                }
            } else {
                // Fallback to 31+ if day not found
                Object fallbackObj = dailyMap.get("31+");
                if (fallbackObj instanceof Number) {
                    totalPoints += ((Number) fallbackObj).doubleValue();
                }
            }
        }
        
        return totalPoints;
    }
    
    public static void cleanup() {
        // No-op, MobWarData handles cleanup
    }
}
