package mod.universalmobwar.system;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WARLORD SYSTEM - Independent Module
 * 
 * Central system for managing Mob Warlord bosses and their minion armies.
 * Handles minion tracking, spawn configuration, and warlord-related utilities.
 * 
 * This system works independently and can be enabled/disabled via:
 * - Config: warlordEnabled
 * 
 * Configurable options:
 * - warlordSpawnChance (default 25%)
 * - warlordMinRaidLevel (default 3)
 * - warlordMinionCount (default 20)
 * - warlordHealthMultiplierPercent (default 300%)
 * - warlordDamageMultiplierPercent (default 200%)
 * 
 * Does NOT depend on: Targeting, Alliance, or Scaling systems
 * 
 * To use this system:
 * 1. Call isEnabled() to check if warlord features should be active
 * 2. Use minion tracking methods to manage warlord armies
 * 3. Use config getters for warlord-specific settings
 */
public class WarlordSystem {

    // ==========================================================================
    //                              MINION TRACKING
    // ==========================================================================
    
    // Static map to track which mobs are minions of which warlord (thread-safe)
    private static final Map<UUID, UUID> MINION_TO_WARLORD = new ConcurrentHashMap<>();
    
    // Track betrayers - minions that attacked other minions (warlord UUID -> set of betrayer UUIDs)
    private static final Map<UUID, Set<UUID>> WARLORD_BETRAYERS = new ConcurrentHashMap<>();
    
    // ==========================================================================
    //                           STATUS METHODS
    // ==========================================================================
    
    /**
     * Check if warlord system is enabled
     */
    public static boolean isEnabled() {
        return ModConfig.getInstance().isWarlordActive();
    }
    
    /**
     * Check if warlord system is enabled (with world context for future gamerule support)
     */
    public static boolean isEnabled(ServerWorld world) {
        return ModConfig.getInstance().isWarlordActive();
    }
    
    // ==========================================================================
    //                           CONFIG GETTERS
    // ==========================================================================
    
    /**
     * Get max minions per warlord from config
     */
    public static int getMaxMinions() {
        return ModConfig.getInstance().warlordMinionCount;
    }
    
    /**
     * Get warlord spawn chance during raids (0.0 to 1.0)
     */
    public static double getSpawnChance() {
        return ModConfig.getInstance().warlordSpawnChance / 100.0;
    }
    
    /**
     * Get minimum raid level for warlord spawns
     */
    public static int getMinRaidLevel() {
        return ModConfig.getInstance().warlordMinRaidLevel;
    }
    
    /**
     * Get health multiplier percent (e.g., 300 = 3x health)
     */
    public static int getHealthMultiplier() {
        return ModConfig.getInstance().warlordHealthMultiplierPercent;
    }
    
    /**
     * Get damage multiplier percent (e.g., 200 = 2x damage)
     */
    public static int getDamageMultiplier() {
        return ModConfig.getInstance().warlordDamageMultiplierPercent;
    }
    
    /**
     * Check if particles are disabled (performance optimization)
     */
    public static boolean areParticlesDisabled() {
        return ModConfig.getInstance().disableParticles;
    }
    
    // ==========================================================================
    //                           MINION MANAGEMENT
    // ==========================================================================
    
    /**
     * Register a mob as a minion of a warlord
     * @param minionUuid The minion's UUID
     * @param warlordUuid The warlord's UUID
     */
    public static void registerMinion(UUID minionUuid, UUID warlordUuid) {
        MINION_TO_WARLORD.put(minionUuid, warlordUuid);
    }
    
    /**
     * Unregister a minion
     * @param minionUuid The minion's UUID to unregister
     */
    public static void unregisterMinion(UUID minionUuid) {
        UUID warlordUuid = MINION_TO_WARLORD.remove(minionUuid);
        // Also remove from betrayers if applicable
        if (warlordUuid != null) {
            Set<UUID> betrayers = WARLORD_BETRAYERS.get(warlordUuid);
            if (betrayers != null) {
                betrayers.remove(minionUuid);
            }
        }
    }
    
    /**
     * Check if a mob is a minion of any warlord
     * @param minionUuid The mob's UUID
     * @return true if the mob is a minion
     */
    public static boolean isMinion(UUID minionUuid) {
        return MINION_TO_WARLORD.containsKey(minionUuid);
    }
    
    /**
     * Check if a mob entity is a minion of any warlord
     * @param mob The mob entity
     * @return true if the mob is a minion
     */
    public static boolean isMinion(MobEntity mob) {
        return isMinion(mob.getUuid());
    }
    
    /**
     * Get the warlord UUID for a minion
     * @param minionUuid The minion's UUID
     * @return The warlord's UUID, or null if not a minion
     */
    public static UUID getMasterUuid(UUID minionUuid) {
        return MINION_TO_WARLORD.get(minionUuid);
    }
    
