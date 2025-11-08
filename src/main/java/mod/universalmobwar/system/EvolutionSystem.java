package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.UUID;

/**
 * Handles mob evolution and power scaling based on kills.
 * Applies stat bonuses and equipment as mobs level up.
 */
public class EvolutionSystem {
    
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("8e5a3f1c-7d2b-4e9a-9f3c-1a2b3c4d5e6f");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("7d4c2b1a-6e3f-4c8d-9a2b-1f3e5d7c9b1a");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("6c3a1f9e-5d2b-4a7c-8e1f-2d4c6a8b0d2e");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("5b2c9f7e-4a1d-3c6b-7f9e-1c3d5b7a9c1b");
    private static final UUID KNOCKBACK_MODIFIER_UUID = UUID.fromString("4a1e8d6c-3b9f-2d5a-6c8e-9b1d3f5a7c9d");
    
    /**
     * Called when a mob kills another mob. Increases kill count and levels up if needed.
     */
    public static void onMobKill(MobEntity killer, LivingEntity victim) {
        MobWarData data = MobWarData.get(killer);
        int oldLevel = data.getLevel();
        
        data.addKill();
        int newLevel = data.getLevel();
        
        // Apply bonuses if leveled up
        if (newLevel > oldLevel) {
            applyLevelBonuses(killer, data);
            updateEquipment(killer, newLevel);
        }
        
        MobWarData.save(killer, data);
    }
    
    /**
     * Applies stat bonuses to a mob based on their level.
     */
    public static void applyLevelBonuses(MobEntity mob, MobWarData data) {
        int level = data.getLevel();
        
        // Health bonus
        EntityAttributeInstance health = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HEALTH_MODIFIER_UUID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    HEALTH_MODIFIER_UUID,
                    "mob_war_health_bonus",
                    data.getHealthBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                health.addPersistentModifier(modifier);
                mob.setHealth(mob.getMaxHealth()); // Heal to new max
            }
        }
        
        // Damage bonus
        EntityAttributeInstance damage = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(DAMAGE_MODIFIER_UUID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    DAMAGE_MODIFIER_UUID,
                    "mob_war_damage_bonus",
                    data.getDamageBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                damage.addPersistentModifier(modifier);
            }
        }
        
        // Speed bonus
        EntityAttributeInstance speed = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_UUID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    SPEED_MODIFIER_UUID,
                    "mob_war_speed_bonus",
                    data.getSpeedBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                speed.addPersistentModifier(modifier);
            }
        }
        
        // Armor bonus
        EntityAttributeInstance armor = mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_UUID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    ARMOR_MODIFIER_UUID,
                    "mob_war_armor_bonus",
                    data.getArmorBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                armor.addPersistentModifier(modifier);
            }
        }
        
        // Knockback resistance bonus
        EntityAttributeInstance knockback = mob.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(KNOCKBACK_MODIFIER_UUID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    KNOCKBACK_MODIFIER_UUID,
                    "mob_war_knockback_bonus",
                    data.getKnockbackResistanceBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                knockback.addPersistentModifier(modifier);
            }
        }
    }
    
    /**
     * Gives equipment to mobs based on their level.
     */
    private static void updateEquipment(MobEntity mob, int level) {
        // Start giving equipment at level 5
        if (level < 5) return;
        
        // Weapon (every 10 levels, upgrade weapon)
        if (level >= 10 && mob.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
            ItemStack weapon = getWeaponForLevel(level);
            mob.equipStack(EquipmentSlot.MAINHAND, weapon);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.1f);
        }
        
        // Armor progression
        if (level >= 20) {
            giveArmorSet(mob, level);
        }
    }
    
    private static ItemStack getWeaponForLevel(int level) {
        ItemStack weapon;
        
        if (level >= 50) {
            weapon = new ItemStack(Items.NETHERITE_SWORD);
        } else if (level >= 40) {
            weapon = new ItemStack(Items.DIAMOND_SWORD);
        } else if (level >= 30) {
            weapon = new ItemStack(Items.IRON_SWORD);
        } else if (level >= 20) {
            weapon = new ItemStack(Items.STONE_SWORD);
        } else {
            weapon = new ItemStack(Items.WOODEN_SWORD);
        }
        
        // Note: Enchantments can be added in future versions
        // For now, the base weapon provides sufficient power scaling
        
        return weapon;
    }
    
    private static void giveArmorSet(MobEntity mob, int level) {
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };
        
        for (EquipmentSlot slot : armorSlots) {
            if (mob.getEquippedStack(slot).isEmpty()) {
                ItemStack armor = getArmorForLevel(level, slot);
                if (armor != null) {
                    mob.equipStack(slot, armor);
                    mob.setEquipmentDropChance(slot, 0.1f);
                }
            }
        }
    }
    
    private static ItemStack getArmorForLevel(int level, EquipmentSlot slot) {
        ItemStack armor = null;
        
        if (level >= 60) {
            armor = switch (slot) {
                case HEAD -> new ItemStack(Items.NETHERITE_HELMET);
                case CHEST -> new ItemStack(Items.NETHERITE_CHESTPLATE);
                case LEGS -> new ItemStack(Items.NETHERITE_LEGGINGS);
                case FEET -> new ItemStack(Items.NETHERITE_BOOTS);
                default -> null;
            };
        } else if (level >= 50) {
            armor = switch (slot) {
                case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
                case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
                case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
                default -> null;
            };
        } else if (level >= 40) {
            armor = switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> null;
            };
        } else if (level >= 30) {
            armor = switch (slot) {
                case HEAD -> new ItemStack(Items.CHAINMAIL_HELMET);
                case CHEST -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                case LEGS -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                case FEET -> new ItemStack(Items.CHAINMAIL_BOOTS);
                default -> null;
            };
        } else if (level >= 20) {
            armor = switch (slot) {
                case HEAD -> new ItemStack(Items.LEATHER_HELMET);
                case CHEST -> new ItemStack(Items.LEATHER_CHESTPLATE);
                case LEGS -> new ItemStack(Items.LEATHER_LEGGINGS);
                case FEET -> new ItemStack(Items.LEATHER_BOOTS);
                default -> null;
            };
        }
        
        // Note: Enchantments can be added in future versions
        // For now, the base armor provides sufficient protection scaling
        
        return armor;
    }
}

