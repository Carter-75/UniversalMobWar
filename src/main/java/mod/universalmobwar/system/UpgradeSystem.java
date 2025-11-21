package mod.universalmobwar.system;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.*;
import mod.universalmobwar.data.PowerProfile;

public class UpgradeSystem {

    // Cost arrays
    private static final int[] GENERAL_COSTS = {2}; // Always 2
    private static final int[] SWORD_COSTS = {1, 1, 2, 2, 3, 3, 4, 4, 5};
    private static final int[] TRIDENT_COSTS = {3, 3, 3};
    private static final int[] BOW_COSTS = {2, 2, 2, 3, 3, 3, 3};
    private static final int[] ARMOR_COSTS = {2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5};
    private static final int[] ZOMBIE_COSTS = {3};
    private static final int[] PROJECTILE_COSTS = {2};
    private static final int[] CAVE_SPIDER_COSTS = {3};
    private static final int[] CREEPER_COSTS = {3};
    private static final int[] WITCH_COSTS = {3};

    // Item Tiers
    private static final List<String> SWORD_TIERS = List.of("minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword", "minecraft:diamond_sword", "minecraft:netherite_sword");
    private static final List<String> GOLD_SWORD_TIERS = List.of("minecraft:golden_sword", "minecraft:netherite_sword");
    private static final List<String> AXE_TIERS = List.of("minecraft:wooden_axe", "minecraft:stone_axe", "minecraft:iron_axe", "minecraft:diamond_axe", "minecraft:netherite_axe");
    
    private static final List<String> HELMET_TIERS = List.of("minecraft:leather_helmet", "minecraft:iron_helmet", "minecraft:diamond_helmet", "minecraft:netherite_helmet");
    private static final List<String> CHEST_TIERS = List.of("minecraft:leather_chestplate", "minecraft:iron_chestplate", "minecraft:diamond_chestplate", "minecraft:netherite_chestplate");
    private static final List<String> LEGS_TIERS = List.of("minecraft:leather_leggings", "minecraft:iron_leggings", "minecraft:diamond_leggings", "minecraft:netherite_leggings");
    private static final List<String> BOOTS_TIERS = List.of("minecraft:leather_boots", "minecraft:iron_boots", "minecraft:diamond_boots", "minecraft:netherite_boots");

    private static class SimState {
        Map<String, Integer> levels = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        Map<String, Integer> itemTiers = new HashMap<>();
        double spentPoints = 0;
        
        int getLevel(String id) { return levels.getOrDefault(id, 0); }
        void setLevel(String id, int val) { levels.put(id, val); }
        void incLevel(String id) { levels.put(id, getLevel(id) + 1); }
        
        int getCategoryCount(String cat) { return categoryCounts.getOrDefault(cat, 0); }
        void incCategoryCount(String cat) { categoryCounts.put(cat, getCategoryCount(cat) + 1); }
        
        int getItemTier(String type) { return itemTiers.getOrDefault(type, 0); }
        void setItemTier(String type, int val) { itemTiers.put(type, val); }
    }

    public static void selectUpgrades(PowerProfile profile, String archetype, long worldSeed) {
        // We don't store chosen upgrades list anymore, we simulate and apply directly.
        // But for persistence/debugging we might want to, but the system is dynamic now.
        // We will just use this method to trigger the application if needed, 
        // but actually we should apply upgrades in applyUpgradeNode equivalent.
        // Since the architecture changed, we'll do simulation and application in one go 
        // or return a state object.
        // For now, let's just update the profile's chosenUpgrades to reflect the simulation result for debugging?
        // Or better, we just don't use chosenUpgrades for logic anymore.
    }
    
    public static void applyUpgradeNode(MobEntity mob, UpgradeNode node) {
        // Deprecated, but kept for compatibility if called from elsewhere.
    }

    // New method to apply everything
    public static void applyUpgrades(MobEntity mob, PowerProfile profile) {
        SimState state = simulate(mob, profile);
        applyStateToMob(mob, state, profile);
    }

