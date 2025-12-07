
package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.util.OperationScheduler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.registry.RegistryKey;

/**
 * THE ULTIMATE PROGRESSION SYSTEM v2.0
 * Implements the complex skill tree system requested by the user.
 */
public class EvolutionSystem {
    
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("universalmobwar", "health_bonus");

    // --- MOB CATEGORIES ---
    private static final Set<String> PASSIVE_MOBS = new HashSet<>();
    private static final Set<String> HOSTILE_MOBS = new HashSet<>();
    private static final Set<String> ZOMBIE_TYPE = new HashSet<>();
    private static final Set<String> PROJECTILE_TYPE = new HashSet<>();
    private static final Set<String> NO_WEAPON = new HashSet<>();
    private static final Set<String> BOW_USERS = new HashSet<>();

    static {
        // Initialize categories based on user list
        // Ally (gp)
        PASSIVE_MOBS.add("minecraft:allay"); // Assuming Ally meant Allay? Or custom?
        PASSIVE_MOBS.add("minecraft:armadillo");
        PASSIVE_MOBS.add("minecraft:axolotl");
        PASSIVE_MOBS.add("minecraft:bat"); NO_WEAPON.add("minecraft:bat");
        HOSTILE_MOBS.add("minecraft:bee");
        HOSTILE_MOBS.add("minecraft:blaze"); PROJECTILE_TYPE.add("minecraft:blaze"); NO_WEAPON.add("minecraft:blaze");
        HOSTILE_MOBS.add("minecraft:bogged");
        HOSTILE_MOBS.add("minecraft:breeze"); PROJECTILE_TYPE.add("minecraft:breeze"); NO_WEAPON.add("minecraft:breeze");
        PASSIVE_MOBS.add("minecraft:camel");
        PASSIVE_MOBS.add("minecraft:cat");
        HOSTILE_MOBS.add("minecraft:cave_spider"); NO_WEAPON.add("minecraft:cave_spider");
        PASSIVE_MOBS.add("minecraft:chicken");
        PASSIVE_MOBS.add("minecraft:cod");
        PASSIVE_MOBS.add("minecraft:cow");
        HOSTILE_MOBS.add("minecraft:creeper"); NO_WEAPON.add("minecraft:creeper");
        PASSIVE_MOBS.add("minecraft:dolphin");
        PASSIVE_MOBS.add("minecraft:donkey");
        HOSTILE_MOBS.add("minecraft:drowned"); ZOMBIE_TYPE.add("minecraft:drowned");
        HOSTILE_MOBS.add("minecraft:elder_guardian"); NO_WEAPON.add("minecraft:elder_guardian");
        HOSTILE_MOBS.add("minecraft:enderman");
        HOSTILE_MOBS.add("minecraft:endermite"); NO_WEAPON.add("minecraft:endermite");
        HOSTILE_MOBS.add("minecraft:evoker"); NO_WEAPON.add("minecraft:evoker");
        PASSIVE_MOBS.add("minecraft:fox");
        PASSIVE_MOBS.add("minecraft:frog");
        HOSTILE_MOBS.add("minecraft:ghast");
        PASSIVE_MOBS.add("minecraft:glow_squid");
        PASSIVE_MOBS.add("minecraft:goat");
        HOSTILE_MOBS.add("minecraft:guardian");
        HOSTILE_MOBS.add("minecraft:hoglin"); NO_WEAPON.add("minecraft:hoglin");
        PASSIVE_MOBS.add("minecraft:horse");
        HOSTILE_MOBS.add("minecraft:husk"); ZOMBIE_TYPE.add("minecraft:husk");
        HOSTILE_MOBS.add("minecraft:iron_golem");
        PASSIVE_MOBS.add("minecraft:llama");
        HOSTILE_MOBS.add("minecraft:magma_cube"); NO_WEAPON.add("minecraft:magma_cube");
        PASSIVE_MOBS.add("minecraft:mooshroom");
        PASSIVE_MOBS.add("minecraft:mule");
        PASSIVE_MOBS.add("minecraft:ocelot");
        PASSIVE_MOBS.add("minecraft:panda");
        PASSIVE_MOBS.add("minecraft:parrot");
        HOSTILE_MOBS.add("minecraft:phantom"); NO_WEAPON.add("minecraft:phantom");
        PASSIVE_MOBS.add("minecraft:pig");
        HOSTILE_MOBS.add("minecraft:piglin");
        HOSTILE_MOBS.add("minecraft:piglin_brute");
        HOSTILE_MOBS.add("minecraft:pillager"); PROJECTILE_TYPE.add("minecraft:pillager"); BOW_USERS.add("minecraft:pillager"); // Uses crossbow but fits bow tree?
        HOSTILE_MOBS.add("minecraft:polar_bear"); NO_WEAPON.add("minecraft:polar_bear");
        HOSTILE_MOBS.add("minecraft:pufferfish"); NO_WEAPON.add("minecraft:pufferfish");
        PASSIVE_MOBS.add("minecraft:rabbit");
        HOSTILE_MOBS.add("minecraft:ravager"); NO_WEAPON.add("minecraft:ravager");
        PASSIVE_MOBS.add("minecraft:salmon");
        PASSIVE_MOBS.add("minecraft:sheep");
        HOSTILE_MOBS.add("minecraft:shulker"); NO_WEAPON.add("minecraft:shulker"); PROJECTILE_TYPE.add("minecraft:shulker");
        HOSTILE_MOBS.add("minecraft:silverfish"); NO_WEAPON.add("minecraft:silverfish");
        HOSTILE_MOBS.add("minecraft:skeleton"); PROJECTILE_TYPE.add("minecraft:skeleton"); BOW_USERS.add("minecraft:skeleton");
        PASSIVE_MOBS.add("minecraft:skeleton_horse");
        HOSTILE_MOBS.add("minecraft:slime"); NO_WEAPON.add("minecraft:slime");
        PASSIVE_MOBS.add("minecraft:sniffer");
        HOSTILE_MOBS.add("minecraft:snow_golem"); PROJECTILE_TYPE.add("minecraft:snow_golem"); NO_WEAPON.add("minecraft:snow_golem");
        HOSTILE_MOBS.add("minecraft:spider"); NO_WEAPON.add("minecraft:spider");
        PASSIVE_MOBS.add("minecraft:squid");
        HOSTILE_MOBS.add("minecraft:stray"); BOW_USERS.add("minecraft:stray"); PROJECTILE_TYPE.add("minecraft:stray");
        PASSIVE_MOBS.add("minecraft:strider");
        PASSIVE_MOBS.add("minecraft:tadpole");
        PASSIVE_MOBS.add("minecraft:trader_llama");
        PASSIVE_MOBS.add("minecraft:tropical_fish");
        PASSIVE_MOBS.add("minecraft:turtle");
        HOSTILE_MOBS.add("minecraft:vex");
        PASSIVE_MOBS.add("minecraft:villager");
        HOSTILE_MOBS.add("minecraft:vindicator");
        PASSIVE_MOBS.add("minecraft:wandering_trader");
        HOSTILE_MOBS.add("minecraft:warden");
        HOSTILE_MOBS.add("minecraft:witch"); NO_WEAPON.add("minecraft:witch"); PROJECTILE_TYPE.add("minecraft:witch");
        HOSTILE_MOBS.add("minecraft:wither_skeleton");
        HOSTILE_MOBS.add("minecraft:wolf"); NO_WEAPON.add("minecraft:wolf");
        HOSTILE_MOBS.add("minecraft:zoglin"); NO_WEAPON.add("minecraft:zoglin"); ZOMBIE_TYPE.add("minecraft:zoglin");
        HOSTILE_MOBS.add("minecraft:zombie"); ZOMBIE_TYPE.add("minecraft:zombie");
        PASSIVE_MOBS.add("minecraft:zombie_horse");
        HOSTILE_MOBS.add("minecraft:zombie_villager"); ZOMBIE_TYPE.add("minecraft:zombie_villager");
        HOSTILE_MOBS.add("minecraft:zombified_piglin"); ZOMBIE_TYPE.add("minecraft:zombified_piglin");
    }
    
