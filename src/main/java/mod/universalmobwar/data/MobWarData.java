package mod.universalmobwar.data;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

/**
 * Tracks mob evolution data, alliance relationships, and combat statistics.
 * This data is stored per-mob using entity NBT data.
 */
public class MobWarData {
    
    // Evolution system
    private int killCount = 0;
    private int level = 0;
    private static final int MAX_LEVEL = 100;
    private static final int KILLS_PER_LEVEL = 3;
    
    // Alliance system - stores UUIDs of allies
    private final Set<UUID> allies = new HashSet<>();
    private UUID currentTarget = null;
    private long lastTargetChangeTime = 0;
    
    // Combat stats
    private double damageDealt = 0;
    private double damageTaken = 0;

    // Skill Tree Data
    private double skillPoints = 0;
    private double spentPoints = 0;
    private NbtCompound skillData = new NbtCompound();

    public MobWarData() {}

    // Skill Tree Methods
    public double getSkillPoints() { return skillPoints; }
    public void setSkillPoints(double points) { this.skillPoints = points; }
    public double getSpentPoints() { return spentPoints; }
    public void setSpentPoints(double points) { this.spentPoints = points; }
    public NbtCompound getSkillData() { return skillData; }
    public void setSkillData(NbtCompound data) { this.skillData = data; }
    
    // Evolution methods
    public void addKill() {
        killCount++;
        updateLevel();
    }
    
    private void updateLevel() {
        int newLevel = Math.min(killCount / KILLS_PER_LEVEL, MAX_LEVEL);
        if (newLevel > level) {
            level = newLevel;
        }
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getKillCount() {
        return killCount;
    }
    
    public static int getMaxLevel() {
        return MAX_LEVEL;
    }
    
    // Stat bonuses based on level
    public double getHealthBonus() {
        return level * 0.5; // +0.5 hearts per level
    }
    
    public double getDamageBonus() {
        return level * 0.1; // +10% damage per level
    }
    
    public double getSpeedBonus() {
        return level * 0.005; // +0.5% speed per level
    }
    
    public double getArmorBonus() {
        return level * 0.1; // +0.1 armor per level
    }
    
    public double getKnockbackResistanceBonus() {
        return Math.min(level * 0.01, 1.0); // +1% per level, max 100%
    }
    
    // Alliance methods
    public void addAlly(UUID uuid) {
        allies.add(uuid);
    }
    
    public void removeAlly(UUID uuid) {
        allies.remove(uuid);
    }
    
    public boolean isAlly(UUID uuid) {
        return allies.contains(uuid);
    }
    
    public Set<UUID> getAllies() {
        return new HashSet<>(allies);
    }
    
    public void clearAllies() {
        allies.clear();
    }
    
    // Target tracking
    public void setCurrentTarget(UUID target) {
        if (currentTarget == null || !currentTarget.equals(target)) {
            currentTarget = target;
            lastTargetChangeTime = System.currentTimeMillis();
        }
    }
    
    public UUID getCurrentTarget() {
        return currentTarget;
    }
    
    public long getTimeSinceTargetChange() {
        return System.currentTimeMillis() - lastTargetChangeTime;
    }
    
    public void clearTarget() {
        currentTarget = null;
    }
    
    // Combat stats
    public void addDamageDealt(double damage) {
        damageDealt += damage;
    }
    
    public void addDamageTaken(double damage) {
        damageTaken += damage;
    }
    
    public double getDamageDealt() {
        return damageDealt;
    }
    
    public double getDamageTaken() {
        return damageTaken;
    }
    
    // NBT serialization
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putInt("killCount", killCount);
        nbt.putInt("level", level);
        nbt.putDouble("damageDealt", damageDealt);
        nbt.putDouble("damageTaken", damageTaken);
        
        // Skill Tree Persistence
        nbt.putDouble("skillPoints", skillPoints);
        nbt.putDouble("spentPoints", spentPoints);
        nbt.put("skillData", skillData);
        
        if (currentTarget != null) {
            nbt.putUuid("currentTarget", currentTarget);
            nbt.putLong("lastTargetChangeTime", lastTargetChangeTime);
        }
        
        // Store allies as long array (2 longs per UUID)
        if (!allies.isEmpty()) {
            long[] allyArray = new long[allies.size() * 2];
            int i = 0;
            for (UUID uuid : allies) {
                allyArray[i++] = uuid.getMostSignificantBits();
                allyArray[i++] = uuid.getLeastSignificantBits();
            }
            nbt.putLongArray("allies", allyArray);
        }
        
        return nbt;
    }
    
    public void readNbt(NbtCompound nbt) {
        killCount = nbt.getInt("killCount");
        level = nbt.getInt("level");
        damageDealt = nbt.getDouble("damageDealt");
        damageTaken = nbt.getDouble("damageTaken");
        
        // Skill Tree Persistence
        if (nbt.contains("skillPoints")) skillPoints = nbt.getDouble("skillPoints");
        if (nbt.contains("spentPoints")) spentPoints = nbt.getDouble("spentPoints");
        if (nbt.contains("skillData")) skillData = nbt.getCompound("skillData");
        
        if (nbt.containsUuid("currentTarget")) {
            currentTarget = nbt.getUuid("currentTarget");
            lastTargetChangeTime = nbt.getLong("lastTargetChangeTime");
        }
        
        if (nbt.contains("allies")) {
            long[] allyArray = nbt.getLongArray("allies");
            allies.clear();
            for (int i = 0; i < allyArray.length; i += 2) {
                UUID uuid = new UUID(allyArray[i], allyArray[i + 1]);
                allies.add(uuid);
            }
        }
    }
    
    // Static helper to get or create data for a mob
    private static final String NBT_KEY = "UniversalMobWarData";
    
    public static MobWarData get(MobEntity mob) {
        NbtCompound nbt = mob.writeNbt(new NbtCompound());
        MobWarData data = new MobWarData();
        
        if (nbt.contains(NBT_KEY)) {
            data.readNbt(nbt.getCompound(NBT_KEY));
        }
        
        return data;
    }
    
    private static final Map<UUID, Long> lastSaveTimes = new HashMap<>();
    private static final long SAVE_DEBOUNCE_MS = 200; // Only save once every 200ms per mob

    public static void save(MobEntity mob, MobWarData data) {
        long now = System.currentTimeMillis();
        UUID uuid = mob.getUuid();
        Long lastSave = lastSaveTimes.get(uuid);
        if (lastSave != null && (now - lastSave) < SAVE_DEBOUNCE_MS) {
            return; // Debounce frequent saves
        }
        lastSaveTimes.put(uuid, now);
        NbtCompound nbt = mob.writeNbt(new NbtCompound());
        nbt.put(NBT_KEY, data.writeNbt());
        mob.readNbt(nbt);
    }
}