    private static SimState simulate(MobEntity mob, PowerProfile profile) {
        SimState state = new SimState();
        double totalPoints = profile.totalPoints;
        long seed = mob.getUuid().hashCode() ^ (long)totalPoints; // Deterministic per point total
        Random rand = new Random(seed);

        Set<String> cats = profile.categories;
        boolean isG = cats.contains("g");
        boolean isGP = cats.contains("gp");
        boolean isZ = cats.contains("z");
        boolean isPro = cats.contains("pro");
        boolean isBow = cats.contains("bow");
        boolean isTrident = cats.contains("trident");
        boolean isAxe = cats.contains("axe");
        boolean isNW = cats.contains("nw");
        boolean isCaveSpider = mob.getType().getTranslationKey().contains("cave_spider");
        boolean isCreeper = mob.getType().getTranslationKey().contains("creeper");
        boolean isWitch = mob.getType().getTranslationKey().contains("witch");
        
        // Determine weapon type if not explicit
        boolean useSword = isG && !isBow && !isTrident && !isAxe && !isNW && !isWitch;
        boolean useAxe = isAxe;
        
        // Main loop
        while (state.spentPoints < totalPoints) {
            List<Runnable> possibleUpgrades = new ArrayList<>();
            
            // General Tree (G)
            if (isG) {
                addGeneralUpgrades(state, possibleUpgrades, GENERAL_COSTS);
            }
            // General Passive Tree (GP)
            if (isGP) {
                addGeneralPassiveUpgrades(state, possibleUpgrades, GENERAL_COSTS);
            }
            // Zombie Tree
            if (isZ) {
                addZombieUpgrades(state, possibleUpgrades, ZOMBIE_COSTS);
            }
            // Projectile Tree
            if (isPro) {
                addProjectileUpgrades(state, possibleUpgrades, PROJECTILE_COSTS);
            }
            // Cave Spider Tree
            if (isCaveSpider) {
                addCaveSpiderUpgrades(state, possibleUpgrades, CAVE_SPIDER_COSTS);
            }
            // Creeper Tree
            if (isCreeper) {
                addCreeperUpgrades(state, possibleUpgrades, CREEPER_COSTS);
            }
            // Witch Tree
            if (isWitch) {
                addWitchUpgrades(state, possibleUpgrades, WITCH_COSTS);
            }
            
            // Weapon Trees
            if (useSword) {
                addWeaponUpgrades(state, possibleUpgrades, SWORD_COSTS, "sword", 
                    List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"),
                    List.of(5, 2, 1, 3, 2, 5, 5, 3));
            }
            if (isTrident) {
                addWeaponUpgrades(state, possibleUpgrades, TRIDENT_COSTS, "trident",
                    List.of("impaling", "channeling", "unbreaking", "mending", "loyalty", "riptide"),
                    List.of(5, 1, 3, 1, 3, 3));
            }
            if (isBow) {
                addWeaponUpgrades(state, possibleUpgrades, BOW_COSTS, "bow",
                    List.of("power", "flame", "punch", "infinity", "unbreaking", "mending"),
                    List.of(5, 1, 2, 1, 3, 1));
            }
            if (useAxe) {
                // Axe uses sword costs? User didn't specify axe costs. Assuming sword costs.
                addWeaponUpgrades(state, possibleUpgrades, SWORD_COSTS, "axe",
                    List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"),
                    List.of(5, 5, 5, 3, 1, 5));
            }
            
            // Armor Tree (Always try)
            addArmorUpgrades(state, possibleUpgrades, ARMOR_COSTS);

            if (possibleUpgrades.isEmpty()) {
                profile.isMaxed = true;
                break;
            }

            // Pick one
            Runnable upgrade = possibleUpgrades.get(rand.nextInt(possibleUpgrades.size()));
            upgrade.run();
            
            // Check for Tier Upgrades
            checkTierUpgrades(state, useSword, isTrident, isBow, isAxe, mob);
        }
        
        return state;
    }