    public static void onMobSpawn(MobEntity mob, ServerWorld world) {
        if (!mod.universalmobwar.config.ModConfig.getInstance().scalingEnabled) return;

        MobWarData data = MobWarData.get(mob);
        
        // Calculate Skill Points based on Day
        long day = world.getTimeOfDay() / 24000L;
        double totalPoints = calculateTotalSkillPoints(day);
        
        // Update points if day has advanced
        if (totalPoints > data.getSkillPoints()) {
            data.setSkillPoints(totalPoints);
            MobWarData.save(mob, data);
        }
        
        // Apply Skill Tree
        applySkillTree(mob, data);
    }

    public static void onMobKill(MobEntity killer, LivingEntity victim) {
        // Kills don't give skill points in this new system, only days.
        // But we still track kills for other reasons if needed.
        MobWarData data = MobWarData.get(killer);
        data.addKill();
        MobWarData.save(killer, data);
    }
    
    private static double calculateTotalSkillPoints(long day) {
        double points = 0;
        
        // Day 0-10: 0.1 per day
        long period1 = Math.min(day, 10);
        points += period1 * 0.1;
        
        // Day 11-15: 0.5 per day
        if (day > 10) {
            long period2 = Math.min(day - 10, 5);
            points += period2 * 0.5;
        }
        
        // Day 16-20: 1.0 per day
        if (day > 15) {
            long period3 = Math.min(day - 15, 5);
            points += period3 * 1.0;
        }
        
        // Day 21-25: 1.5 per day
        if (day > 20) {
            long period4 = Math.min(day - 20, 5);
            points += period4 * 1.5;
        }
        
        // Day 26-30: 3.0 per day
        if (day > 25) {
            long period5 = Math.min(day - 25, 5);
            points += period5 * 3.0;
        }
        
        // Day 31+: 5.0 per day
        if (day > 30) {
            long period6 = day - 30;
            points += period6 * 5.0;
        }
        
        return points;
    }

