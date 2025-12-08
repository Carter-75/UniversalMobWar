package mod.universalmobwar.system;

import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.*;
import mod.universalmobwar.data.PowerProfile;

public class UpgradeSystem {

    // Cost arrays (modernized, all costs configurable via ModConfig)
    private static final int[] GENERAL_COSTS = ModConfig.getInstance().generalUpgradeCosts;
    private static final int[] GENERAL_PASSIVE_COSTS = ModConfig.getInstance().generalPassiveUpgradeCosts;
    private static final int[] SWORD_COSTS = ModConfig.getInstance().swordUpgradeCosts;
    private static final int[] TRIDENT_COSTS = ModConfig.getInstance().tridentUpgradeCosts;
    private static final int[] BOW_COSTS = ModConfig.getInstance().bowUpgradeCosts;
    private static final int[] ARMOR_COSTS = ModConfig.getInstance().armorUpgradeCosts;
    private static final int[] ZOMBIE_COSTS = ModConfig.getInstance().zombieUpgradeCosts;
    private static final int[] PROJECTILE_COSTS = ModConfig.getInstance().projectileUpgradeCosts;
    private static final int[] CREEPER_COSTS = ModConfig.getInstance().creeperUpgradeCosts;
    private static final int[] WITCH_COSTS = ModConfig.getInstance().witchUpgradeCosts;
    private static final int[] CAVE_SPIDER_COSTS = ModConfig.getInstance().caveSpiderUpgradeCosts;

    // Item Tiers (modernized, all tiers configurable via ModConfig)
    public static final List<String> SWORD_TIERS = ModConfig.getInstance().swordTiers;
    public static final List<String> GOLD_SWORD_TIERS = ModConfig.getInstance().goldSwordTiers;
    public static final List<String> AXE_TIERS = ModConfig.getInstance().axeTiers;
    public static final List<String> GOLD_AXE_TIERS = ModConfig.getInstance().goldAxeTiers;
    public static final List<String> HELMET_TIERS = ModConfig.getInstance().helmetTiers;
    public static final List<String> CHEST_TIERS = ModConfig.getInstance().chestTiers;
    public static final List<String> LEGS_TIERS = ModConfig.getInstance().legsTiers;
    public static final List<String> BOOTS_TIERS = ModConfig.getInstance().bootsTiers;

    public static class SimState {
        public Map<String, Integer> levels = new HashMap<>();
        public Map<String, Integer> categoryCounts = new HashMap<>();
        public Map<String, Integer> itemTiers = new HashMap<>();
        public double spentPoints = 0;
        
        public int getLevel(String id) { return levels.getOrDefault(id, 0); }
        public void setLevel(String id, int val) { levels.put(id, val); }
        public void incLevel(String id) { levels.put(id, getLevel(id) + 1); }
        
        public int getCategoryCount(String cat) { return categoryCounts.getOrDefault(cat, 0); }
        public void incCategoryCount(String cat) { categoryCounts.put(cat, getCategoryCount(cat) + 1); }
        
        public int getItemTier(String type) { return itemTiers.getOrDefault(type, 0); }
        public void setItemTier(String type, int val) { itemTiers.put(type, val); }
    }

    public static class SimulationContext {
        public final long seed;
        public final String translationKey;
        public final Set<String> tags;
        public final Set<String> categories;
        
        public SimulationContext(MobEntity mob, double totalPoints) {
            this.seed = mob.getUuid().hashCode() ^ (long)totalPoints;
            this.translationKey = mob.getType().getTranslationKey();
            this.tags = new HashSet<>(mob.getCommandTags());
            // Resolve archetype categories here so downstream checks use deterministic categories
            this.categories = ArchetypeClassifier.getMobCategories(mob);
        }
    }
    
    private static class UpgradeCollector {
        final List<String> ids = new ArrayList<>();
        final List<String> cats = new ArrayList<>();
        final List<String> groups = new ArrayList<>();
        final List<Integer> costs = new ArrayList<>();
        final List<Integer> weights = new ArrayList<>();
        
        void clear() {
            ids.clear();
            cats.clear();
            groups.clear();
            costs.clear();
            weights.clear();
        }
        
        void add(String id, String cat, String group, int cost, int weight) {
            ids.add(id);
            cats.add(cat);
            groups.add(group);
            costs.add(cost);
            weights.add(weight);
        }
        
        boolean isEmpty() {
            return ids.isEmpty();
        }
        
        int size() {
            return ids.size();
        }
        
        void apply(int index, SimState state) {
            state.incLevel(ids.get(index));
            state.incCategoryCount(cats.get(index));
            state.spentPoints += costs.get(index);
        }
    }

    public static void applyUpgrades(MobEntity mob, PowerProfile profile) {
        // Synchronous fallback if async is not used
        SimState state = simulateWithDebug(mob, profile);
        applyStateToMob(mob, state, profile);
    }

