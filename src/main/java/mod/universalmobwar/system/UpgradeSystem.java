package mod.universalmobwar.system;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.*;
import mod.universalmobwar.data.PowerProfile;

public class UpgradeSystem {

    // Cost arrays
    private static final int[] GENERAL_COSTS = {2}; 
    private static final int[] GENERAL_PASSIVE_COSTS = {2};
    private static final int[] SWORD_COSTS = {1, 1, 2, 2, 3, 3, 4, 4, 5};
    private static final int[] TRIDENT_COSTS = {3};
    private static final int[] BOW_COSTS = {2, 2, 2, 3, 3, 3, 3};
    private static final int[] ARMOR_COSTS = {2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5};
    private static final int[] ZOMBIE_COSTS = {3};
    private static final int[] PROJECTILE_COSTS = {2};
    private static final int[] CAVE_SPIDER_COSTS = {3};
    private static final int[] CREEPER_COSTS = {3};
    private static final int[] WITCH_COSTS = {3}; // Assumed 3

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

    public static void applyUpgrades(MobEntity mob, PowerProfile profile) {
        SimState state = simulate(mob, profile);
        applyStateToMob(mob, state, profile);
    }

    private static SimState simulate(MobEntity mob, PowerProfile profile) {
        SimState state = new SimState();
        double totalPoints = profile.totalPoints;
        long seed = mob.getUuid().hashCode() ^ (long)totalPoints; 
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
        
        boolean useSword = isG && !isBow && !isTrident && !isAxe && !isNW && !isWitch;
        boolean useAxe = isAxe;
        
        // Main loop
        while (state.spentPoints < totalPoints) {
            List<Runnable> possibleUpgrades = new ArrayList<>();
            
            if (isG) addGeneralUpgrades(state, possibleUpgrades, GENERAL_COSTS);
            if (isGP) addGeneralPassiveUpgrades(state, possibleUpgrades, GENERAL_PASSIVE_COSTS);
            if (isZ) addZombieUpgrades(state, possibleUpgrades, ZOMBIE_COSTS);
            if (isPro) addProjectileUpgrades(state, possibleUpgrades, PROJECTILE_COSTS);
            if (isCaveSpider) addCaveSpiderUpgrades(state, possibleUpgrades, CAVE_SPIDER_COSTS);
            if (isCreeper) addCreeperUpgrades(state, possibleUpgrades, CREEPER_COSTS);
            if (isWitch) addWitchUpgrades(state, possibleUpgrades, WITCH_COSTS);
            
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
                addWeaponUpgrades(state, possibleUpgrades, SWORD_COSTS, "axe",
                    List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"),
                    List.of(5, 5, 5, 3, 1, 5));
            }
            
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
        
        if (state.getLevel("healing") < 5) addOpt(options, state, "healing", "g", cost);
        if (state.getLevel("health_boost") < 10) addOpt(options, state, "health_boost", "g", cost);
        if (state.getLevel("resistance") < 4) addOpt(options, state, "resistance", "g", cost);
        if (state.getLevel("invis_mastery") < 10) addOpt(options, state, "invis_mastery", "g", cost);
        if (state.getLevel("strength") < 4) addOpt(options, state, "strength", "g", cost);
    }

