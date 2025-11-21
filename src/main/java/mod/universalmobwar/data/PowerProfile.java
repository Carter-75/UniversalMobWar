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
        NbtList upgrades = new NbtList();
        for (String id : chosenUpgrades) {
            upgrades.add(net.minecraft.nbt.NbtString.of(id));
        }
        nbt.put("chosenUpgrades", upgrades);
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
        profile.chosenUpgrades.clear();
        if (nbt.contains("chosenUpgrades", 9)) { // 9 = NbtList
            NbtList upgrades = nbt.getList("chosenUpgrades", 8); // 8 = NbtString
            for (int i = 0; i < upgrades.size(); i++) {
                profile.chosenUpgrades.add(upgrades.getString(i));
            }
        }
        return profile;
    }
}
