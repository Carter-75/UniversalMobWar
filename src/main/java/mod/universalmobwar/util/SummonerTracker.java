package mod.universalmobwar.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal tracker for summoner-summoned relationships.
 * Prevents summoned mobs from attacking their summoners.
 * Works for ALL mobs (vanilla and modded):
 * - Evokers summoning Vexes
 * - Illusioners summoning duplicates
 * - Mob Warlord summoning minions
 * - Any modded mob that summons others
 */
public class SummonerTracker {
    
    // Maps summoned mob UUID -> summoner mob UUID (thread-safe)
    private static final Map<UUID, UUID> SUMMONED_TO_SUMMONER = new ConcurrentHashMap<>();
    
    /**
     * Registers a mob as being summoned by another mob.
     * @param summonedUuid The UUID of the summoned mob
     * @param summonerUuid The UUID of the summoner mob
     */
    public static void registerSummoned(UUID summonedUuid, UUID summonerUuid) {
        if (summonedUuid != null && summonerUuid != null) {
            SUMMONED_TO_SUMMONER.put(summonedUuid, summonerUuid);
        }
    }
    
    /**
     * Gets the summoner UUID for a summoned mob.
     * @param summonedUuid The UUID of the summoned mob
     * @return The summoner's UUID, or null if not summoned
     */
    public static UUID getSummoner(UUID summonedUuid) {
        return SUMMONED_TO_SUMMONER.get(summonedUuid);
    }
    
    /**
     * Checks if a mob is summoned by another specific mob.
     * @param summonedUuid The UUID of the potentially summoned mob
     * @param summonerUuid The UUID of the potential summoner
     * @return true if the mob was summoned by the specified summoner
     */
    public static boolean isSummonedBy(UUID summonedUuid, UUID summonerUuid) {
        if (summonedUuid == null || summonerUuid == null) return false;
        UUID actualSummoner = SUMMONED_TO_SUMMONER.get(summonedUuid);
        return summonerUuid.equals(actualSummoner);
    }
    
    /**
     * Checks if a mob is summoned (by anyone).
     * @param summonedUuid The UUID of the potentially summoned mob
     * @return true if the mob was summoned
     */
    public static boolean isSummoned(UUID summonedUuid) {
        return SUMMONED_TO_SUMMONER.containsKey(summonedUuid);
    }
    
    /**
     * Removes a mob from tracking (e.g., when it dies).
     * @param summonedUuid The UUID of the summoned mob
     */
    public static void unregisterSummoned(UUID summonedUuid) {
        SUMMONED_TO_SUMMONER.remove(summonedUuid);
    }
    
    /**
     * Clears all tracking data.
     */
    public static void clearAll() {
        SUMMONED_TO_SUMMONER.clear();
    }
    
    /**
     * Gets the total number of summoned mobs currently tracked.
     * @return The number of tracked summoned mobs
     */
    public static int getTrackedCount() {
        return SUMMONED_TO_SUMMONER.size();
    }
}