    private static void addGeneralPassiveUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("gp"), costs);
        if (state.getLevel("healing") < 3) addOpt(options, state, "healing", "gp", cost);
        if (state.getLevel("health_boost") < 3) addOpt(options, state, "health_boost", "gp", cost);
        if (state.getLevel("resistance") < 1) addOpt(options, state, "resistance", "gp", cost);
    }

    private static void addZombieUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("z"), costs);
        if (state.getLevel("hunger_attack") < 3) addOpt(options, state, "hunger_attack", "z", cost);
        if (state.getLevel("horde_summon") < 8) addOpt(options, state, "horde_summon", "z", cost);
    }

    private static void addProjectileUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("pro"), costs);
        if (state.getLevel("piercing_shot") < 5) addOpt(options, state, "piercing_shot", "pro", cost);
        if (state.getLevel("multishot_skill") < 4) addOpt(options, state, "multishot_skill", "pro", cost);
    }

    private static void addCaveSpiderUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("cave_spider"), costs);
        if (state.getLevel("poison_attack") < 2) addOpt(options, state, "poison_attack", "cave_spider", cost);
    }

    private static void addCreeperUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("creeper"), costs);
        if (state.getLevel("creeper_potion_mastery") < 10) addOpt(options, state, "creeper_potion_mastery", "creeper", cost);
    }

    private static void addWitchUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("witch"), costs);
        if (state.getLevel("witch_potion_mastery") < 10) addOpt(options, state, "witch_potion_mastery", "witch", cost);
    }

    private static void addWeaponUpgrades(SimState state, List<Runnable> options, int[] costs, String catName, List<String> enchants, List<Integer> maxLevels) {
        int cost = getCost(state.getCategoryCount(catName), costs);
        for (int i = 0; i < enchants.size(); i++) {
            String ench = enchants.get(i);
            int max = maxLevels.get(i);
            if (state.getLevel(ench) < max) {
                addOpt(options, state, ench, catName, cost);
            }
        }
        // Bow Potion Arrows
        if (catName.equals("bow")) {
            if (state.getLevel("bow_potion_mastery") < 10) {
                addOpt(options, state, "bow_potion_mastery", catName, cost);
            }
        }
    }

    private static void addArmorUpgrades(SimState state, List<Runnable> options, int[] costs) {
        int cost = getCost(state.getCategoryCount("armor"), costs);
        if (state.getLevel("protection") < 4) addOpt(options, state, "protection", "armor", cost);
        if (state.getLevel("fire_protection") < 4) addOpt(options, state, "fire_protection", "armor", cost);
        if (state.getLevel("blast_protection") < 4) addOpt(options, state, "blast_protection", "armor", cost);
        if (state.getLevel("projectile_protection") < 4) addOpt(options, state, "projectile_protection", "armor", cost);
        if (state.getLevel("thorns") < 3) addOpt(options, state, "thorns", "armor", cost);
        if (state.getLevel("armor_unbreaking") < 3) addOpt(options, state, "armor_unbreaking", "armor", cost);
        if (state.getLevel("armor_mending") < 1) addOpt(options, state, "armor_mending", "armor", cost);
        
        if (state.getLevel("aqua_affinity") < 1) addOpt(options, state, "aqua_affinity", "armor", cost);
        if (state.getLevel("respiration") < 3) addOpt(options, state, "respiration", "armor", cost);
        if (state.getLevel("swift_sneak") < 3) addOpt(options, state, "swift_sneak", "armor", cost);
        if (state.getLevel("feather_falling") < 4) addOpt(options, state, "feather_falling", "armor", cost);
        if (state.getLevel("soul_speed") < 3) addOpt(options, state, "soul_speed", "armor", cost);
        if (state.getLevel("depth_strider") < 3) addOpt(options, state, "depth_strider", "armor", cost);
        if (state.getLevel("frost_walker") < 2) addOpt(options, state, "frost_walker", "armor", cost);
    }
    
    private static void addOpt(List<Runnable> options, SimState state, String id, String cat, int cost) {
        options.add(() -> {
            state.incLevel(id);
            state.incCategoryCount(cat);
            state.spentPoints += cost;
        });
    }

    private static int getCost(int count, int[] costs) {
        if (count >= costs.length) return costs[costs.length - 1];
        return costs[count];
    }

    private static void checkTierUpgrades(SimState state, boolean sword, boolean trident, boolean bow, boolean axe, MobEntity mob) {
        if (sword) {
            boolean piglin = mob.getType().getTranslationKey().contains("piglin");
            List<String> tiers = piglin ? GOLD_SWORD_TIERS : SWORD_TIERS;
            int currentTier = state.getItemTier("sword");
            if (currentTier < tiers.size() - 1) {
                if (isMaxed(state, List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"), 
                    List.of(5, 2, 1, 3, 2, 5, 5, 3))) {
                    state.setItemTier("sword", currentTier + 1);
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
        // Armor Tier Logic
        int currentArmorTier = state.getItemTier("armor");
        if (currentArmorTier < HELMET_TIERS.size() - 1) {
             // Require full enchants for armor too? "must get full enchant on that item before getting next tier item"
             // This implies ALL armor enchants.
             if (isMaxed(state, List.of("protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "armor_unbreaking", "armor_mending",
                 "aqua_affinity", "respiration", "swift_sneak", "feather_falling", "soul_speed", "depth_strider", "frost_walker"),
                 List.of(4, 4, 4, 4, 3, 3, 1, 1, 3, 3, 4, 3, 3, 2))) {
                 
                 state.setItemTier("armor", currentArmorTier + 1);
                 resetEnchants(state, List.of("protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "armor_unbreaking", "armor_mending",
                     "aqua_affinity", "respiration", "swift_sneak", "feather_falling", "soul_speed", "depth_strider", "frost_walker"));
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
        
        var attr = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(profile.baseHealth + healthBonus);
            if (mob.getHealth() > attr.getValue()) mob.setHealth((float)attr.getValue());
        }
        
        var strength = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (strength != null) {
            strength.setBaseValue(profile.baseDamage + state.getLevel("strength") * 1.0); 
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
        if (state.getLevel("resistance") >= 4) { // Level 4 gives Fire Resis 1
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
        // Estimated max points for a full tree is around 350.
        float progress = profile.isMaxed ? 1.0f : (float)Math.min(profile.totalPoints / 350.0, 1.0); 
        
        // Drop Chance: 50% -> 40% -> 30% -> 20% -> 10% -> 5%
        float dropChance = 0.5f;
        if (progress >= 1.0f) dropChance = 0.05f;
        else if (progress >= 0.8f) dropChance = 0.10f;
        else if (progress >= 0.6f) dropChance = 0.20f;
        else if (progress >= 0.4f) dropChance = 0.30f;
        else if (progress >= 0.2f) dropChance = 0.40f;
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setEquipmentDropChance(slot, dropChance);
            
            // Durability: 100% -> 5% (Damage: 0% -> 95%)
            ItemStack stack = mob.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.isDamageable()) {
                int maxDamage = stack.getMaxDamage();
                int damage = (int)(maxDamage * (0.95f * progress));
                stack.setDamage(damage);
            }
        }
        
        // Shield Chance: 0% -> 100% (Normal curve)
        if (profile.categories.contains("g")) {
             // Normal curve centered around progress? Or just linear probability?
             // "normal curve from 0% to 100% so synced around when mobs get full max"
             // This implies probability increases with progress.
             // Let's use a sigmoid or just linear for simplicity, or actual normal distribution sample?
             // "normal curve" usually means Bell curve. But probability 0 to 100 implies cumulative distribution function (CDF) of normal curve?
             // Or just that the probability follows a normal distribution shape?
             // I'll use a CDF-like curve: low at start, steep in middle, high at end.
             // Sigmoid: 1 / (1 + e^(-k(x - x0)))
             double p = 1.0 / (1.0 + Math.exp(-10.0 * (progress - 0.5)));
             if (mob.getRandom().nextDouble() < p) {
                 mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
             }
        }

        // Creeper Specifics
        if (mob instanceof CreeperEntity creeper) {
            // NBT manipulation for creeper properties
            NbtCompound nbt = new NbtCompound();
            creeper.writeCustomDataToNbt(nbt);
            // No charged creeper in new spec? "lingering potions on explode..."
            // Prompt doesn't mention charged creeper.
            // But it mentions "lingering potions on explode".
            // I will handle that in Mixin.
        }

        // Save special skills to PowerProfile for Mixins
        profile.specialSkills.clear();
        profile.specialSkills.put("horde_summon", state.getLevel("horde_summon"));
        profile.specialSkills.put("hunger_attack", state.getLevel("hunger_attack"));
        profile.specialSkills.put("invis_mastery", state.getLevel("invis_mastery"));
        profile.specialSkills.put("piercing_shot", state.getLevel("piercing_shot"));
        profile.specialSkills.put("multishot_skill", state.getLevel("multishot_skill"));
        profile.specialSkills.put("poison_attack", state.getLevel("poison_attack"));
        
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
    
    public static class UpgradeNode {
        // Deprecated
    }
    
    public static UpgradeNode findUpgradeNode(String archetype, String upgradeId) { return null; }
    public static void maybeRerollPriorityPath(PowerProfile profile, long worldSeed) {}
}