    private static void applySkillTree(MobEntity mob, MobWarData data) {
        double availablePoints = data.getSkillPoints() - data.getSpentPoints();
        NbtCompound skillData = data.getSkillData();
        String mobId = Registries.ENTITY_TYPE.getId(mob.getType()).toString();
        
        boolean isHostile = HOSTILE_MOBS.contains(mobId) || (!PASSIVE_MOBS.contains(mobId) && mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) > 0);
        
        boolean changed = false;
        
        // Loop until we can't afford anything or maxed out
        // We'll try to spend points on available trees
        int safetyCounter = 0;
        while (availablePoints >= 1.0 && safetyCounter++ < 100) {
            boolean spent = false;
            
            // 1. General Tree (Hostile) or General Passive (Passive)
            if (isHostile) {
                if (availablePoints >= 2.0 && tryUpgradeGeneral(mob, skillData)) {
                    availablePoints -= 2.0;
                    spent = true;
                }
            } else {
                if (availablePoints >= 2.0 && tryUpgradeGeneralPassive(mob, skillData)) {
                    availablePoints -= 2.0;
                    spent = true;
                }
            }
            
            // 2. Equipment Trees (Sword, Bow, Armor)
            // Only if hostile and has equipment slots
            if (isHostile) {
                // Sword
                if (mob.getMainHandStack().getItem() instanceof net.minecraft.item.SwordItem) {
                    double cost = getSwordUpgradeCost(skillData.getInt("sword_level"));
                    if (availablePoints >= cost && tryUpgradeSword(mob, skillData)) {
                        availablePoints -= cost;
                        spent = true;
                    }
                }
                
                // Bow
                if (mob.getMainHandStack().getItem() instanceof net.minecraft.item.BowItem || mob.getMainHandStack().getItem() instanceof net.minecraft.item.CrossbowItem) {
                    double cost = getBowUpgradeCost(skillData.getInt("bow_level"));
                    if (availablePoints >= cost && tryUpgradeBow(mob, skillData)) {
                        availablePoints -= cost;
                        spent = true;
                    }
                }
                
                // Armor (Always try to upgrade armor if they have it or can have it)
                double armorCost = getArmorUpgradeCost(skillData.getInt("armor_tree_level"));
                if (availablePoints >= armorCost && tryUpgradeArmor(mob, skillData)) {
                    availablePoints -= armorCost;
                    spent = true;
                }
            }
            
            // 3. Special Trees
            if (ZOMBIE_TYPE.contains(mobId)) {
                if (availablePoints >= 3.0 && tryUpgradeZombie(mob, skillData)) {
                    availablePoints -= 3.0;
                    spent = true;
                }
            }
            
            if (PROJECTILE_TYPE.contains(mobId)) {
                if (availablePoints >= 2.0 && tryUpgradeProjectile(mob, skillData)) {
                    availablePoints -= 2.0;
                    spent = true;
                }
            }
            
            if (!spent) break; // Couldn't afford anything
            changed = true;
        }
        
