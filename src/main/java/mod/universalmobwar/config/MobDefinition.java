package mod.universalmobwar.config;

import java.util.*;

/**
 * Represents a single mob's configuration loaded from skilltree.json
 */
public class MobDefinition {
    public final String name;
    public final String type; // "hostile", "neutral", "passive"
    public final String weapon; // "none", "normal_sword", "bow", "trident", etc.
    public final String armor; // "none", "full_normal", "full_gold"
    public final boolean shield;
    public final List<String> trees; // List of tree IDs: "z", "r", "creeper", etc.
    
    // Derived properties
    public final boolean startsWithWeapon;
    public final boolean startsWithArmor;
    public final WeaponType weaponType;
    public final ArmorType armorType;
    
    public enum WeaponType {
        NONE,
        NORMAL_SWORD,      // Wood -> Stone -> Iron -> Diamond -> Netherite
        STONE_SWORD,       // Stone -> Iron -> Diamond -> Netherite
        GOLD_SWORD,        // Gold -> Netherite
        IRON_AXE,          // Iron -> Diamond -> Netherite
        GOLD_AXE,          // Gold -> Netherite
        BOW,               // Bow (no tiers)
        CROSSBOW,          // Crossbow (no tiers)
        TRIDENT            // Trident (no tiers)
    }
    
    public enum ArmorType {
        NONE,
        FULL_NORMAL,       // Leather -> Chain -> Iron -> Diamond -> Netherite (all 4 pieces)
        FULL_GOLD          // Gold -> Netherite (all 4 pieces)
    }
    
    public MobDefinition(String name, String type, String weapon, String armor, boolean shield, List<String> trees) {
        this.name = name;
        this.type = type;
        this.weapon = weapon;
        this.armor = armor;
        this.shield = shield;
        this.trees = trees != null ? new ArrayList<>(trees) : new ArrayList<>();
        
        // Parse weapon type
        this.weaponType = parseWeaponType(weapon);
        // CRITICAL: ONLY ranged weapons start with weapon per skilltree.txt
        // All melee weapons (swords, axes) must be earned
        this.startsWithWeapon = isRangedWeapon(weaponType);
        
        // Parse armor type
        this.armorType = parseArmorType(armor);
        this.startsWithArmor = false; // All mobs start naked per skilltree
    }
    
    private static WeaponType parseWeaponType(String weapon) {
        if (weapon == null || weapon.equals("none")) return WeaponType.NONE;
        return switch (weapon) {
            case "normal_sword" -> WeaponType.NORMAL_SWORD;
            case "stone_sword" -> WeaponType.STONE_SWORD;
            case "gold_sword", "gold_sword_or_crossbow_50%" -> WeaponType.GOLD_SWORD; // Piglin special case handled elsewhere
            case "iron_axe" -> WeaponType.IRON_AXE;
            case "gold_axe" -> WeaponType.GOLD_AXE;
            case "bow" -> WeaponType.BOW;
            case "crossbow" -> WeaponType.CROSSBOW;
            case "trident" -> WeaponType.TRIDENT;
            default -> WeaponType.NONE;
        };
    }
    
    private static ArmorType parseArmorType(String armor) {
        if (armor == null || armor.equals("none")) return ArmorType.NONE;
        return switch (armor) {
            case "normal" -> ArmorType.FULL_NORMAL;
            case "gold" -> ArmorType.FULL_GOLD;
            default -> ArmorType.NONE;
        };
    }
    
    private static boolean isRangedWeapon(WeaponType type) {
        return type == WeaponType.BOW || type == WeaponType.CROSSBOW || type == WeaponType.TRIDENT;
    }
    
    public boolean hasTree(String treeId) {
        return trees.contains(treeId);
    }
    
    public boolean isHostile() {
        return type.equals("hostile");
    }
    
    public boolean isNeutral() {
        return type.equals("neutral");
    }
    
    public boolean isPassive() {
        return type.equals("passive");
    }
    
    @Override
    public String toString() {
        return String.format("MobDefinition{name='%s', type='%s', weapon=%s, armor=%s, shield=%s, trees=%s}", 
            name, type, weaponType, armorType, shield, trees);
    }
}
