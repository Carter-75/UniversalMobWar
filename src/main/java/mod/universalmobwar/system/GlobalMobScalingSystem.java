package mod.universalmobwar.system;

import mod.universalmobwar.data.PowerProfile;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalMobScalingSystem: Handles world-day and kill-based scaling, upgrades, and tier logic for all mobs.
 * This system is only active if ModConfig.scalingEnabled is true.
 */
public class GlobalMobScalingSystem {
    // In-memory cache for active mob profiles (cleared on chunk unload)
    private static final Map<MobEntity, PowerProfile> ACTIVE_PROFILES = new HashMap<>();

    /**
     * Called when a mob spawns or enters simulation distance.
     */
    public static void onMobActivated(MobEntity mob, ServerWorld world) {
        if (!ModConfig.getInstance().scalingEnabled) return;
        if (ACTIVE_PROFILES.containsKey(mob)) return;
        PowerProfile profile = new PowerProfile();
        // Copy base stats
        profile.baseHealth = mob.getMaxHealth();
        profile.baseDamage = getBaseDamage(mob);
        profile.baseSpeed = mob.getMovementSpeed();
        profile.baseArmor = mob.getArmor();
        profile.baseKnockbackResist = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) != null ?
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).getBaseValue() : 0.0;
        // Calculate scaling points
        profile.dayScalingPoints = calculateDayScalingPoints(world);
        profile.killScalingPoints = 0;
        profile.totalPoints = profile.dayScalingPoints;
        profile.tierLevel = 0;
        profile.priorityPath = detectPriorityPath(mob);
        profile.archetype = detectArchetype(mob);
        ACTIVE_PROFILES.put(mob, profile);
        updateTierAndUpgrades(mob, profile);
    }

    /**
     * Called when a mob gets a kill or assist.
     */
    public static void onMobKill(MobEntity mob, LivingEntity victim) {
        if (!ModConfig.getInstance().scalingEnabled) return;
        PowerProfile profile = ACTIVE_PROFILES.get(mob);
        if (profile == null) return;
        double points = calculateKillPoints(mob, victim, profile);
        profile.killScalingPoints += points;
        profile.totalPoints = profile.dayScalingPoints + profile.killScalingPoints;
        updateTierAndUpgrades(mob, profile);
    }

    /**
     * Calculate day-based scaling points using a soft exponential curve.
     */
    private static double calculateDayScalingPoints(ServerWorld world) {
        long day = world.getTimeOfDay() / 24000L;
        double base = ModConfig.getInstance().dayScalingMultiplier;
        // Example: soft exponential (tweak as needed)
        return base * Math.pow(day, 1.18);
    }

    /**
     * Calculate kill-based scaling points (progressive, harder per tier).
     */
    private static double calculateKillPoints(MobEntity mob, LivingEntity victim, PowerProfile profile) {
        double base = ModConfig.getInstance().killScalingMultiplier;
        int tier = profile.tierLevel;
        // Example: each tier requires 2x more points
        return base / Math.pow(2, tier);
    }

    /**
     * Update tier and buy upgrades based on total points.
     */
    private static void updateTierAndUpgrades(MobEntity mob, PowerProfile profile) {
        int newTier = calculateTier(profile.totalPoints);
        if (newTier > profile.tierLevel) {
            profile.tierLevel = newTier;
            // Buy upgrades for new tier
            buyUpgrades(mob, profile, newTier);
        }
    }

    /**
     * Calculate tier from total points (nonlinear, hidden from player).
     */
    private static int calculateTier(double totalPoints) {
        // Example: nonlinear tier curve
        if (totalPoints < 10) return 0;
        if (totalPoints < 30) return 1;
        if (totalPoints < 80) return 2;
        if (totalPoints < 200) return 3;
        if (totalPoints < 500) return 4;
        if (totalPoints < 1200) return 5;
        if (totalPoints < 3000) return 6;
        if (totalPoints < 7000) return 7;
        if (totalPoints < 15000) return 8;
        if (totalPoints < 30000) return 9;
        return Math.min((int)(Math.log10(totalPoints) * 2), ModConfig.getInstance().maxTier);
    }

    /**
     * Buy upgrades for the mob at the given tier.
     */
    private static void buyUpgrades(MobEntity mob, PowerProfile profile, int tier) {
        // Remove old modifiers first (to avoid stacking)
        var health = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        var damage = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        var speed = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
        var armor = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR);
        var knockback = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);



        net.minecraft.util.Identifier HEALTH_MODIFIER_ID = net.minecraft.util.Identifier.of("universalmobwar", "scaling_health");
        net.minecraft.util.Identifier DAMAGE_MODIFIER_ID = net.minecraft.util.Identifier.of("universalmobwar", "scaling_damage");
        net.minecraft.util.Identifier SPEED_MODIFIER_ID = net.minecraft.util.Identifier.of("universalmobwar", "scaling_speed");
        net.minecraft.util.Identifier ARMOR_MODIFIER_ID = net.minecraft.util.Identifier.of("universalmobwar", "scaling_armor");
        net.minecraft.util.Identifier KNOCKBACK_MODIFIER_ID = net.minecraft.util.Identifier.of("universalmobwar", "scaling_knockback");

        if (health != null) health.removeModifier(HEALTH_MODIFIER_ID);
        if (damage != null) damage.removeModifier(DAMAGE_MODIFIER_ID);
        if (speed != null) speed.removeModifier(SPEED_MODIFIER_ID);
        if (armor != null) armor.removeModifier(ARMOR_MODIFIER_ID);
        if (knockback != null) knockback.removeModifier(KNOCKBACK_MODIFIER_ID);

        // Calculate bonuses by tier
        double healthBonus = profile.baseHealth * 0.10 * tier;
        double damageBonus = profile.baseDamage * 0.10 * tier;
        double speedBonus = profile.baseSpeed * 0.05 * tier;
        double armorBonus = 2.0 * tier;
        double knockbackBonus = 0.10 * tier;

        if (health != null && healthBonus > 0) {
            health.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                HEALTH_MODIFIER_ID,
                healthBonus,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
            mob.setHealth(mob.getMaxHealth());
        }
        if (damage != null && damageBonus > 0) {
            damage.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                DAMAGE_MODIFIER_ID,
                damageBonus,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (speed != null && speedBonus > 0) {
            speed.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                SPEED_MODIFIER_ID,
                speedBonus,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (armor != null && armorBonus > 0) {
            armor.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                ARMOR_MODIFIER_ID,
                armorBonus,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (knockback != null && knockbackBonus > 0) {
            knockback.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
                KNOCKBACK_MODIFIER_ID,
                knockbackBonus,
                net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
        }

        // Equipment progression (similar to evolution system)
        if (tier >= 2 && mob.getEquippedStack(net.minecraft.entity.EquipmentSlot.MAINHAND).isEmpty()) {
            net.minecraft.item.ItemStack weapon;
            if (tier >= 10) weapon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_SWORD);
            else if (tier >= 8) weapon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_SWORD);
            else if (tier >= 6) weapon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_SWORD);
            else if (tier >= 4) weapon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.STONE_SWORD);
            else weapon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.WOODEN_SWORD);
            mob.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, weapon);
            mob.setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0.1f);
        }
        if (tier >= 4) {
            net.minecraft.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
            };
            for (net.minecraft.entity.EquipmentSlot slot : armorSlots) {
                if (mob.getEquippedStack(slot).isEmpty()) {
                    net.minecraft.item.ItemStack armorItem = null;
                    if (tier >= 10) {
                        armorItem = switch (slot) {
                            case HEAD -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_HELMET);
                            case CHEST -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_CHESTPLATE);
                            case LEGS -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_LEGGINGS);
                            case FEET -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_BOOTS);
                            default -> null;
                        };
                    } else if (tier >= 8) {
                        armorItem = switch (slot) {
                            case HEAD -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_HELMET);
                            case CHEST -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_CHESTPLATE);
                            case LEGS -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_LEGGINGS);
                            case FEET -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_BOOTS);
                            default -> null;
                        };
                    } else if (tier >= 6) {
                        armorItem = switch (slot) {
                            case HEAD -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_HELMET);
                            case CHEST -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_CHESTPLATE);
                            case LEGS -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_LEGGINGS);
                            case FEET -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_BOOTS);
                            default -> null;
                        };
                    } else if (tier >= 4) {
                        armorItem = switch (slot) {
                            case HEAD -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_HELMET);
                            case CHEST -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_CHESTPLATE);
                            case LEGS -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_LEGGINGS);
                            case FEET -> new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_BOOTS);
                            default -> null;
                        };
                    }
                    if (armorItem != null) {
                        mob.equipStack(slot, armorItem);
                        mob.setEquipmentDropChance(slot, 0.1f);
                    }
                }
            }
        }
        // TODO: Add special effects for high tiers/archetypes if desired
    }

    /**
     * Detect mob archetype for modded mob support.
     */
    private static String detectArchetype(MobEntity mob) {
        // Use entity type, tags, attributes, etc. to classify
        // (Implement full detection logic as needed)
        return "melee";
    }

    /**
     * Detect priority path for upgrade tree.
     */
    private static String detectPriorityPath(MobEntity mob) {
        // Use mob type, NBT, or random for now
        // (Implement full detection logic as needed)
        return "universal";
    }

    /**
     * Get base damage for a mob (attribute or fallback).
     */
    private static double getBaseDamage(MobEntity mob) {
        if (mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            return mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
        }
        return 2.0;
    }
}