    /**
     * Check if two mobs share the same warlord master (are fellow minions)
     * @param mob1Uuid First mob's UUID
     * @param mob2Uuid Second mob's UUID
     * @return true if both are minions of the same warlord
     */
    public static boolean areFellowMinions(UUID mob1Uuid, UUID mob2Uuid) {
        UUID master1 = getMasterUuid(mob1Uuid);
        UUID master2 = getMasterUuid(mob2Uuid);
        return master1 != null && master1.equals(master2);
    }
    
    /**
     * Check if a mob is a minion of a specific warlord
     * @param minionUuid The minion's UUID
     * @param warlordUuid The warlord's UUID to check against
     * @return true if the mob is a minion of the specified warlord
     */
    public static boolean isMinionOf(UUID minionUuid, UUID warlordUuid) {
        UUID masterUuid = getMasterUuid(minionUuid);
        return masterUuid != null && masterUuid.equals(warlordUuid);
    }
    
    // ==========================================================================
    //                           BETRAYER TRACKING
    // ==========================================================================
    
    /**
     * Mark a minion as a betrayer (attacked other minions)
     * @param minionUuid The betraying minion's UUID
     * @param warlordUuid The warlord's UUID
     */
    public static void markBetrayer(UUID minionUuid, UUID warlordUuid) {
        // Remove from minion tracking
        MINION_TO_WARLORD.remove(minionUuid);
        
        // Add to betrayers list for this warlord
        WARLORD_BETRAYERS.computeIfAbsent(warlordUuid, k -> ConcurrentHashMap.newKeySet()).add(minionUuid);
    }
    
    /**
     * Check if a minion is a betrayer of a specific warlord
     * @param minionUuid The minion's UUID
     * @param warlordUuid The warlord's UUID
     * @return true if the minion has betrayed this warlord
     */
    public static boolean isBetrayer(UUID minionUuid, UUID warlordUuid) {
        Set<UUID> betrayers = WARLORD_BETRAYERS.get(warlordUuid);
        return betrayers != null && betrayers.contains(minionUuid);
    }
    
    // ==========================================================================
    //                           CLEANUP METHODS
    // ==========================================================================
    
    /**
     * Clean up all minions for a warlord (call when warlord dies)
     * @param warlordUuid The warlord's UUID
     */
    public static void clearWarlordMinions(UUID warlordUuid) {
        // Remove all minions belonging to this warlord
        MINION_TO_WARLORD.entrySet().removeIf(entry -> entry.getValue().equals(warlordUuid));
        
        // Clear betrayers for this warlord
        WARLORD_BETRAYERS.remove(warlordUuid);
    }
    
    /**
     * Get the count of minions for a warlord
     * @param warlordUuid The warlord's UUID
     * @return Number of registered minions
     */
    public static int getMinionCount(UUID warlordUuid) {
        return (int) MINION_TO_WARLORD.values().stream()
            .filter(masterUuid -> masterUuid.equals(warlordUuid))
            .count();
    }
    
    /**
     * Get all minion UUIDs for a warlord
     * @param warlordUuid The warlord's UUID
     * @return Set of minion UUIDs (copy, safe to modify)
     */
    public static Set<UUID> getMinions(UUID warlordUuid) {
        Set<UUID> minions = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : MINION_TO_WARLORD.entrySet()) {
            if (entry.getValue().equals(warlordUuid)) {
                minions.add(entry.getKey());
            }
        }
        return minions;
    }
    
    // ==========================================================================
    //                           TARGETING HELPERS
    // ==========================================================================
    
    /**
     * Check if a mob should be protected from targeting by minions
     * (Used by targeting system to prevent minions from attacking their master or fellow minions)
     * @param attackerUuid The attacking mob's UUID
     * @param targetUuid The target mob's UUID
     * @return true if the target should be protected
     */
    public static boolean isProtectedFromMinion(UUID attackerUuid, UUID targetUuid) {
        UUID attackerMaster = getMasterUuid(attackerUuid);
        if (attackerMaster == null) return false; // Attacker isn't a minion
        
        // Check if target is the warlord master
        if (targetUuid.equals(attackerMaster)) return true;
        
        // Check if target is a fellow minion
        return isMinionOf(targetUuid, attackerMaster);
    }
    
    /**
     * Validate and clean a target for a minion mob
     * @param attackerMob The attacking minion
     * @param targetMob The potential target
     * @return true if the target is valid (not protected)
     */
    public static boolean isValidMinionTarget(MobEntity attackerMob, MobEntity targetMob) {
        if (!isMinion(attackerMob)) return true; // Non-minions can target anything
        
        return !isProtectedFromMinion(attackerMob.getUuid(), targetMob.getUuid());
    }
}
