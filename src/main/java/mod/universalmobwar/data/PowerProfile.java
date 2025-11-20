package mod.universalmobwar.data;

// ...existing code...
import java.util.List;
import java.util.ArrayList;

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
}