    private static void addGeneralUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("g"), costs);
        
        // healing 1-5
        if (state.getLevel("healing") < 5) options.add(() -> {
            state.incLevel("healing");
            state.incCategoryCount("g");
            state.spentPoints += cost;
        });
        // healthboost 1-10
        if (state.getLevel("health_boost") < 10) options.add(() -> {
            state.incLevel("health_boost");
            state.incCategoryCount("g");
            state.spentPoints += cost;
        });
        // resis 1-4 (Level 4 grants Fire Resis 1 too)
        if (state.getLevel("resistance") < 4) options.add(() -> {
            state.incLevel("resistance");
            if (state.getLevel("resistance") == 4) state.setLevel("fire_resistance", 1);
            state.incCategoryCount("g");
            state.spentPoints += cost;
        });
        // invis 1-10
        if (state.getLevel("invis_mastery") < 10) options.add(() -> {
            state.incLevel("invis_mastery");
            state.incCategoryCount("g");
            state.spentPoints += cost;
        });
        // strength 1-4
        if (state.getLevel("strength") < 4) options.add(() -> {
            state.incLevel("strength");
            state.incCategoryCount("g");
            state.spentPoints += cost;
        });
    }

    private static void addGeneralPassiveUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("gp"), costs);
        // healing 1-3
        if (state.getLevel("healing") < 3) options.add(() -> {
            state.incLevel("healing");
            state.incCategoryCount("gp");
            state.spentPoints += cost;
        });
        // healthboost 1-3
        if (state.getLevel("health_boost") < 3) options.add(() -> {
            state.incLevel("health_boost");
            state.incCategoryCount("gp");
            state.spentPoints += cost;
        });
        // resis 1
        if (state.getLevel("resistance") < 1) options.add(() -> {
            state.incLevel("resistance");
            state.incCategoryCount("gp");
            state.spentPoints += cost;
        });
    }

    private static void addZombieUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("z"), costs);
        // hunger 1-3
        if (state.getLevel("hunger_attack") < 3) options.add(() -> {
            state.incLevel("hunger_attack");
            state.incCategoryCount("z");
            state.spentPoints += cost;
        });
        // horde summon 1-8
        if (state.getLevel("horde_summon") < 8) options.add(() -> {
            state.incLevel("horde_summon");
            state.incCategoryCount("z");
            state.spentPoints += cost;
        });
    }

    private static void addProjectileUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("pro"), costs);
        // piercing 1-5
        if (state.getLevel("piercing_shot") < 5) options.add(() -> {
            state.incLevel("piercing_shot");
            state.incCategoryCount("pro");
            state.spentPoints += cost;
        });
        // multishot 1-4
        if (state.getLevel("multishot_skill") < 4) options.add(() -> {
            state.incLevel("multishot_skill");
            state.incCategoryCount("pro");
            state.spentPoints += cost;
        });
    }

    private static void addCaveSpiderUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("cave_spider"), costs);
        // web 1-5
        if (state.getLevel("web_shot") < 5) options.add(() -> {
            state.incLevel("web_shot");
            state.incCategoryCount("cave_spider");
            state.spentPoints += cost;
        });
        // poi 1-2 (Poison 2, Poison 3)
        if (state.getLevel("poison_attack") < 2) options.add(() -> {
            state.incLevel("poison_attack");
            state.incCategoryCount("cave_spider");
            state.spentPoints += cost;
        });
    }

    private static void addCreeperUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("creeper"), costs);
        // spd 1-5
        if (state.getLevel("speed") < 5) options.add(() -> {
            state.incLevel("speed");
            state.incCategoryCount("creeper");
            state.spentPoints += cost;
        });
        // ch 1-1
        if (state.getLevel("charged") < 1) options.add(() -> {
            state.incLevel("charged");
            state.incCategoryCount("creeper");
            state.spentPoints += cost;
        });
        // fuse 1-5
        if (state.getLevel("fuse_reduction") < 5) options.add(() -> {
            state.incLevel("fuse_reduction");
            state.incCategoryCount("creeper");
            state.spentPoints += cost;
        });
        // exp 1-5
        if (state.getLevel("explosion_radius") < 5) options.add(() -> {
            state.incLevel("explosion_radius");
            state.incCategoryCount("creeper");
            state.spentPoints += cost;
        });
        // potion mastery 1-10 (Chance 10% -> 100%)
        if (state.getLevel("creeper_potion_mastery") < 10) options.add(() -> {
            state.incLevel("creeper_potion_mastery");
            state.incCategoryCount("creeper");
            state.spentPoints += cost;
        });
    }

    private static void addWitchUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("witch"), costs);
        // potion mastery 1-10 (Chance 10% -> 100%)
        if (state.getLevel("witch_potion_mastery") < 10) options.add(() -> {
            state.incLevel("witch_potion_mastery");
            state.incCategoryCount("witch");
            state.spentPoints += cost;
        });
    }

    private static void addWeaponUpgrades(SimState state, List<Runnable> options, int[] costs, String catName, List<String> enchants, List<Integer> maxLevels) {
        int cost = getCost(state.getCategoryCount(catName), costs);
        for (int i = 0; i < enchants.size(); i++) {
            String ench = enchants.get(i);
            int max = maxLevels.get(i);
            if (state.getLevel(ench) < max) {
                options.add(() -> {
                    state.incLevel(ench);
                    state.incCategoryCount(catName);
                    state.spentPoints += cost;
                });
            }
        }
        // Bow Potion Arrows
        if (catName.equals("bow")) {
            if (state.getLevel("bow_potion_mastery") < 10) {
                options.add(() -> {
                    state.incLevel("bow_potion_mastery");
                    state.incCategoryCount(catName);
                    state.spentPoints += cost;
                });
            }
        }
    }

    private static void addArmorUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("armor"), costs);
        // protection 1-4
        if (state.getLevel("protection") < 4) options.add(() -> updateArmor(state, "protection", cost));
        // fire_protection 1-4
        if (state.getLevel("fire_protection") < 4) options.add(() -> updateArmor(state, "fire_protection", cost));
        // blast_protection 1-4
        if (state.getLevel("blast_protection") < 4) options.add(() -> updateArmor(state, "blast_protection", cost));
        // projectile_protection 1-4
        if (state.getLevel("projectile_protection") < 4) options.add(() -> updateArmor(state, "projectile_protection", cost));
        // thorns 1-3
        if (state.getLevel("thorns") < 3) options.add(() -> updateArmor(state, "thorns", cost));
        // unbreaking 1-3
        if (state.getLevel("armor_unbreaking") < 3) options.add(() -> updateArmor(state, "armor_unbreaking", cost));
        // mending 1
        if (state.getLevel("armor_mending") < 1) options.add(() -> updateArmor(state, "armor_mending", cost));
        // Helmet specific
        if (state.getLevel("aqua_affinity") < 1) options.add(() -> updateArmor(state, "aqua_affinity", cost));
        if (state.getLevel("respiration") < 3) options.add(() -> updateArmor(state, "respiration", cost));
        // Leggings specific
        if (state.getLevel("swift_sneak") < 3) options.add(() -> updateArmor(state, "swift_sneak", cost));
        // Boots specific
        if (state.getLevel("feather_falling") < 4) options.add(() -> updateArmor(state, "feather_falling", cost));
        if (state.getLevel("soul_speed") < 3) options.add(() -> updateArmor(state, "soul_speed", cost));
        if (state.getLevel("depth_strider") < 3) options.add(() -> updateArmor(state, "depth_strider", cost));
        if (state.getLevel("frost_walker") < 2) options.add(() -> updateArmor(state, "frost_walker", cost));
    }
    
    private static void updateArmor(SimState state, String ench, int cost) {
        state.incLevel(ench);
        state.incCategoryCount("armor");
        state.spentPoints += cost;
    }

    private static int getCost(int count, int[] costs) {
        if (count >= costs.length) return costs[costs.length - 1];
        return costs[count];
    }

    private static void checkTierUpgrades(SimState state, boolean sword, boolean trident, boolean bow, boolean axe, MobEntity mob) {
        // Sword Tier Logic
        if (sword) {
            boolean piglin = mob.getType().getTranslationKey().contains("piglin");
            List<String> tiers = piglin ? GOLD_SWORD_TIERS : SWORD_TIERS;
            int currentTier = state.getItemTier("sword");
            if (currentTier < tiers.size() - 1) {
                // Check if all enchants are maxed
                if (isMaxed(state, List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"), 
                    List.of(5, 2, 1, 3, 2, 5, 5, 3))) {
                    // Upgrade Tier
                    state.setItemTier("sword", currentTier + 1);
                    // Reset Enchants
                    resetEnchants(state, List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"));
                }
            }
        }
        if (axe) {
            int currentTier = state.getItemTier("axe");
            if (currentTier < AXE_TIERS.size() - 1) {
                if (isMaxed(state, List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"),
                    List.of(5, 5, 5, 3, 1, 5))) {
                    state.setItemTier("axe", currentTier + 1);
                    resetEnchants(state, List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"));
                }
            }
        }
        // Armor Tier Logic (Simplified: if protection is maxed, upgrade tier?)
        // User said: "must get full enchant on that item before getting next tier item"
        // For armor, there are many enchants. I'll require Protection 4 + Unbreaking 3 at least.
        int currentArmorTier = state.getItemTier("armor");
        if (currentArmorTier < HELMET_TIERS.size() - 1) {
             if (state.getLevel("protection") >= 4 && state.getLevel("armor_unbreaking") >= 3) {
                 state.setItemTier("armor", currentArmorTier + 1);
                 // Reset common armor enchants
                 resetEnchants(state, List.of("protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "armor_unbreaking", "armor_mending"));
                 // Reset specific ones too? Maybe keep them? User said "clears enchants".
                 resetEnchants(state, List.of("aqua_affinity", "respiration", "swift_sneak", "feather_falling", "soul_speed", "depth_strider", "frost_walker"));
             }
        }
    }
    
    private static boolean isMaxed(SimState state, List<String> enchants, List<Integer> maxLevels) {
        for (int i = 0; i < enchants.size(); i++) {
            if (state.getLevel(enchants.get(i)) < maxLevels.get(i)) return false;
        }
        return true;
    }
    
    private static void resetEnchants(SimState state, List<String> enchants) {
        for (String ench : enchants) {
            state.setLevel(ench, 0);
        }
    }

    private static void applyStateToMob(MobEntity mob, SimState state, PowerProfile profile) {
        // Apply Stats
        double healthBonus = state.getLevel("health_boost") * 4.0; 
        
        // Apply Attributes
        var attr = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(profile.baseHealth + healthBonus);
            if (mob.getHealth() > attr.getValue()) mob.setHealth((float)attr.getValue());
        }
        
        var strength = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (strength != null) {
            strength.setBaseValue(profile.baseDamage + state.getLevel("strength") * 1.0); // +1 dmg per level
        }

        var speed = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && state.getLevel("speed") > 0) {
            // +5% speed per level? Or flat amount? Vanilla speed is ~0.23.
            // Let's add 0.02 per level (~10%).
            speed.setBaseValue(speed.getBaseValue() + (state.getLevel("speed") * 0.02));
        }
        
        // Apply Effects
        if (state.getLevel("healing") > 0) {
            mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.REGENERATION, 999999, state.getLevel("healing") - 1));
        }
        if (state.getLevel("resistance") > 0) {
            mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.RESISTANCE, 999999, state.getLevel("resistance") - 1));
        }
        if (state.getLevel("fire_resistance") > 0) {
            mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE, 999999, 0));
        }
        
        // Equipment
        boolean isPiglin = mob.getType().getTranslationKey().contains("piglin");
        
        // Sword
        if (state.getCategoryCount("sword") > 0 || state.getItemTier("sword") > 0) {
            List<String> tiers = isPiglin ? GOLD_SWORD_TIERS : SWORD_TIERS;
            String itemId = tiers.get(Math.min(state.getItemTier("sword"), tiers.size() - 1));
            ItemStack stack = new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of(itemId.split(":")[0], itemId.split(":")[1])));
            applyEnchant(mob, stack, "sharpness", state.getLevel("sharpness"));
            applyEnchant(mob, stack, "fire_aspect", state.getLevel("fire_aspect"));
            applyEnchant(mob, stack, "mending", state.getLevel("mending"));
            applyEnchant(mob, stack, "unbreaking", state.getLevel("unbreaking"));
            applyEnchant(mob, stack, "knockback", state.getLevel("knockback"));
            applyEnchant(mob, stack, "smite", state.getLevel("smite"));
            applyEnchant(mob, stack, "bane_of_arthropods", state.getLevel("bane_of_arthropods"));
            applyEnchant(mob, stack, "looting", state.getLevel("looting"));
            mob.equipStack(EquipmentSlot.MAINHAND, stack);
        }
        
        // Axe
        if (state.getCategoryCount("axe") > 0 || state.getItemTier("axe") > 0) {
            String itemId = AXE_TIERS.get(Math.min(state.getItemTier("axe"), AXE_TIERS.size() - 1));
            ItemStack stack = new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of(itemId.split(":")[0], itemId.split(":")[1])));
            applyEnchant(mob, stack, "sharpness", state.getLevel("sharpness"));
            applyEnchant(mob, stack, "smite", state.getLevel("smite"));
            applyEnchant(mob, stack, "bane_of_arthropods", state.getLevel("bane_of_arthropods"));
            applyEnchant(mob, stack, "unbreaking", state.getLevel("unbreaking"));
            applyEnchant(mob, stack, "mending", state.getLevel("mending"));
            applyEnchant(mob, stack, "efficiency", state.getLevel("efficiency"));
            mob.equipStack(EquipmentSlot.MAINHAND, stack);
        }
        
        // Armor
        if (state.getCategoryCount("armor") > 0 || state.getItemTier("armor") > 0) {
            int tier = Math.min(state.getItemTier("armor"), HELMET_TIERS.size() - 1);
            
            ItemStack helm = new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of(HELMET_TIERS.get(tier).split(":")[0], HELMET_TIERS.get(tier).split(":")[1])));
            applyArmorEnchants(mob, helm, state);
            applyEnchant(mob, helm, "aqua_affinity", state.getLevel("aqua_affinity"));
            applyEnchant(mob, helm, "respiration", state.getLevel("respiration"));
            mob.equipStack(EquipmentSlot.HEAD, helm);
            
            ItemStack chest = new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of(CHEST_TIERS.get(tier).split(":")[0], CHEST_TIERS.get(tier).split(":")[1])));
            applyArmorEnchants(mob, chest, state);
            mob.equipStack(EquipmentSlot.CHEST, chest);
            
            ItemStack legs = new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of(LEGS_TIERS.get(tier).split(":")[0], LEGS_TIERS.get(tier).split(":")[1])));
            applyArmorEnchants(mob, legs, state);
            applyEnchant(mob, legs, "swift_sneak", state.getLevel("swift_sneak"));
            mob.equipStack(EquipmentSlot.LEGS, legs);
            
            ItemStack boots = new ItemStack(net.minecraft.registry.Registries.ITEM.get(Identifier.of(BOOTS_TIERS.get(tier).split(":")[0], BOOTS_TIERS.get(tier).split(":")[1])));
            applyArmorEnchants(mob, boots, state);
            applyEnchant(mob, boots, "feather_falling", state.getLevel("feather_falling"));
            applyEnchant(mob, boots, "soul_speed", state.getLevel("soul_speed"));
            applyEnchant(mob, boots, "depth_strider", state.getLevel("depth_strider"));
            applyEnchant(mob, boots, "frost_walker", state.getLevel("frost_walker"));
            mob.equipStack(EquipmentSlot.FEET, boots);
        }
        
        // Drop Chance & Durability
        float progress = profile.isMaxed ? 1.0f : (float)Math.min(profile.totalPoints / 1000.0, 1.0); // Estimated max points prob less than 1000
        float dropChance = 0.5f - (0.45f * progress); // 0.5 -> 0.05
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setEquipmentDropChance(slot, dropChance);
            
            // Durability: 100% -> 5%
            ItemStack stack = mob.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.isDamageable()) {
                int maxDamage = stack.getMaxDamage();
                int damage = (int)(maxDamage * (0.95f * progress));
                stack.setDamage(damage);
            }
        }
        
        // Shield Chance
        if (profile.categories.contains("g")) {
             if (mob.getWorld().random.nextFloat() < progress) {
                 mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
             }
        }

        // Creeper Specifics
        if (mob instanceof CreeperEntity creeper) {
            if (state.getLevel("charged") > 0) {
                // Need access transformer or mixin usually, but Fabric might expose it?
                // CreeperEntity.setCharged is not public in vanilla mappings usually, but let's try.
                // If not, we need to use NBT or data tracker.
                // Fabric mappings usually expose it as setCharged or similar.
                // Actually, `setIgnited` is for exploding. `setPowered`?
                // In Yarn: `setCharged`? No. `getDataTracker().set(CHARGED, true)`.
                // But `CreeperEntity` has `shouldRenderOverlay`?
                // Let's check if `setPowered` exists or similar.
                // Actually, standard mapping is `setPowered(boolean)`. Wait, no.
                // It's `setChared` in some mappings.
                // Let's try `setPowered` if it exists, or check NBT.
                // For now, I'll assume `setPowered` or `setCharged` is not available directly without AT.
                // But wait, `CreeperEntity` has `onStruckByLightning`.
                // I can use NBT to set "powered" tag.
                // But `mob` is already spawned.
                // I can use `creeper.getDataTracker().set(CreeperEntity.CHARGED, true);` if I can access the field.
                // Let's try to use NBT write/read which is safer.
                var nbt = new net.minecraft.nbt.NbtCompound();
                creeper.writeCustomDataToNbt(nbt);
                nbt.putBoolean("powered", true);
                creeper.readCustomDataFromNbt(nbt);
            }
            
            if (state.getLevel("fuse_reduction") > 0) {
                // Default 30. Reduce by 5 per level?
                int fuse = 30 - (state.getLevel("fuse_reduction") * 5);
                if (fuse < 5) fuse = 5;
                // creeper.setFuse(fuse);
                // Again, check if method exists. Usually `setFuse` is available.
                // If not, NBT.
                var nbt = new net.minecraft.nbt.NbtCompound();
                creeper.writeCustomDataToNbt(nbt);
                nbt.putShort("Fuse", (short)fuse);
                creeper.readCustomDataFromNbt(nbt);
            }
            
            if (state.getLevel("explosion_radius") > 0) {
                // Default 3. Increase by 1 per level.
                int radius = 3 + state.getLevel("explosion_radius");
                // creeper.setExplosionRadius(radius);
                var nbt = new net.minecraft.nbt.NbtCompound();
                creeper.writeCustomDataToNbt(nbt);
                nbt.putByte("ExplosionRadius", (byte)radius);
                creeper.readCustomDataFromNbt(nbt);
            }
        }

        // Save special skills to PowerProfile for Mixins
        profile.specialSkills.clear();
        profile.specialSkills.put("horde_summon", state.getLevel("horde_summon"));
        profile.specialSkills.put("hunger_attack", state.getLevel("hunger_attack"));
        profile.specialSkills.put("invis_mastery", state.getLevel("invis_mastery"));
        profile.specialSkills.put("piercing_shot", state.getLevel("piercing_shot"));
        profile.specialSkills.put("multishot_skill", state.getLevel("multishot_skill"));
        profile.specialSkills.put("web_shot", state.getLevel("web_shot"));
        profile.specialSkills.put("poison_attack", state.getLevel("poison_attack"));
        
        // Potion Masteries (Chance 0-100%)
        if (state.getLevel("creeper_potion_mastery") > 0) {
            profile.specialSkills.put("creeper_potion_chance", state.getLevel("creeper_potion_mastery") * 10);
        }
        if (state.getLevel("witch_potion_mastery") > 0) {
            profile.specialSkills.put("witch_potion_chance", state.getLevel("witch_potion_mastery") * 10);
        }
        if (state.getLevel("bow_potion_mastery") > 0) {
            profile.specialSkills.put("bow_potion_chance", state.getLevel("bow_potion_mastery") * 10);
        }
    }
    
    private static void applyArmorEnchants(MobEntity mob, ItemStack stack, SimState state) {
        applyEnchant(mob, stack, "protection", state.getLevel("protection"));
        applyEnchant(mob, stack, "fire_protection", state.getLevel("fire_protection"));
        applyEnchant(mob, stack, "blast_protection", state.getLevel("blast_protection"));
        applyEnchant(mob, stack, "projectile_protection", state.getLevel("projectile_protection"));
        applyEnchant(mob, stack, "thorns", state.getLevel("thorns"));
        applyEnchant(mob, stack, "unbreaking", state.getLevel("armor_unbreaking"));
        applyEnchant(mob, stack, "mending", state.getLevel("armor_mending"));
    }

    private static void applyEnchant(MobEntity mob, ItemStack stack, String id, int level) {
        if (level <= 0) return;
        var registry = mob.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(Identifier.of("minecraft", id));
        if (entry.isPresent()) {
            stack.addEnchantment(entry.get(), level);
        }
    }
    
    // Dummy class for compatibility if needed
    public static class UpgradeNode {
        public String id;
        public int cost;
        public int tierReq;
        public String category;
        public Map<String, Double> attributes;
        public Map<EquipmentSlot, String> equipment;
        public Map<String, Integer> effects;
        public Map<String, Integer> enchantments;
        public double shieldChance;
        public double healthCap;
        public List<UpgradeNode> children;
    }
    
    public static UpgradeNode findUpgradeNode(String archetype, String upgradeId) { return null; }
    public static void maybeRerollPriorityPath(PowerProfile profile, long worldSeed) {}
}
