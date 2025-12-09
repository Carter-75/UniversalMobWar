package mod.universalmobwar.system;

import mod.universalmobwar.config.MobConfig;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.PowerProfile;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.*;

/**
 * CLEAN UPGRADE SYSTEM - Uses MobConfig for everything
 * Implements 80%/20% buy/save logic
 * All costs come from individual mob JSON files
 */
public class UpgradeSystem {
    
    // Equipment tier constants from ModConfig
    public static final List<String> SWORD_TIERS = ModConfig.getInstance().swordTiers;
    public static final List<String> GOLD_SWORD_TIERS = ModConfig.getInstance().goldSwordTiers;
    public static final List<String> AXE_TIERS = ModConfig.getInstance().axeTiers;
    public static final List<String> GOLD_AXE_TIERS = ModConfig.getInstance().goldAxeTiers;
    public static final List<String> HELMET_TIERS = ModConfig.getInstance().helmetTiers;
    public static final List<String> CHEST_TIERS = ModConfig.getInstance().chestTiers;
    public static final List<String> LEGS_TIERS = ModConfig.getInstance().legsTiers;
    public static final List<String> BOOTS_TIERS = ModConfig.getInstance().bootsTiers;
    
    /**
     * Apply all upgrades to a mob based on available points
     */
    public static void applyUpgrades(MobEntity mob, PowerProfile profile) {
        String mobName = ArchetypeClassifier.getMobName(mob);
        MobConfig config = MobConfig.load(mobName);
        if (config == null) return;
        
        // Simulate upgrade purchases with 80%/20% logic
        simulateUpgrades(mob, profile, config);
        
        // Apply equipment and stats to mob
        applyToMob(mob, profile, config);
    }
    
    /**
     * Simulate upgrade purchases using 80%/20% buy/save logic
     */
    private static void simulateUpgrades(MobEntity mob, PowerProfile profile, MobConfig config) {
        Random random = new Random(mob.getUuid().getMostSignificantBits());
        double availablePoints = profile.totalPoints;
        
        // Track what we've bought (using profile.specialSkills as storage)
        Map<String, Integer> levels = profile.specialSkills;
        
        int safety = 0;
        while (availablePoints > 0 && safety++ < 10000) {
            // Collect all affordable upgrades
            List<String> affordable = new ArrayList<>();
            List<Integer> costs = new ArrayList<>();
            
            // Universal upgrades
            for (String upgradeName : config.universalUpgrades.keySet()) {
                int currentLevel = levels.getOrDefault(upgradeName, 0);
                int cost = config.getUniversalUpgradeCost(upgradeName, currentLevel);
                if (cost > 0 && cost <= availablePoints) {
                    affordable.add("universal:" + upgradeName);
                    costs.add(cost);
                }
            }
            
            // Skill tree upgrades
            for (String treeId : config.assignedTrees) {
                Map<String, List<MobConfig.UpgradeCost>> tree = config.skillTrees.get(treeId);
                if (tree != null) {
                    for (String skillName : tree.keySet()) {
                        int currentLevel = levels.getOrDefault(skillName, 0);
                        int cost = config.getSkillTreeUpgradeCost(treeId, skillName, currentLevel);
                        if (cost > 0 && cost <= availablePoints) {
                            affordable.add("tree:" + treeId + ":" + skillName);
                            costs.add(cost);
                        }
                    }
                }
            }
            
            if (affordable.isEmpty()) break; // Nothing left to buy
            
            // 20% chance to SAVE and stop
            if (random.nextDouble() < 0.2) {
                break;
            }
            
            // 80% chance to BUY a random affordable upgrade
            int index = random.nextInt(affordable.size());
            String upgrade = affordable.get(index);
            int cost = costs.get(index);
            
            // Parse upgrade type and increment level
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
        
        // Save back to profile
        profile.specialSkills = levels;
    }
    
    /**
     * Apply stats and equipment to mob based on purchased upgrades
     * Public so BatchedUpgradeProcessor can use it
     */
    public static void applyToMob(MobEntity mob, PowerProfile profile, MobConfig config) {
        Map<String, Integer> levels = profile.specialSkills;
        
        // Apply stat boosts
        int healthBoost = levels.getOrDefault("health_boost", 0);
        int strength = levels.getOrDefault("strength", 0);
        int speed = levels.getOrDefault("speed", 0);
        int resistance = levels.getOrDefault("resistance", 0);
        
        // Health
        var healthAttr = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null && healthBoost > 0) {
            healthAttr.setBaseValue(profile.baseHealth + (healthBoost * 4.0));
        }
        
        // Strength
        var damageAttr = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null && strength > 0) {
            damageAttr.setBaseValue(profile.baseDamage + strength);
        }
        
        // Speed
        var speedAttr = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null && speed > 0) {
            double baseSpeed = speedAttr.getBaseValue();
            speedAttr.setBaseValue(baseSpeed * (1.0 + speed * 0.1));
        }
        
        // Resistance (potion effect)
        if (resistance > 0) {
            mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.RESISTANCE,
                999999,
                resistance - 1,
                false,
                false
            ));
        }
        
        // Equipment
        applyEquipment(mob, config, levels);
    }
    
    /**
     * Apply weapon/armor based on config and levels
     */
    private static void applyEquipment(MobEntity mob, MobConfig config, Map<String, Integer> levels) {
        // Weapon
        if (config.startsWithWeapon) {
            // Mob starts with weapon - equip base version
            if (config.weapon.contains("bow")) {
                mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            } else if (config.weapon.contains("crossbow")) {
                mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            } else if (config.weapon.contains("trident")) {
                mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            } else if (config.weapon.contains("iron_axe")) {
                mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            } else if (config.weapon.contains("gold_axe")) {
                mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
            }
        } else {
            // Melee weapons must be purchased
            int swordLevel = levels.getOrDefault("sharpness", 0); // Using sharpness as proxy for "has sword"
            if (swordLevel > 0) {
                if (config.weapon.contains("stone_sword")) {
                    mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                } else if (config.weapon.contains("normal_sword")) {
                    mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
                }
            }
        }
        
        // Shield
        if (config.shield && levels.getOrDefault("shield_chance", 0) > 0) {
            mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        
        // Armor (TODO: implement tier progression)
        // For now, just basic leather if purchased
        int armorLevel = levels.getOrDefault("protection", 0);
        if (armorLevel > 0 && !config.armor.equals("none")) {
            mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            mob.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            mob.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            mob.equipStack(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        }
    }
    
    /**
     * Perform one upgrade step for a mob (used by MobUpgradeTickMixin)
     */
    public static void performOneStep(MobEntity mob, MobWarData data) {
        PowerProfile profile = data.getPowerProfile();
        applyUpgrades(mob, profile);
    }
}
