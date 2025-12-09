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
     * Apply weapon/armor based on config and levels - fully data-driven from mob JSON
     */
    private static void applyEquipment(MobEntity mob, MobConfig config, Map<String, Integer> levels) {
        // Apply weapon based on mob's weapon type from JSON
        applyWeapon(mob, config, levels);
        
        // Shield (if mob has shield enabled in JSON)
        if (config.shield && levels.getOrDefault("shield_tier", 0) > 0) {
            mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
        
        // Armor - full tier progression based on purchased levels
        if (!config.armor.equals("none")) {
            applyArmorTiers(mob, config, levels);
        }
    }
    
    /**
     * Apply weapon based on mob's weapon type and purchased tier
     */
    private static void applyWeapon(MobEntity mob, MobConfig config, Map<String, Integer> levels) {
        String weaponType = config.weapon.toLowerCase();
        int weaponTier = levels.getOrDefault("weapon_tier", config.startsWithWeapon ? 1 : 0);
        
        if (weaponTier <= 0) return; // No weapon
        
        ItemStack weapon = null;
        
        // Ranged weapons (always start with)
        if (weaponType.contains("bow")) {
            weapon = new ItemStack(Items.BOW);
        } else if (weaponType.contains("crossbow")) {
            weapon = new ItemStack(Items.CROSSBOW);
        } else if (weaponType.contains("trident")) {
            weapon = new ItemStack(Items.TRIDENT);
            
        // Axes (for Piglins/Vindicators)
        } else if (weaponType.contains("gold_axe") || weaponType.contains("golden_axe")) {
            // Gold axes: golden -> netherite
            weapon = weaponTier >= 2 ? new ItemStack(Items.NETHERITE_AXE) : new ItemStack(Items.GOLDEN_AXE);
        } else if (weaponType.contains("iron_axe")) {
            // Regular axes: iron -> diamond -> netherite
            if (weaponTier >= 3) weapon = new ItemStack(Items.NETHERITE_AXE);
            else if (weaponTier == 2) weapon = new ItemStack(Items.DIAMOND_AXE);
            else weapon = new ItemStack(Items.IRON_AXE);
            
        // Swords (most common)
        } else if (weaponType.contains("gold_sword") || weaponType.contains("golden_sword")) {
            // Gold swords: golden -> netherite
            weapon = weaponTier >= 2 ? new ItemStack(Items.NETHERITE_SWORD) : new ItemStack(Items.GOLDEN_SWORD);
        } else if (weaponType.contains("stone_sword")) {
            // Stone sword progression: stone -> iron -> diamond -> netherite
            if (weaponTier >= 4) weapon = new ItemStack(Items.NETHERITE_SWORD);
            else if (weaponTier == 3) weapon = new ItemStack(Items.DIAMOND_SWORD);
            else if (weaponTier == 2) weapon = new ItemStack(Items.IRON_SWORD);
            else weapon = new ItemStack(Items.STONE_SWORD);
        } else if (weaponType.contains("normal_sword") || weaponType.contains("sword")) {
            // Normal sword progression: wood -> stone -> iron -> diamond -> netherite
            if (weaponTier >= 5) weapon = new ItemStack(Items.NETHERITE_SWORD);
            else if (weaponTier == 4) weapon = new ItemStack(Items.DIAMOND_SWORD);
            else if (weaponTier == 3) weapon = new ItemStack(Items.IRON_SWORD);
            else if (weaponTier == 2) weapon = new ItemStack(Items.STONE_SWORD);
            else weapon = new ItemStack(Items.WOODEN_SWORD);
        }
        
        if (weapon != null) {
            mob.equipStack(EquipmentSlot.MAINHAND, weapon);
        }
    }
    
    /**
     * Apply armor based on tier progression
     */
    private static void applyArmorTiers(MobEntity mob, MobConfig config, Map<String, Integer> levels) {
        // Determine armor tier from purchased levels
        int helmetLevel = levels.getOrDefault("helmet_tier", 0);
        int chestLevel = levels.getOrDefault("chestplate_tier", 0);
        int legsLevel = levels.getOrDefault("leggings_tier", 0);
        int bootsLevel = levels.getOrDefault("boots_tier", 0);
        
        // Use gold armor for Piglins, regular armor for others
        boolean useGold = config.mobName.equalsIgnoreCase("Piglin") || 
                          config.mobName.equalsIgnoreCase("Piglin_Brute") ||
                          config.mobName.equalsIgnoreCase("Zombified_Piglin");
        
        // Helmet
        if (helmetLevel > 0) {
            ItemStack helmet = getArmorPiece(helmetLevel, "helmet", useGold);
            if (helmet != null) mob.equipStack(EquipmentSlot.HEAD, helmet);
        }
        
        // Chestplate
        if (chestLevel > 0) {
            ItemStack chest = getArmorPiece(chestLevel, "chestplate", useGold);
            if (chest != null) mob.equipStack(EquipmentSlot.CHEST, chest);
        }
        
        // Leggings
        if (legsLevel > 0) {
            ItemStack legs = getArmorPiece(legsLevel, "leggings", useGold);
            if (legs != null) mob.equipStack(EquipmentSlot.LEGS, legs);
        }
        
        // Boots
        if (bootsLevel > 0) {
            ItemStack boots = getArmorPiece(bootsLevel, "boots", useGold);
            if (boots != null) mob.equipStack(EquipmentSlot.FEET, boots);
        }
    }
    
    /**
     * Get armor piece ItemStack based on tier level
     */
    private static ItemStack getArmorPiece(int tier, String type, boolean useGold) {
        if (useGold) {
            // Gold armor progression: gold -> netherite
            switch (tier) {
                case 1: return getGoldArmor(type);
                case 2: return getNetheriteArmor(type);
                default: return getGoldArmor(type);
            }
        } else {
            // Regular armor progression: leather -> chain -> iron -> diamond -> netherite
            switch (tier) {
                case 1: return getLeatherArmor(type);
                case 2: return getChainmailArmor(type);
                case 3: return getIronArmor(type);
                case 4: return getDiamondArmor(type);
                case 5: return getNetheriteArmor(type);
                default: return getLeatherArmor(type);
            }
        }
    }
    
    private static ItemStack getLeatherArmor(String type) {
        switch (type) {
            case "helmet": return new ItemStack(Items.LEATHER_HELMET);
            case "chestplate": return new ItemStack(Items.LEATHER_CHESTPLATE);
            case "leggings": return new ItemStack(Items.LEATHER_LEGGINGS);
            case "boots": return new ItemStack(Items.LEATHER_BOOTS);
            default: return null;
        }
    }
    
    private static ItemStack getChainmailArmor(String type) {
        switch (type) {
            case "helmet": return new ItemStack(Items.CHAINMAIL_HELMET);
            case "chestplate": return new ItemStack(Items.CHAINMAIL_CHESTPLATE);
            case "leggings": return new ItemStack(Items.CHAINMAIL_LEGGINGS);
            case "boots": return new ItemStack(Items.CHAINMAIL_BOOTS);
            default: return null;
        }
    }
    
    private static ItemStack getIronArmor(String type) {
        switch (type) {
            case "helmet": return new ItemStack(Items.IRON_HELMET);
            case "chestplate": return new ItemStack(Items.IRON_CHESTPLATE);
            case "leggings": return new ItemStack(Items.IRON_LEGGINGS);
            case "boots": return new ItemStack(Items.IRON_BOOTS);
            default: return null;
        }
    }
    
    private static ItemStack getDiamondArmor(String type) {
        switch (type) {
            case "helmet": return new ItemStack(Items.DIAMOND_HELMET);
            case "chestplate": return new ItemStack(Items.DIAMOND_CHESTPLATE);
            case "leggings": return new ItemStack(Items.DIAMOND_LEGGINGS);
            case "boots": return new ItemStack(Items.DIAMOND_BOOTS);
            default: return null;
        }
    }
    
    private static ItemStack getNetheriteArmor(String type) {
        switch (type) {
            case "helmet": return new ItemStack(Items.NETHERITE_HELMET);
            case "chestplate": return new ItemStack(Items.NETHERITE_CHESTPLATE);
            case "leggings": return new ItemStack(Items.NETHERITE_LEGGINGS);
            case "boots": return new ItemStack(Items.NETHERITE_BOOTS);
            default: return null;
        }
    }
    
    private static ItemStack getGoldArmor(String type) {
        switch (type) {
            case "helmet": return new ItemStack(Items.GOLDEN_HELMET);
            case "chestplate": return new ItemStack(Items.GOLDEN_CHESTPLATE);
            case "leggings": return new ItemStack(Items.GOLDEN_LEGGINGS);
            case "boots": return new ItemStack(Items.GOLDEN_BOOTS);
            default: return null;
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