    // Perform one incremental upgrade step (for tick-based progression)
    public static void performOneStep(MobEntity mob, MobWarData data) {
        PowerProfile profile = data.getPowerProfile();
        if (profile == null || profile.totalPoints <= data.getSpentPoints()) return;

        // Instantly apply all upgrades up to totalPoints
        SimState state = loadStateFromProfile(profile);
        SimulationContext context = new SimulationContext(mob, profile.totalPoints);
        double availablePoints = profile.totalPoints - data.getSpentPoints();
        int safety = 0;
        while (availablePoints > 0 && safety++ < 1000) {
            UpgradeCollector collector = new UpgradeCollector();
            collectOptions(state, collector, context, profile);
            if (collector.isEmpty()) {
                profile.isMaxed = true;
                break;
            }
            // Find affordable upgrades
            List<Integer> affordable = new ArrayList<>();
            for (int i = 0; i < collector.size(); i++) {
                double cost = collector.costs.get(i);
                if (cost <= availablePoints) {
                    affordable.add(i);
                }
            }
            if (affordable.isEmpty()) break;
            // Pick one randomly
            int index = affordable.get(mob.getRandom().nextInt(affordable.size()));
            collector.apply(index, state);
            checkTierUpgrades(state, context);
            availablePoints -= collector.costs.get(index);
        }
        // Update spent points
        data.setSpentPoints(profile.totalPoints);
        // Save state back to profile
        saveStateToProfile(state, profile);
        data.setSkillData(profile.writeNbt());
        // Apply to mob immediately
        applyStateToMob(mob, state, profile);
        // Debug logging
        if (ModConfig.getInstance().debugUpgradeLog && mob.getWorld() instanceof ServerWorld sw) {
            MinecraftServer server = sw.getServer();
            String msg = String.format("[UMW] Upgrades applied instantly to %s (total spent: %.1f/%.1f)", 
                mob.getType().getTranslationKey(), (double)profile.totalPoints, (double)profile.totalPoints);
            Text text = Text.literal(msg);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.sendMessage(text, false);
            }
        }
    }

    // Simulate with debug logging if enabled
    private static SimState simulateWithDebug(MobEntity mob, PowerProfile profile) {
        boolean debug = ModConfig.getInstance().debugUpgradeLog;
        SimulationContext context = new SimulationContext(mob, profile.totalPoints);
        SimState state = new SimState();
        double totalPoints = profile.totalPoints;
        Random rand = new Random(context.seed);
        int safety = 0;
        UpgradeCollector collector = new UpgradeCollector();
        List<String> debugLog = debug ? new ArrayList<>() : null;
        while (state.spentPoints < totalPoints && safety++ < 1000000) {
            collector.clear();
            collectOptions(state, collector, context, profile);
            if (collector.isEmpty()) {
                profile.isMaxed = true;
                if (debug) debugLog.add("No more upgrade options. Maxed out.");
                break;
            }
            // Filter affordable options
            List<Integer> affordable = new ArrayList<>();
            double remaining = totalPoints - state.spentPoints;
            Map<String, Double> groupTotalBaseWeight = new HashMap<>();
            for (int i = 0; i < collector.size(); i++) {
                if (collector.costs.get(i) <= remaining) {
                    affordable.add(i);
                    String g = collector.groups.get(i);
                    double w = collector.weights.get(i);
                    groupTotalBaseWeight.put(g, groupTotalBaseWeight.getOrDefault(g, 0.0) + w);
                }
            }
            if (affordable.isEmpty()) {
                if (debug) debugLog.add("No affordable upgrades left. Stopping.");
                break;
            }
            // Calculate selection weights (Equal probability per group)
            double totalSelectionWeight = 0;
            Map<Integer, Double> selectionWeights = new HashMap<>();
            double TARGET_GROUP_WEIGHT = 100.0;
            for (int i : affordable) {
                String g = collector.groups.get(i);
                double base = collector.weights.get(i);
                double groupTotal = groupTotalBaseWeight.get(g);
                double w = (base / groupTotal) * TARGET_GROUP_WEIGHT;
                selectionWeights.put(i, w);
                totalSelectionWeight += w;
            }
            // Pick random weighted
            double r = rand.nextDouble() * totalSelectionWeight;
            double currentWeight = 0;
            int index = -1;
            for (int i : affordable) {
                currentWeight += selectionWeights.get(i);
                if (r < currentWeight) {
                    index = i;
                    break;
                }
            }
            if (index == -1) index = affordable.get(affordable.size() - 1); // Fallback
            if (debug) {
                StringBuilder sb = new StringBuilder();
                sb.append("[Mob: ").append(mob.getType().getTranslationKey()).append("] ");
                sb.append("Points: ").append(state.spentPoints).append("/").append(totalPoints).append(" | ");
                sb.append("Upgrade: ").append(collector.ids.get(index)).append(" (cat: ").append(collector.cats.get(index)).append(", group: ").append(collector.groups.get(index)).append(", cost: ").append(collector.costs.get(index)).append(")");
                debugLog.add(sb.toString());
            }
            collector.apply(index, state);
            checkTierUpgrades(state, context);
        }
        if (debug && mob.getWorld() instanceof ServerWorld sw) {
            MinecraftServer server = sw.getServer();
            String mobName = mob.getType().getTranslationKey();
            // Write to chat
            for (String msg : debugLog) {
                Text text = Text.literal("[UMW Debug] " + msg);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(text, false);
                }
            }
            // Write to file (append)
            try {
                Path logPath = FabricLoader.getInstance().getConfigDir().resolve("upgrade_debug.log");
                Files.write(logPath, debugLog, java.nio.charset.StandardCharsets.UTF_8,
                    Files.exists(logPath) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            } catch (Exception e) {
                // Ignore file write errors
            }
        }
        return state;
    }
    
    public static java.util.concurrent.CompletableFuture<SimState> simulateAsync(MobEntity mob, PowerProfile profile) {
        SimulationContext context = new SimulationContext(mob, profile.totalPoints);
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> simulate(context, profile));
    }

    public static SimState simulate(SimulationContext context, PowerProfile profile) {
        SimState state = new SimState();
        double totalPoints = profile.totalPoints;
        Random rand = new Random(context.seed);

        // Main loop
        int safety = 0;
        UpgradeCollector collector = new UpgradeCollector();
        while (state.spentPoints < totalPoints && safety++ < 1000000) {
            collector.clear();
            collectOptions(state, collector, context, profile);

            if (collector.isEmpty()) {
                profile.isMaxed = true;
                break;
            }

            // Filter affordable options
            List<Integer> affordable = new ArrayList<>();
            double remaining = totalPoints - state.spentPoints;
            
            Map<String, Double> groupTotalBaseWeight = new HashMap<>();
            
            for (int i = 0; i < collector.size(); i++) {
                if (collector.costs.get(i) <= remaining) {
                    affordable.add(i);
                    String g = collector.groups.get(i);
                    double w = collector.weights.get(i);
                    groupTotalBaseWeight.put(g, groupTotalBaseWeight.getOrDefault(g, 0.0) + w);
                }
            }

            if (affordable.isEmpty()) break;

            // Calculate selection weights (Equal probability per group)
            double totalSelectionWeight = 0;
            Map<Integer, Double> selectionWeights = new HashMap<>();
            double TARGET_GROUP_WEIGHT = 100.0;
            
            for (int i : affordable) {
                String g = collector.groups.get(i);
                double base = collector.weights.get(i);
                double groupTotal = groupTotalBaseWeight.get(g);
                
                // w_i = (base / groupTotal) * TARGET
                double w = (base / groupTotal) * TARGET_GROUP_WEIGHT;
                
                selectionWeights.put(i, w);
                totalSelectionWeight += w;
            }

            // Pick random weighted
            double r = rand.nextDouble() * totalSelectionWeight;
            double currentWeight = 0;
            int index = -1;
            
            for (int i : affordable) {
                currentWeight += selectionWeights.get(i);
                if (r < currentWeight) {
                    index = i;
                    break;
                }
            }
            
            if (index == -1) index = affordable.get(affordable.size() - 1); // Fallback
            
            collector.apply(index, state);
            
            // Check for Tier Upgrades
            checkTierUpgrades(state, context);
        }
        
        return state;
    }

    private static void collectOptions(SimState state, UpgradeCollector collector, SimulationContext context, PowerProfile profile) {
        Set<String> cats = profile.categories;
        boolean isG = cats.contains("g");
        boolean isGP = cats.contains("gp");
        boolean isZ = cats.contains("z");
        boolean isPro = cats.contains("pro");
        boolean isBow = cats.contains("bow");
        boolean isTrident = cats.contains("trident");
        boolean isAxe = cats.contains("axe");
        boolean isNW = cats.contains("nw");
        boolean isWitch = context.translationKey.contains("witch");
        boolean isCreeper = context.translationKey.contains("creeper");
        boolean isCaveSpider = context.translationKey.contains("cave_spider");
        boolean isPiglinBrute = context.translationKey.contains("piglin_brute");
        
        boolean useSword = isG && !isBow && !isTrident && !isAxe && !isNW && !isWitch && !isPiglinBrute;
        boolean useAxe = isAxe || isPiglinBrute;

        if (isG) addGeneralUpgrades(state, collector, GENERAL_COSTS);
        if (isGP) addGeneralPassiveUpgrades(state, collector, GENERAL_PASSIVE_COSTS);
        if (isZ) addZombieUpgrades(state, collector, ZOMBIE_COSTS, context);
        if (isPro) addProjectileUpgrades(state, collector, PROJECTILE_COSTS);
        if (isCreeper) addCreeperUpgrades(state, collector, CREEPER_COSTS);
        if (isWitch) addWitchUpgrades(state, collector, WITCH_COSTS);
        if (isCaveSpider) addCaveSpiderUpgrades(state, collector, CAVE_SPIDER_COSTS);
        
        if (useSword) {
            addWeaponUpgrades(state, collector, SWORD_COSTS, "sword", 
                List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"),
                List.of(5, 2, 1, 3, 2, 5, 5, 3));
        }
        if (isTrident) {
            addWeaponUpgrades(state, collector, TRIDENT_COSTS, "trident",
                List.of("impaling", "channeling", "unbreaking", "mending", "loyalty", "riptide"),
                List.of(5, 1, 3, 1, 3, 3));
        }
        if (isBow) {
            addWeaponUpgrades(state, collector, BOW_COSTS, "bow",
                List.of("power", "flame", "punch", "infinity", "unbreaking", "mending"),
                List.of(5, 1, 2, 1, 3, 1));
        }
        if (useAxe) {
            addWeaponUpgrades(state, collector, SWORD_COSTS, "axe",
                List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"),
                List.of(5, 5, 5, 3, 1, 5));
        }
        
        if (isG) {
            addArmorUpgrades(state, collector, ARMOR_COSTS);
        }
        
        // Equipment Stats (Durability & Drop Chance)
        boolean hasMain = useSword || useAxe || isBow || isTrident;
        boolean hasOff = state.getLevel("shield_chance") > 0;
        boolean hasArmor = isG;
        addEquipmentStatsUpgrades(state, collector, hasMain, hasOff, hasArmor);
    }

    private static void checkTierUpgrades(SimState state, SimulationContext context) {
        boolean isPiglinBrute = context.translationKey.contains("piglin_brute");
        boolean isWitch = context.translationKey.contains("witch");
        boolean isNW = context.categories != null && context.categories.contains("nw");
        // Re-derive flags or pass them? 
        // For simplicity, we'll re-derive basic ones.
        // Actually, checkTierUpgrades needs to know if it uses sword/axe etc.
        // Let's just check all categories in state.
        
        if (state.getCategoryCount("sword") > 0) {
             boolean piglin = context.translationKey.contains("piglin");
             List<String> tiers = piglin ? GOLD_SWORD_TIERS : SWORD_TIERS;
             int currentTier = state.getItemTier("sword");
             if (currentTier < tiers.size() - 1) {
                 if (isMaxed(state, List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"), 
                     List.of(5, 2, 1, 3, 2, 5, 5, 3))) {
                     state.setItemTier("sword", currentTier + 1);
                     resetEnchants(state, List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting"));
                     state.setLevel("durability_mainhand", 0);
                     state.setLevel("drop_chance_mainhand", 0);
                 }
             }
        }
        
        if (state.getCategoryCount("axe") > 0) {
            boolean piglin = context.translationKey.contains("piglin");
            List<String> tiers = piglin ? GOLD_AXE_TIERS : AXE_TIERS;
            int currentTier = state.getItemTier("axe");
            if (currentTier < tiers.size() - 1) {
                if (isMaxed(state, List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"),
                    List.of(5, 5, 5, 3, 1, 5))) {
                    state.setItemTier("axe", currentTier + 1);
                    resetEnchants(state, List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "efficiency"));
                    state.setLevel("durability_mainhand", 0);
                    state.setLevel("drop_chance_mainhand", 0);
                }
            }
        }
        
        // Armor Tier Logic (Per Item)
        checkArmorTier(state, "head", HELMET_TIERS, List.of("aqua_affinity", "respiration"), List.of(1, 3));
        checkArmorTier(state, "chest", CHEST_TIERS, List.of(), List.of());
        checkArmorTier(state, "legs", LEGS_TIERS, List.of("swift_sneak"), List.of(3));
        checkArmorTier(state, "feet", BOOTS_TIERS, List.of("feather_falling", "soul_speed", "depth_strider", "frost_walker"), List.of(4, 3, 3, 2));
    }

    public static SimState loadStateFromProfile(PowerProfile profile) {
        SimState state = new SimState();
        // Load data from PowerProfile's specialSkills map
        // Data is stored with prefixes: "lvl_", "cat_", "tier_"
        for (Map.Entry<String, Integer> entry : profile.specialSkills.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("lvl_")) {
                state.levels.put(key.substring(4), entry.getValue());
            } else if (key.startsWith("cat_")) {
                state.categoryCounts.put(key.substring(4), entry.getValue());
            } else if (key.startsWith("tier_")) {
                state.itemTiers.put(key.substring(5), entry.getValue());
            }
            // Skip raw unprefixed keys (duplicates from old saving logic)
        }
        
        // Load spent points if available
        if (profile.specialSkills.containsKey("_spent_points")) {
            state.spentPoints = profile.specialSkills.get("_spent_points");
        }
        
        return state;
    }

    public static void saveStateToProfile(SimState state, PowerProfile profile) {
        // Clear old data to prevent duplication issues
        profile.specialSkills.clear();
        
        // Save levels with "lvl_" prefix
        for (Map.Entry<String, Integer> e : state.levels.entrySet()) {
            profile.specialSkills.put("lvl_" + e.getKey(), e.getValue());
        }
        
        // Save category counts with "cat_" prefix
        for (Map.Entry<String, Integer> e : state.categoryCounts.entrySet()) {
            profile.specialSkills.put("cat_" + e.getKey(), e.getValue());
        }
        
        // Save item tiers with "tier_" prefix
        for (Map.Entry<String, Integer> e : state.itemTiers.entrySet()) {
            profile.specialSkills.put("tier_" + e.getKey(), e.getValue());
        }
        
        // Save spent points for tracking
        profile.specialSkills.put("_spent_points", (int)Math.round(state.spentPoints));
        
        // CRITICAL: For mixins that read directly from specialSkills (e.g., horde_summon),
        // we MUST also store unprefixed versions of skill-specific upgrades
        String[] mixinSkills = {
            "horde_summon", "infectious_bite", "hunger_attack", "piercing_shot", 
            "bow_potion_mastery", "multishot", "creeper_power", "cave_spider_poison_mastery",
            "witch_potion_mastery", "witch_harming_upgrade", "equipment_break_mastery"
        };
        
        for (String skill : mixinSkills) {
            if (state.levels.containsKey(skill)) {
                profile.specialSkills.put(skill, state.levels.get(skill));
            }
        }
    }

    private static void addGeneralUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        // Each skill has its own specific cost per the spec
        // HEALING: 1/2/3/4/5 pts progressive
        int healingLvl = state.getLevel("healing");
        if (healingLvl < 5) {
            int healingCost = healingLvl + 1; // Level 0->1 = 1pt, 1->2 = 2pts, etc
            addOpt(options, state, "healing", "g", "general", healingCost);
        }
        
        // HEALTH BOOST: 2 pts each (10 levels)
        if (state.getLevel("health_boost") < 10) addOpt(options, state, "health_boost", "g", "general", 2);
        
        // RESISTANCE: 4 pts each (3 levels)
        if (state.getLevel("resistance") < 3) addOpt(options, state, "resistance", "g", "general", 4);
        
        // INVISIBILITY MASTERY: 5 pts each (5 levels)
        if (state.getLevel("invis_mastery") < 5) addOpt(options, state, "invis_mastery", "g", "general", 5);
        
        // STRENGTH: 3 pts each (4 levels)
        if (state.getLevel("strength") < 4) addOpt(options, state, "strength", "g", "general", 3);
        
        // SPEED: 6 pts each (3 levels)
        if (state.getLevel("speed") < 3) addOpt(options, state, "speed", "g", "general", 6);
        
        // SHIELD CHANCE: 8 pts each (5 levels)
        if (state.getLevel("shield_chance") < 5) addOpt(options, state, "shield_chance", "g", "offhand", 8);
    }

    private static void addGeneralPassiveUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        int cost = getCost(state.getCategoryCount("gp"), costs);
        if (state.getLevel("healing") < 3) addOpt(options, state, "healing", "gp", "general", cost);
        if (state.getLevel("health_boost") < 3) addOpt(options, state, "health_boost", "gp", "general", cost);
        if (state.getLevel("resistance") < 1) addOpt(options, state, "resistance", "gp", "general", cost);
    }

    private static void addZombieUpgrades(SimState state, UpgradeCollector options, int[] costs, SimulationContext context) {
        // Hunger Attack: 6/10/14 pts progressive
        int hungerLvl = state.getLevel("hunger_attack");
        if (hungerLvl < 3) {
            int hungerCost = (hungerLvl == 0) ? 6 : (hungerLvl == 1) ? 10 : 14;
            addOpt(options, state, "hunger_attack", "z", "skill", hungerCost);
        }
        
        // Infectious Bite: 8/12/16 pts progressive
        int infectLvl = state.getLevel("infectious_bite");
        if (infectLvl < 3) {
            int infectCost = (infectLvl == 0) ? 8 : (infectLvl == 1) ? 12 : 16;
            addOpt(options, state, "infectious_bite", "z", "skill", infectCost);
        }
        
        // Horde Summon: 10/15/20/25/30 pts progressive
        boolean isReinforcement = context.tags.contains("umw_horde_reinforcement");
        int hordeLvl = state.getLevel("horde_summon");
        if (!isReinforcement && hordeLvl < 5) {
            int hordeCost = 10 + (hordeLvl * 5); // 10, 15, 20, 25, 30
            addOpt(options, state, "horde_summon", "z", "skill", hordeCost);
        }
    }

    private static void addProjectileUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        // Piercing Shot: 8/12/16/20 pts progressive (4 levels, not 5)
        int pierceLvl = state.getLevel("piercing_shot");
        if (pierceLvl < 4) {
            int pierceCost = 8 + (pierceLvl * 4); // 8, 12, 16, 20
            addOpt(options, state, "piercing_shot", "pro", "skill", pierceCost);
        }
        
        // Multishot: 15/25/35 pts progressive
        int multiLvl = state.getLevel("multishot_skill");
        if (multiLvl < 3) {
            int multiCost = (multiLvl == 0) ? 15 : (multiLvl == 1) ? 25 : 35;
            addOpt(options, state, "multishot_skill", "pro", "skill", multiCost);
        }
        
        // Bow Potion Mastery: 10/15/20/25/30 pts progressive
        int bowPotLvl = state.getLevel("bow_potion_mastery");
        if (bowPotLvl < 5) {
            int bowPotCost = 10 + (bowPotLvl * 5); // 10, 15, 20, 25, 30
            addOpt(options, state, "bow_potion_mastery", "pro", "skill", bowPotCost);
        }
    }
    
    private static void addCreeperUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        // Creeper Power: 10/15/20/25/30 pts progressive
        int powerLvl = state.getLevel("creeper_power");
        if (powerLvl < 5) {
            int powerCost = 10 + (powerLvl * 5); // 10, 15, 20, 25, 30
            addOpt(options, state, "creeper_power", "creeper", "skill", powerCost);
        }
        
        // Creeper Potion: 12/18/24 pts progressive
        int potLvl = state.getLevel("creeper_potion_mastery");
        if (potLvl < 3) {
            int potCost = 12 + (potLvl * 6); // 12, 18, 24
            addOpt(options, state, "creeper_potion_mastery", "creeper", "skill", potCost);
        }
    }

    private static void addWitchUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        // Potion Throw Speed: 10/15/20/25/30 pts progressive
        int throwLvl = state.getLevel("witch_potion_mastery");
        if (throwLvl < 5) {
            int throwCost = 10 + (throwLvl * 5); // 10, 15, 20, 25, 30
            addOpt(options, state, "witch_potion_mastery", "witch", "skill", throwCost);
        }
        
        // Harming Upgrade: 12/18/24 pts progressive
        int harmLvl = state.getLevel("witch_harming_upgrade");
        if (harmLvl < 3) {
            int harmCost = 12 + (harmLvl * 6); // 12, 18, 24
            addOpt(options, state, "witch_harming_upgrade", "witch", "skill", harmCost);
        }
    }

    private static void addCaveSpiderUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        // Poison Mastery: 8/12/16/20/24 pts progressive
        int poisonLvl = state.getLevel("poison_attack");
        if (poisonLvl < 5) {
            int poisonCost = 8 + (poisonLvl * 4); // 8, 12, 16, 20, 24
            addOpt(options, state, "poison_attack", "cave_spider", "skill", poisonCost);
        }
    }

    private static void addWeaponUpgrades(SimState state, UpgradeCollector options, int[] costs, String catName, List<String> enchants, List<Integer> maxLevels) {
        int cost = getCost(state.getCategoryCount(catName), costs);
        
        // Weight is now handled dynamically by group normalization
        int weight = 1;
        
        for (int i = 0; i < enchants.size(); i++) {
            String ench = enchants.get(i);
            int max = maxLevels.get(i);
            if (state.getLevel(ench) < max) {
                addOpt(options, state, ench, catName, "mainhand", cost, weight);
            }
        }
    }

    private static void addArmorUpgrades(SimState state, UpgradeCollector options, int[] costs) {
        int cost = getCost(state.getCategoryCount("armor"), costs);
        
        // Per-slot shared enchants
        addPerSlot(options, state, "protection", 4, cost);
        addPerSlot(options, state, "fire_protection", 4, cost);
        addPerSlot(options, state, "blast_protection", 4, cost);
        addPerSlot(options, state, "projectile_protection", 4, cost);
        addPerSlot(options, state, "thorns", 3, cost);
        addPerSlot(options, state, "armor_unbreaking", 3, cost);
        addPerSlot(options, state, "armor_mending", 1, cost);
        
        // Specific enchants (Head)
        if (state.getLevel("aqua_affinity") < 1) addOpt(options, state, "aqua_affinity", "armor", "head", cost);
        if (state.getLevel("respiration") < 3) addOpt(options, state, "respiration", "armor", "head", cost);
        
        // Specific enchants (Legs)
        if (state.getLevel("swift_sneak") < 3) addOpt(options, state, "swift_sneak", "armor", "legs", cost);
        
        // Specific enchants (Feet)
        if (state.getLevel("feather_falling") < 4) addOpt(options, state, "feather_falling", "armor", "feet", cost);
        if (state.getLevel("soul_speed") < 3) addOpt(options, state, "soul_speed", "armor", "feet", cost);
        if (state.getLevel("depth_strider") < 3) addOpt(options, state, "depth_strider", "armor", "feet", cost);
        if (state.getLevel("frost_walker") < 2) addOpt(options, state, "frost_walker", "armor", "feet", cost);
    }
    
    private static void addPerSlot(UpgradeCollector options, SimState state, String baseId, int max, int cost) {
        for (String slot : List.of("head", "chest", "legs", "feet")) {
            if (state.getLevel(baseId + "_" + slot) < max) {
                addOpt(options, state, baseId + "_" + slot, "armor", slot, cost);
            }
        }
    }
    
    private static void addEquipmentStatsUpgrades(SimState state, UpgradeCollector options, boolean main, boolean off, boolean armor) {
        int cost = 1;
        if (main) addStatUpgrades(state, options, "mainhand", cost);
        if (off) addStatUpgrades(state, options, "offhand", cost);
        if (armor) {
            addStatUpgrades(state, options, "head", cost);
            addStatUpgrades(state, options, "chest", cost);
            addStatUpgrades(state, options, "legs", cost);
            addStatUpgrades(state, options, "feet", cost);
        }
    }

    private static void addStatUpgrades(SimState state, UpgradeCollector options, String slot, int cost) {
        // Durability (0-10) - 10 pts each per spec
        int durLvl = state.getLevel("durability_" + slot);
        if (durLvl < 10) {
            // Weighting: Lower durability = Higher chance
            // Level 0: 10 entries. Level 9: 1 entry.
            int weight = 10 - durLvl;
            addOpt(options, state, "durability_" + slot, "stats", slot, 10, weight);
        }
        // Drop Chance (0-10) - 10 pts each per spec
        int dropLvl = state.getLevel("drop_chance_" + slot);
        if (dropLvl < 10) {
            addOpt(options, state, "drop_chance_" + slot, "stats", slot, 10);
        }
    }
    
    private static void addOpt(UpgradeCollector options, SimState state, String id, String cat, String group, int cost) {
        options.add(id, cat, group, cost, 1);
    }
    
    private static void addOpt(UpgradeCollector options, SimState state, String id, String cat, String group, int cost, int weight) {
        options.add(id, cat, group, cost, weight);
    }

    private static int getCost(int count, int[] costs) {
        if (count >= costs.length) return costs[costs.length - 1];
        return costs[count];
    }


    private static void checkArmorTier(SimState state, String slot, List<String> tiers, List<String> extraEnchants, List<Integer> extraMax) {
        int currentTier = state.getItemTier(slot);
        if (currentTier < tiers.size() - 1) {
            // Check shared enchants for this slot
            List<String> shared = List.of("protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "armor_unbreaking", "armor_mending");
            List<Integer> sharedMax = List.of(4, 4, 4, 4, 3, 3, 1);
            
            boolean maxed = true;
            for (int i = 0; i < shared.size(); i++) {
                if (state.getLevel(shared.get(i) + "_" + slot) < sharedMax.get(i)) {
                    maxed = false;
                    break;
                }
            }
            
            if (maxed && isMaxed(state, extraEnchants, extraMax)) {
                state.setItemTier(slot, currentTier + 1);
                
                // Reset enchants for this slot
                for (String s : shared) state.setLevel(s + "_" + slot, 0);
                for (String s : extraEnchants) state.setLevel(s, 0);
                
                // Reset stats
                state.setLevel("durability_" + slot, 0);
                state.setLevel("drop_chance_" + slot, 0);
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

    public static void applyStateToMob(MobEntity mob, SimState state, PowerProfile profile) {
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
        if (state.getLevel("resistance") >= 3) { // Level 3 gives Fire Resis 1
            mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE, 999999, 0));
        }
        
        // Equipment
        boolean isPiglin = mob.getType().getTranslationKey().contains("piglin");
        
        // Sword
        if ((state.getCategoryCount("sword") > 0 || state.getItemTier("sword") > 0) && state.getItemTier("sword") >= 0) {
            List<String> tiers = isPiglin ? GOLD_SWORD_TIERS : SWORD_TIERS;
            int tierIndex = state.getItemTier("sword");
            if (tierIndex >= 0 && tierIndex < tiers.size()) {
                String itemId = tiers.get(Math.min(tierIndex, tiers.size() - 1));
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack stack = new ItemStack(item);
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
                    }
                }
            }
        }

        // Axe
        if ((state.getCategoryCount("axe") > 0 || state.getItemTier("axe") > 0) && state.getItemTier("axe") >= 0) {
            List<String> tiers = isPiglin ? GOLD_AXE_TIERS : AXE_TIERS;
            int tierIndex = state.getItemTier("axe");
            if (tierIndex >= 0 && tierIndex < tiers.size()) {
                String itemId = tiers.get(Math.min(tierIndex, tiers.size() - 1));
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack stack = new ItemStack(item);
                            applyEnchant(mob, stack, "sharpness", state.getLevel("sharpness"));
                            applyEnchant(mob, stack, "smite", state.getLevel("smite"));
                            applyEnchant(mob, stack, "bane_of_arthropods", state.getLevel("bane_of_arthropods"));
                            applyEnchant(mob, stack, "unbreaking", state.getLevel("unbreaking"));
                            applyEnchant(mob, stack, "mending", state.getLevel("mending"));
                            applyEnchant(mob, stack, "efficiency", state.getLevel("efficiency"));
                            mob.equipStack(EquipmentSlot.MAINHAND, stack);
                        }
                    }
                }
            }
        }

        // Trident
        if ((state.getCategoryCount("trident") > 0 || state.getItemTier("trident") > 0) && state.getItemTier("trident") >= 0) {
            int tierIndex = state.getItemTier("trident");
            // Only one trident tier in vanilla, but support for future expansion
            List<String> tridentTiers = List.of("minecraft:trident");
            if (tierIndex >= 0 && tierIndex < tridentTiers.size()) {
                String itemId = tridentTiers.get(tierIndex);
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack stack = new ItemStack(item);
                            applyEnchant(mob, stack, "impaling", state.getLevel("impaling"));
                            applyEnchant(mob, stack, "channeling", state.getLevel("channeling"));
                            applyEnchant(mob, stack, "unbreaking", state.getLevel("unbreaking"));
                            applyEnchant(mob, stack, "mending", state.getLevel("mending"));
                            applyEnchant(mob, stack, "loyalty", state.getLevel("loyalty"));
                            applyEnchant(mob, stack, "riptide", state.getLevel("riptide"));
                            mob.equipStack(EquipmentSlot.MAINHAND, stack);
                        }
                    }
                }
            }
        }

        // Bow
        if ((state.getCategoryCount("bow") > 0 || state.getItemTier("bow") > 0) && state.getItemTier("bow") >= 0) {
            int tierIndex = state.getItemTier("bow");
            // Only one bow tier in vanilla, but support for future expansion
            List<String> bowTiers = List.of("minecraft:bow");
            if (tierIndex >= 0 && tierIndex < bowTiers.size()) {
                String itemId = bowTiers.get(tierIndex);
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack stack = new ItemStack(item);
                            applyEnchant(mob, stack, "power", state.getLevel("power"));
                            applyEnchant(mob, stack, "flame", state.getLevel("flame"));
                            applyEnchant(mob, stack, "punch", state.getLevel("punch"));
                            applyEnchant(mob, stack, "infinity", state.getLevel("infinity"));
                            applyEnchant(mob, stack, "unbreaking", state.getLevel("unbreaking"));
                            applyEnchant(mob, stack, "mending", state.getLevel("mending"));
                            mob.equipStack(EquipmentSlot.MAINHAND, stack);
                        }
                    }
                }
            }
        }
        
        // Armor
        if (state.getCategoryCount("armor") > 0 || state.getItemTier("head") > 0 || state.getItemTier("chest") > 0 || state.getItemTier("legs") > 0 || state.getItemTier("feet") > 0) {
            if (state.getItemTier("head") >= 0) {
                int headTier = Math.min(state.getItemTier("head"), HELMET_TIERS.size() - 1);
                String itemId = HELMET_TIERS.get(headTier);
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack helm = new ItemStack(item);
                            applyArmorEnchants(mob, helm, state, "head");
                            applyEnchant(mob, helm, "aqua_affinity", state.getLevel("aqua_affinity"));
                            applyEnchant(mob, helm, "respiration", state.getLevel("respiration"));
                            mob.equipStack(EquipmentSlot.HEAD, helm);
                        }
                    }
                }
            }
            if (state.getItemTier("chest") >= 0) {
                int chestTier = Math.min(state.getItemTier("chest"), CHEST_TIERS.size() - 1);
                String itemId = CHEST_TIERS.get(chestTier);
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack chest = new ItemStack(item);
                            applyArmorEnchants(mob, chest, state, "chest");
                            mob.equipStack(EquipmentSlot.CHEST, chest);
                        }
                    }
                }
            }
            if (state.getItemTier("legs") >= 0) {
                int legsTier = Math.min(state.getItemTier("legs"), LEGS_TIERS.size() - 1);
                String itemId = LEGS_TIERS.get(legsTier);
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack legs = new ItemStack(item);
                            applyArmorEnchants(mob, legs, state, "legs");
                            applyEnchant(mob, legs, "swift_sneak", state.getLevel("swift_sneak"));
                            mob.equipStack(EquipmentSlot.LEGS, legs);
                        }
                    }
                }
            }
            if (state.getItemTier("feet") >= 0) {
                int feetTier = Math.min(state.getItemTier("feet"), BOOTS_TIERS.size() - 1);
                String itemId = BOOTS_TIERS.get(feetTier);
                if (itemId != null && itemId.contains(":")) {
                    String[] parts = itemId.split(":", 2);
                    if (parts.length == 2) {
                        Identifier id = Identifier.of(parts[0], parts[1]);
                        var item = net.minecraft.registry.Registries.ITEM.get(id);
                        if (item != null && item != Items.AIR) {
                            ItemStack boots = new ItemStack(item);
                            applyArmorEnchants(mob, boots, state, "feet");
                            applyEnchant(mob, boots, "feather_falling", state.getLevel("feather_falling"));
                            applyEnchant(mob, boots, "soul_speed", state.getLevel("soul_speed"));
                            applyEnchant(mob, boots, "depth_strider", state.getLevel("depth_strider"));
                            applyEnchant(mob, boots, "frost_walker", state.getLevel("frost_walker"));
                            mob.equipStack(EquipmentSlot.FEET, boots);
                        }
                    }
                }
            }
        }
        
        // Apply Equipment Stats (Durability & Drop Chance)
        // We pass the tier category to track tier changes
        String mainTierCat = state.getCategoryCount("axe") > 0 ? "axe" : "sword"; // Simple heuristic
        applyEquipmentStats(mob, state, profile, EquipmentSlot.MAINHAND, "mainhand", mainTierCat);
        applyEquipmentStats(mob, state, profile, EquipmentSlot.OFFHAND, "offhand", "shield");
        applyEquipmentStats(mob, state, profile, EquipmentSlot.HEAD, "head", "armor");
        applyEquipmentStats(mob, state, profile, EquipmentSlot.CHEST, "chest", "armor");
        applyEquipmentStats(mob, state, profile, EquipmentSlot.LEGS, "legs", "armor");
        applyEquipmentStats(mob, state, profile, EquipmentSlot.FEET, "feet", "armor");
        
        // Shield Chance: 0% -> 100% (Normal curve)
        // Now handled via points (shield_chance level)
        int shieldLvl = state.getLevel("shield_chance");
        if (shieldLvl > 0) {
             // Level 1-5. 
             // Level 1: 20%, Level 5: 100%
             double p = shieldLvl * 0.20;
             if (mob.getRandom().nextDouble() < p) {
                 ItemStack shield = new ItemStack(Items.SHIELD);
                 // Apply defensive enchants (Unbreaking/Mending) from Armor tree (using Chest stats as proxy for shield)
                 applyEnchant(mob, shield, "unbreaking", state.getLevel("armor_unbreaking_chest"));
                 applyEnchant(mob, shield, "mending", state.getLevel("armor_mending_chest"));
                 mob.equipStack(EquipmentSlot.OFFHAND, shield);
             }
        }

        // NOTE: Removed automatic 'maxed' forcing of durability/drop chance.
        // Durability and drop chance are now exclusively controlled by purchased
        // `durability_<slot>` and `drop_chance_<slot>` levels and by tier multipliers.

        // Creeper Specifics
        if (mob instanceof net.minecraft.entity.mob.CreeperEntity creeper) {
            NbtCompound nbt = new NbtCompound();
            creeper.writeCustomDataToNbt(nbt);
        }

        // Save special skills to PowerProfile for Mixins
        // DO NOT CLEAR - just update values so prefixed data remains intact
        // Update all skills that mixins depend on
        profile.specialSkills.put("horde_summon", state.getLevel("horde_summon"));
        profile.specialSkills.put("hunger_attack", state.getLevel("hunger_attack"));
        profile.specialSkills.put("invis_mastery", state.getLevel("invis_mastery"));
        profile.specialSkills.put("piercing_shot", state.getLevel("piercing_shot"));
        profile.specialSkills.put("multishot_skill", state.getLevel("multishot_skill"));
        profile.specialSkills.put("poison_attack", state.getLevel("poison_attack"));
        profile.specialSkills.put("infectious_bite", state.getLevel("infectious_bite"));
        profile.specialSkills.put("creeper_power", state.getLevel("creeper_power"));
        profile.specialSkills.put("creeper_potion_mastery", state.getLevel("creeper_potion_mastery"));
        profile.specialSkills.put("speed", state.getLevel("speed"));
        profile.specialSkills.put("strength", state.getLevel("strength"));
        profile.specialSkills.put("resistance", state.getLevel("resistance"));
        profile.specialSkills.put("healing", state.getLevel("healing"));
        profile.specialSkills.put("health_boost", state.getLevel("health_boost"));
        profile.specialSkills.put("shield_chance", state.getLevel("shield_chance"));
        
        // Invis Interval Calculation
        if (state.getLevel("invis_mastery") > 0) {
            // 1 -> 10min, 12 -> 0.25min
            // Mapping: 1=10, 2=9, 3=8, 4=7, 5=6, 6=5, 7=4, 8=3, 9=2, 10=1, 11=0.5, 12=0.25
            double minutes = 10.0;
            int lvl = state.getLevel("invis_mastery");
            if (lvl <= 10) {
                minutes = 11.0 - lvl; // 1->10, 10->1
            } else if (lvl == 11) {
                minutes = 0.5;
            } else {
                minutes = 0.25;
            }
            profile.specialSkills.put("invis_interval_ticks", (int)(minutes * 60 * 20));
        }
        
        // Potion Mastery (Points Based)
        // Always save potion mastery skills (even if 0) so mixins can read them
        profile.specialSkills.put("bow_potion_mastery", state.getLevel("bow_potion_mastery"));
        profile.specialSkills.put("witch_potion_mastery", state.getLevel("witch_potion_mastery"));
        profile.specialSkills.put("witch_harming_upgrade", state.getLevel("witch_harming_upgrade"));
        
        spawnUpgradeParticles(mob);
    }
    
    private static void applyArmorEnchants(MobEntity mob, ItemStack stack, SimState state, String slot) {
        applyEnchant(mob, stack, "protection", state.getLevel("protection_" + slot));
        applyEnchant(mob, stack, "fire_protection", state.getLevel("fire_protection_" + slot));
        applyEnchant(mob, stack, "blast_protection", state.getLevel("blast_protection_" + slot));
        applyEnchant(mob, stack, "projectile_protection", state.getLevel("projectile_protection_" + slot));
        applyEnchant(mob, stack, "thorns", state.getLevel("thorns_" + slot));
        applyEnchant(mob, stack, "unbreaking", state.getLevel("armor_unbreaking_" + slot));
        applyEnchant(mob, stack, "mending", state.getLevel("armor_mending_" + slot));
    }

    private static void applyEquipmentStats(MobEntity mob, SimState state, PowerProfile profile, EquipmentSlot slot, String slotName, String tierCat) {
        ItemStack stack = mob.getEquippedStack(slot);
        // If there is no stack currently equipped, it may have broken. Handle downgrade mechanic.
        SimulationContext context = new SimulationContext(mob, profile.totalPoints);
        if (stack.isEmpty()) {
            int lastAppliedTier = profile.specialSkills.getOrDefault("last_tier_" + slotName, -1);
            if (lastAppliedTier >= 0) {
                // Determine tier list for this category
                List<String> tiers = getTiersForCategory(tierCat, context);
                int prevTier = lastAppliedTier - 1;
                if (prevTier < 0) {
                    // If lowest tier is wood/gold, the item is lost; otherwise remain at lowest tier
                    String lowest = tiers.isEmpty() ? "" : tiers.get(0);
                    if (lowest.contains("wood") || lowest.contains("wooden") || lowest.contains("gold") || lowest.contains("golden")) {
                        state.setItemTier(tierCat, -1);
                        profile.specialSkills.put("last_tier_" + slotName, -1);
                    } else {
                        state.setItemTier(tierCat, 0);
                        profile.specialSkills.put("last_tier_" + slotName, 0);
                    }
                } else {
                    state.setItemTier(tierCat, prevTier);
                    profile.specialSkills.put("last_tier_" + slotName, prevTier);
                }

                // Reset enchant progression and stats for this slot
                resetEnchantsForSlotOnDowngrade(state, tierCat, slotName);
                state.setLevel("durability_" + slotName, 0);
                state.setLevel("drop_chance_" + slotName, 0);
            }
            return;
        }

        // Drop Chance: Base 100% - (5% * level)
        int dropLvl = state.getLevel("drop_chance_" + slotName);
        float dropChance = Math.max(0.01f, 1.0f - (dropLvl * 0.05f));

        // Apply tier-based enchant drop multipliers (50% -> 40% -> 30% -> 20% -> 10% -> 5%)
        int tier = state.getItemTier(tierCat);
        double[] tierDrop = new double[] {0.5, 0.4, 0.3, 0.2, 0.1, 0.05};
        double tierMultiplier = 1.0;
        if (tier >= 0) {
            int idx = Math.min(tier, tierDrop.length - 1);
            tierMultiplier = tierDrop[idx];
            // store for debugging/inspection
            profile.specialSkills.put("tier_drop_mult_" + slotName, (int)(tierMultiplier * 100));
        }

        float finalDropChance = (float)(dropChance * tierMultiplier);
        finalDropChance = Math.max(0.01f, Math.min(1.0f, finalDropChance));
        mob.setEquipmentDropChance(slot, finalDropChance);

        // Durability Logic
        if (stack.isDamageable()) {
            int currentDurLvl = state.getLevel("durability_" + slotName);
            int currentTier = state.getItemTier(tierCat);
            
            // Retrieve last applied state from persistent profile
            int lastAppliedTier = profile.specialSkills.getOrDefault("last_tier_" + slotName, -1);
            int lastAppliedDur = profile.specialSkills.getOrDefault("last_dur_" + slotName, -1);
            
            // If Tier changed, reset tracking (New Item = Fresh Start)
            if (currentTier != lastAppliedTier) {
                lastAppliedDur = -1;
                profile.specialSkills.put("last_tier_" + slotName, currentTier);
            }
            
            // Only apply durability if we have UPGRADED it (Current > Last)
            // This prevents "healing" the item by re-applying the same level
            if (currentDurLvl > lastAppliedDur) {
                int maxDamage = stack.getMaxDamage();
                int damage;
                
                if (currentDurLvl == 0) {
                    damage = maxDamage - 1; // 1 durability left
                } else {
                    // Level 1 = 10%, Level 10 = 100%
                    float percent = currentDurLvl * 0.10f;
                    damage = maxDamage - (int)(maxDamage * percent);
                    if (damage < 0) damage = 0;
                    if (damage >= maxDamage) damage = maxDamage - 1;
                }
                
                stack.setDamage(damage);
                
                // Update persistent state
                profile.specialSkills.put("last_dur_" + slotName, currentDurLvl);
                // Debug: report durability application
                try {
                    if (ModConfig.getInstance().debugUpgradeLog && mob.getWorld() instanceof ServerWorld sw) {
                        MinecraftServer server = sw.getServer();
                        String msg = String.format("[UMW] Durability applied: mob=%s slot=%s level=%d => remaining=%.1f%%", mob.getType().getTranslationKey(), slotName, currentDurLvl, (1.0 - ((double)damage / (double)maxDamage)) * 100.0);
                        Text text = Text.literal(msg);
                        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) p.sendMessage(text, false);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private static List<String> getTiersForCategory(String tierCat) {
        return getTiersForCategory(tierCat, null);
    }

    private static List<String> getTiersForCategory(String tierCat, SimulationContext context) {
        boolean isPiglin = context != null && context.translationKey != null && context.translationKey.contains("piglin");
        boolean isBrute = context != null && context.translationKey != null && context.translationKey.contains("piglin_brute");

        if (isPiglin || isBrute) {
            switch (tierCat) {
                case "helmet": case "head": return ModConfig.getInstance().goldHelmetTiers;
                case "chest": return ModConfig.getInstance().goldChestTiers;
                case "legs": return ModConfig.getInstance().goldLegsTiers;
                case "feet": return ModConfig.getInstance().goldBootsTiers;
                case "sword": return GOLD_SWORD_TIERS;
                case "axe": return GOLD_AXE_TIERS;
                default: return List.of();
            }
        }
        return switch (tierCat) {
            case "sword" -> SWORD_TIERS;
            case "axe" -> AXE_TIERS;
            case "helmet", "head" -> HELMET_TIERS;
            case "chest" -> CHEST_TIERS;
            case "legs" -> LEGS_TIERS;
            case "feet" -> BOOTS_TIERS;
            case "shield" -> List.of("minecraft:shield");
            default -> List.of();
        };
    }

    private static void resetEnchantsForSlotOnDowngrade(SimState state, String tierCat, String slotName) {
        if (tierCat.equals("sword") || tierCat.equals("axe")) {
            resetEnchants(state, List.of("sharpness", "fire_aspect", "mending", "unbreaking", "knockback", "smite", "bane_of_arthropods", "looting", "efficiency"));
        } else if (tierCat.equals("head") || tierCat.equals("chest") || tierCat.equals("legs") || tierCat.equals("feet") || tierCat.equals("armor")) {
            // Reset per-slot armor enchants
            List<String> shared = List.of("protection", "fire_protection", "blast_protection", "projectile_protection", "thorns", "armor_unbreaking", "armor_mending");
            for (String s : shared) state.setLevel(s + "_" + slotName, 0);
            // Reset extra slot-specific enchants
            if (slotName.equals("head")) {
                state.setLevel("aqua_affinity", 0);
                state.setLevel("respiration", 0);
            } else if (slotName.equals("legs")) {
                state.setLevel("swift_sneak", 0);
            } else if (slotName.equals("feet")) {
                state.setLevel("feather_falling", 0);
                state.setLevel("soul_speed", 0);
                state.setLevel("depth_strider", 0);
                state.setLevel("frost_walker", 0);
            }
        }
    }

    private static void applyEnchant(MobEntity mob, ItemStack stack, String id, int level) {
        if (level <= 0) return;
        var registry = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(net.minecraft.util.Identifier.of("minecraft", id));
        if (entry.isPresent()) {
            stack.addEnchantment(entry.get(), level);
        }
    }

    private static void spawnUpgradeParticles(MobEntity mob) {
        if (!ModConfig.getInstance().showLevelParticles) return;
        if (mob.getWorld().isClient) return;
        
        if (mob.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + mob.getHeight(), mob.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticles(ParticleTypes.WAX_ON, mob.getX(), mob.getY() + mob.getHeight() / 2, mob.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    // Remove deprecated UpgradeNode and legacy methods
}