        if (changed) {
            data.setSpentPoints(data.getSkillPoints() - availablePoints);
            data.setSkillData(skillData);
            MobWarData.save(mob, data);
        }
        
        // Apply effects based on current skill data
        applyEffects(mob, skillData);
    }
    
    // --- UPGRADE LOGIC ---
    
    private static boolean tryUpgradeGeneral(MobEntity mob, NbtCompound data) {
        // healing 1>2>3>4>5
        // healthboost 1>...10
        // resis 1>2>3>resis 3 and fire resis 1
        // invis 1...
        // strength 1>2>3>4
        // shield chance
        
        // We pick a random available upgrade in this tree
        List<String> options = new ArrayList<>();
        int healing = data.getInt("g_healing");
        if (healing < 5) options.add("healing");
        
        int health = data.getInt("g_health");
        if (health < 10) options.add("health");
        
        int resis = data.getInt("g_resis");
        if (resis < 4) options.add("resis"); // 4th level is resis 3 + fire res
        
        int strength = data.getInt("g_strength");
        if (strength < 4) options.add("strength");
        
        int invis = data.getInt("g_invis");
        if (invis < 9) options.add("invis");
        
        int shield = data.getInt("g_shield");
        if (shield < 1) options.add("shield"); // Just a flag to enable shield chance logic
        
        if (options.isEmpty()) return false;
        
        String choice = options.get(mob.getRandom().nextInt(options.size()));
        data.putInt("g_" + choice, data.getInt("g_" + choice) + 1);
        return true;
    }
    
    private static boolean tryUpgradeGeneralPassive(MobEntity mob, NbtCompound data) {
        List<String> options = new ArrayList<>();
        if (data.getInt("gp_healing") < 3) options.add("gp_healing");
        if (data.getInt("gp_health") < 3) options.add("gp_health");
        if (data.getInt("gp_resis") < 1) options.add("gp_resis");
        
        if (options.isEmpty()) return false;
        String choice = options.get(mob.getRandom().nextInt(options.size()));
        data.putInt(choice, data.getInt(choice) + 1);
        return true;
    }
    
    private static boolean tryUpgradeSword(MobEntity mob, NbtCompound data) {
        // sharp 1-5, fire 1-2, mending 1, unbreaking 1-3, knockback 1-2, smite 1-5, bane 1-5, looting 1-3
        List<String> options = new ArrayList<>();
        if (data.getInt("s_sharp") < 5) options.add("s_sharp");
        if (data.getInt("s_fire") < 2) options.add("s_fire");
        if (data.getInt("s_kb") < 2) options.add("s_kb");
        if (data.getInt("s_unb") < 3) options.add("s_unb");
        
        if (options.isEmpty()) return false;
        String choice = options.get(mob.getRandom().nextInt(options.size()));
        data.putInt(choice, data.getInt(choice) + 1);
        data.putInt("sword_level", data.getInt("sword_level") + 1);
        
        // Apply enchant immediately
        ItemStack stack = mob.getMainHandStack();
        if (!stack.isEmpty()) {
            if (choice.equals("s_sharp")) addEnchant(mob, stack, Enchantments.SHARPNESS, data.getInt("s_sharp"));
            if (choice.equals("s_fire")) addEnchant(mob, stack, Enchantments.FIRE_ASPECT, data.getInt("s_fire"));
            if (choice.equals("s_kb")) addEnchant(mob, stack, Enchantments.KNOCKBACK, data.getInt("s_kb"));
            if (choice.equals("s_unb")) addEnchant(mob, stack, Enchantments.UNBREAKING, data.getInt("s_unb"));
        }
        return true;
    }
    
    private static boolean tryUpgradeBow(MobEntity mob, NbtCompound data) {
        List<String> options = new ArrayList<>();
        if (data.getInt("b_power") < 5) options.add("b_power");
        if (data.getInt("b_punch") < 2) options.add("b_punch");
        if (data.getInt("b_flame") < 1) options.add("b_flame");
        
        if (options.isEmpty()) return false;
        String choice = options.get(mob.getRandom().nextInt(options.size()));
        data.putInt(choice, data.getInt(choice) + 1);
        data.putInt("bow_level", data.getInt("bow_level") + 1);
        
        ItemStack stack = mob.getMainHandStack();
        if (!stack.isEmpty()) {
            if (choice.equals("b_power")) addEnchant(mob, stack, Enchantments.POWER, data.getInt("b_power"));
            if (choice.equals("b_punch")) addEnchant(mob, stack, Enchantments.PUNCH, data.getInt("b_punch"));
            if (choice.equals("b_flame")) addEnchant(mob, stack, Enchantments.FLAME, data.getInt("b_flame"));
        }
        return true;
    }
    
    private static boolean tryUpgradeArmor(MobEntity mob, NbtCompound data) {
        // prot 1-4, fire 1-4, blast 1-4, proj 1-4, thorns 1-3, unb 1-3
        List<String> options = new ArrayList<>();
        if (data.getInt("a_prot") < 4) options.add("a_prot");
        if (data.getInt("a_thorns") < 3) options.add("a_thorns");
        if (data.getInt("a_unb") < 3) options.add("a_unb");
        
        if (options.isEmpty()) return false;
        String choice = options.get(mob.getRandom().nextInt(options.size()));
        data.putInt(choice, data.getInt(choice) + 1);
        data.putInt("armor_tree_level", data.getInt("armor_tree_level") + 1);
        
        // Apply to all armor pieces
        for (ItemStack stack : mob.getArmorItems()) {
            if (stack.isEmpty()) continue;
            if (choice.equals("a_prot")) addEnchant(mob, stack, Enchantments.PROTECTION, data.getInt("a_prot"));
            if (choice.equals("a_thorns")) addEnchant(mob, stack, Enchantments.THORNS, data.getInt("a_thorns"));
            if (choice.equals("a_unb")) addEnchant(mob, stack, Enchantments.UNBREAKING, data.getInt("a_unb"));
        }
        return true;
    }
    
    private static boolean tryUpgradeZombie(MobEntity mob, NbtCompound data) {
        if (data.getInt("z_level") < 3) {
            data.putInt("z_level", data.getInt("z_level") + 1);
            return true;
        }
        return false;
    }
    
    private static boolean tryUpgradeProjectile(MobEntity mob, NbtCompound data) {
        if (data.getInt("pro_level") < 5) {
            data.putInt("pro_level", data.getInt("pro_level") + 1);
            return true;
        }
        return false;
    }

    // --- COST CALCULATORS ---
    private static double getSwordUpgradeCost(int currentLevel) {
        // 1, 1, 2, 2, 3, 3, 4, 4, 5...
        if (currentLevel < 2) return 1.0;
        if (currentLevel < 4) return 2.0;
        if (currentLevel < 6) return 3.0;
        if (currentLevel < 8) return 4.0;
        return 5.0;
    }
    
    private static double getBowUpgradeCost(int currentLevel) {
        // 2, 2, 2, 3, 3, 3, 3...
        if (currentLevel < 3) return 2.0;
        return 3.0;
    }
    
    private static double getArmorUpgradeCost(int currentLevel) {
        // 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5...
        if (currentLevel < 3) return 2.0;
        if (currentLevel < 7) return 3.0;
        if (currentLevel < 10) return 4.0;
        return 5.0;
    }

    // --- EFFECT APPLICATION ---
    private static void applyEffects(MobEntity mob, NbtCompound data) {
        // General Tree Effects
        int healthLvl = data.getInt("g_health");
        if (healthLvl > 0) updateModifier(mob, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER_ID, healthLvl * 2.0); // +1 heart per level
        
        int strengthLvl = data.getInt("g_strength");
        if (strengthLvl > 0) mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 999999, strengthLvl - 1, false, false));
        
        int resisLvl = data.getInt("g_resis");
        if (resisLvl > 0) {
            int amp = (resisLvl >= 4) ? 2 : resisLvl - 1; // Level 4 gives Resis 3 (amp 2)
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 999999, amp, false, false));
            if (resisLvl >= 4) mob.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 999999, 0, false, false));
        }
        
        int healingLvl = data.getInt("g_healing");
        if (healingLvl > 0) mob.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 999999, healingLvl - 1, false, false));
        
        // Invisibility Logic (Simplified for now: 25% chance if level > 0)
        int invisLvl = data.getInt("g_invis");
        if (invisLvl > 0 && mob.getRandom().nextFloat() < 0.25f) {
             mob.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false)); // 10s burst
        }
        
        // Shield Logic
        if (data.getInt("g_shield") > 0 && mob.getOffHandStack().isEmpty()) {
            // Curve logic: 0% to 100% based on day/level. Simplified to 50% for now if unlocked.
            if (mob.getRandom().nextFloat() < 0.5f) {
                mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
        }
        
        // Passive Tree Effects
        int gpHealth = data.getInt("gp_health");
        if (gpHealth > 0) updateModifier(mob, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER_ID, gpHealth * 2.0);
        
        int gpResis = data.getInt("gp_resis");
        if (gpResis > 0) mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 999999, 0, false, false));
        
        int gpHealing = data.getInt("gp_healing");
        if (gpHealing > 0) mob.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 999999, gpHealing - 1, false, false));
    }

    private static void updateModifier(MobEntity mob, RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attributeEntry, Identifier id, double value) {
        EntityAttributeInstance attribute = mob.getAttributeInstance(attributeEntry);
        if (attribute == null) return;
        attribute.removeModifier(id);
        if (value > 0) {
            attribute.addPersistentModifier(new EntityAttributeModifier(id, value, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }
    
    private static void addEnchant(MobEntity mob, ItemStack stack, RegistryKey<Enchantment> key, int level) {
        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            Optional<RegistryEntry.Reference<Enchantment>> entry = serverWorld.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key);
            entry.ifPresent(enchantmentReference -> stack.addEnchantment(enchantmentReference, level));
        }
    }
    
    public static void cleanup() {}
}

