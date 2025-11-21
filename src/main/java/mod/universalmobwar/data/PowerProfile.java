package mod.universalmobwar.data;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

/**
 * PowerProfile: Stores all scaling/progression data for a mob (global system).
 * This is separate from MobWarData for modularity and future expansion.
 */
public class PowerProfile {
    // Base stats (copied from entity on spawn)
    public double baseHealth;
    public double baseDamage;
    public double baseSpeed;
    public double baseArmor;
    public double baseKnockbackResist;

    // Scaling points
    public double dayScalingPoints;
    public double killScalingPoints;
    public double totalPoints;

    // Upgrades
    public List<String> chosenUpgrades = new ArrayList<>();
    public String priorityPath = "universal";
    public int tierLevel = 0;

    // Archetype (for modded mob support)
    public String archetype = "unknown";
    public java.util.Set<String> categories = new java.util.HashSet<>();
    public java.util.Map<String, Integer> specialSkills = new java.util.HashMap<>();
    public boolean isMaxed = false;

    // For deterministic behavior
    public long lastUpdateTick = 0;

    public PowerProfile() {}

    /**
     * Serializes this PowerProfile to NBT for persistent storage.
     */
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble("baseHealth", baseHealth);
        nbt.putDouble("baseDamage", baseDamage);
        nbt.putDouble("baseSpeed", baseSpeed);
        nbt.putDouble("baseArmor", baseArmor);
        nbt.putDouble("baseKnockbackResist", baseKnockbackResist);
        nbt.putDouble("dayScalingPoints", dayScalingPoints);
        nbt.putDouble("killScalingPoints", killScalingPoints);
        nbt.putDouble("totalPoints", totalPoints);
        nbt.putInt("tierLevel", tierLevel);
        nbt.putString("priorityPath", priorityPath);
        nbt.putString("archetype", archetype);
        nbt.putLong("lastUpdateTick", lastUpdateTick);
        nbt.putBoolean("isMaxed", isMaxed);
        NbtList upgrades = new NbtList();
        for (String id : chosenUpgrades) {
            upgrades.add(net.minecraft.nbt.NbtString.of(id));
        }
        nbt.put("chosenUpgrades", upgrades);
        
        NbtList cats = new NbtList();
        for (String c : categories) {
            cats.add(net.minecraft.nbt.NbtString.of(c));
        }
        nbt.put("categories", cats);
        
        NbtCompound skills = new NbtCompound();
        for (java.util.Map.Entry<String, Integer> entry : specialSkills.entrySet()) {
            skills.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("specialSkills", skills);
        
        return nbt;
    }

    /**
     * Loads a PowerProfile from NBT.
     */
    public static PowerProfile fromNbt(NbtCompound nbt) {
        PowerProfile profile = new PowerProfile();
        profile.baseHealth = nbt.getDouble("baseHealth");
        profile.baseDamage = nbt.getDouble("baseDamage");
        profile.baseSpeed = nbt.getDouble("baseSpeed");
        profile.baseArmor = nbt.getDouble("baseArmor");
        profile.baseKnockbackResist = nbt.getDouble("baseKnockbackResist");
        profile.dayScalingPoints = nbt.getDouble("dayScalingPoints");
        profile.killScalingPoints = nbt.getDouble("killScalingPoints");
        profile.totalPoints = nbt.getDouble("totalPoints");
        profile.tierLevel = nbt.getInt("tierLevel");
        profile.priorityPath = nbt.getString("priorityPath");
        profile.archetype = nbt.getString("archetype");
        profile.lastUpdateTick = nbt.getLong("lastUpdateTick");
        profile.isMaxed = nbt.getBoolean("isMaxed");
        profile.chosenUpgrades.clear();
        if (nbt.contains("chosenUpgrades", 9)) { // 9 = NbtList
            NbtList upgrades = nbt.getList("chosenUpgrades", 8); // 8 = NbtString
            for (int i = 0; i < upgrades.size(); i++) {
                profile.chosenUpgrades.add(upgrades.getString(i));
            }
        }
        profile.categories.clear();
        if (nbt.contains("categories", 9)) {
            NbtList cats = nbt.getList("categories", 8);
            for (int i = 0; i < cats.size(); i++) {
                profile.categories.add(cats.getString(i));
            }
        }
        profile.specialSkills.clear();
        if (nbt.contains("specialSkills", 10)) { // 10 = NbtCompound
            NbtCompound skills = nbt.getCompound("specialSkills");
            for (String key : skills.getKeys()) {
                profile.specialSkills.put(key, skills.getInt(key));
            }
        }
        return profile;
    }
}
