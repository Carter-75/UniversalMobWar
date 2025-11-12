package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.util.OperationScheduler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Handles mob evolution and power scaling based on kills.
 * Applies stat bonuses and equipment as mobs level up.
 */
public class EvolutionSystem {
    
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("universalmobwar", "health_bonus");
    private static final Identifier DAMAGE_MODIFIER_ID = Identifier.of("universalmobwar", "damage_bonus");
    private static final Identifier SPEED_MODIFIER_ID = Identifier.of("universalmobwar", "speed_bonus");
    private static final Identifier ARMOR_MODIFIER_ID = Identifier.of("universalmobwar", "armor_bonus");
    private static final Identifier KNOCKBACK_MODIFIER_ID = Identifier.of("universalmobwar", "knockback_bonus");
    
    /**
     * Called when a mob kills another mob. Increases kill count and levels up if needed.
     * OPTIMIZED: Smart scheduled with 0.25s minimum delay, prevents operation overlaps.
     * ANTI-STARVATION: Won't queue if queue is full (drops operation - mob will retry next kill).
     */
    public static void onMobKill(MobEntity killer, LivingEntity victim) {
        String killerId = killer.getUuid().toString();
        
        // SMART SCHEDULING: Only process if not overlapping with other operations
        if (!OperationScheduler.canExecuteEvolution(killerId)) {
            // ANTI-STARVATION: Queue is either full or operation on cooldown
            // Queue for later - schedule 250ms in the future
            if (killer.getWorld() instanceof ServerWorld serverWorld) {
                OperationScheduler.incrementEvolutionQueue(); // Track queue depth
                serverWorld.getServer().execute(() -> {
                    try {
                        Thread.sleep(250); // 0.25s delay
                        processKillInternal(killer, victim);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        OperationScheduler.decrementEvolutionQueue(); // Done - free queue slot
                    }
                });
            }
            return;
        }
        
        // Execute immediately if allowed
        processKillInternal(killer, victim);
        OperationScheduler.markEvolutionExecuted(killerId);
    }
    
    /**
     * Internal kill processing with stat and equipment updates.
     */
    private static void processKillInternal(MobEntity killer, LivingEntity victim) {
        MobWarData data = MobWarData.get(killer);
        int oldLevel = data.getLevel();
        
        data.addKill();
        int newLevel = data.getLevel();
        
        // Apply bonuses if leveled up
        if (newLevel > oldLevel) {
            applyLevelBonuses(killer, data);
            
            // OPTIMIZATION: Delay equipment spawning by additional 250ms to spread load
            if (killer.getWorld() instanceof ServerWorld serverWorld) {
                final int finalNewLevel = newLevel;
                serverWorld.getServer().execute(() -> {
                    try {
                        Thread.sleep(250); // Additional 0.25s delay for equipment
                        updateEquipment(killer, finalNewLevel);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } else {
                updateEquipment(killer, newLevel);
            }
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
            health.removeModifier(HEALTH_MODIFIER_ID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    HEALTH_MODIFIER_ID,
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
            damage.removeModifier(DAMAGE_MODIFIER_ID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    DAMAGE_MODIFIER_ID,
                    data.getDamageBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                damage.addPersistentModifier(modifier);
            }
        }
        
        // Speed bonus
        EntityAttributeInstance speed = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    SPEED_MODIFIER_ID,
                    data.getSpeedBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                speed.addPersistentModifier(modifier);
            }
        }
        
        // Armor bonus
        EntityAttributeInstance armor = mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    ARMOR_MODIFIER_ID,
                    data.getArmorBonus(),
                    EntityAttributeModifier.Operation.ADD_VALUE
                );
                armor.addPersistentModifier(modifier);
            }
        }
        
        // Knockback resistance bonus
        EntityAttributeInstance knockback = mob.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.removeModifier(KNOCKBACK_MODIFIER_ID);
            if (level > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    KNOCKBACK_MODIFIER_ID,
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

