package mod.universalmobwar.system;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import mod.universalmobwar.data.PowerProfile;

/**
 * UpgradeSystem: Applies deterministic upgrades, equipment, enchantments, and effects for each archetype and tier.
 * Handles hard caps, shield chance, potion effects, and sensible upgrades for each mob type.
 */
public class UpgradeSystem {
        // Track which mobs have already performed horde summon (prevents recursion, not NBT-based)
        private static final java.util.Set<java.util.UUID> HORDE_SUMMONED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    /**
     * Skill tree node for upgrades.
     */
    public static class UpgradeNode {
        public final String id; // unique string, e.g. "health_1"
        public final int cost;
        public final int tierReq;
        public final String category;
        public final Map<String, Double> attributes;
        public final Map<EquipmentSlot, String> equipment;
        public final Map<String, Integer> effects;
        public final Map<String, Integer> enchantments;
        public final double shieldChance;
        public final double healthCap;
        public final List<UpgradeNode> children;
        public UpgradeNode(String id, int cost, int tierReq, String category,
                   Map<String, Double> attributes,
                   Map<EquipmentSlot, String> equipment,
                   Map<String, Integer> effects,
                   Map<String, Integer> enchantments,
                   double shieldChance, double healthCap,
                   List<UpgradeNode> children) {
            this.id = id;
            this.cost = cost;
            this.tierReq = tierReq;
            this.category = category;
            this.attributes = attributes;
            this.equipment = equipment;
            this.effects = effects;
            this.enchantments = enchantments;
            this.shieldChance = shieldChance;
            this.healthCap = healthCap;
            this.children = children;
        }
    }

    // --- Skill Tree UpgradeNode helpers ---
    /**
     * Finds an UpgradeNode by id in the given archetype's skill tree.
     */
    public static UpgradeNode findUpgradeNode(String archetype, String upgradeId) {
        List<UpgradeNode> roots = archetypeSkillTrees.getOrDefault(archetype, archetypeSkillTrees.get("universal"));
        for (UpgradeNode root : roots) {
            UpgradeNode found = findUpgradeNodeRecursive(root, upgradeId);
            if (found != null) return found;
        }
        return null;
    }

    private static UpgradeNode findUpgradeNodeRecursive(UpgradeNode node, String upgradeId) {
        if (node.id.equals(upgradeId)) return node;
        for (UpgradeNode child : node.children) {
            UpgradeNode found = findUpgradeNodeRecursive(child, upgradeId);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Applies the effects of an UpgradeNode to the given mob.
     */
    public static void applyUpgradeNode(MobEntity mob, UpgradeNode node) {
        if (node == null) return;
        // Apply attributes (with hard caps)
        if (node.attributes.containsKey("health")) {
            double cap = Math.min(node.attributes.get("health"), node.healthCap);
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(cap);
            mob.setHealth((float) cap);
        }
        if (node.attributes.containsKey("attack")) {
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(node.attributes.get("attack"));
        }
        if (node.attributes.containsKey("speed")) {
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(node.attributes.get("speed"));
        }
        if (node.attributes.containsKey("armor")) {
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR).setBaseValue(node.attributes.get("armor"));
        }
        if (node.attributes.containsKey("knockback")) {
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(node.attributes.get("knockback"));
        }
        // Custom Drowned and Piglin weapon logic
        String mobName = mob.getType().toString().toLowerCase();
        boolean isDrowned = mobName.contains("drowned");
        boolean isPiglin = mobName.contains("piglin");
        boolean isApex = node != null && node.id.equals("universal_apex_10");
        for (Map.Entry<EquipmentSlot, String> entry : node.equipment.entrySet()) {
            EquipmentSlot slot = entry.getKey();
            String id = entry.getValue();
            String[] parts = id.split(":");
            ItemStack stack = null;
            if (isDrowned && slot == EquipmentSlot.MAINHAND) {
                // Drowned always get trident, enchanted at high tier
                stack = Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "trident")).getDefaultStack();
                if (node.tierReq >= 5) {
                    var enchReg = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
                    var loyalty = enchReg.getEntry(net.minecraft.util.Identifier.of("minecraft", "loyalty"));
                    var impaling = enchReg.getEntry(net.minecraft.util.Identifier.of("minecraft", "impaling"));
                    if (loyalty.isPresent()) stack.addEnchantment(loyalty.get(), 3);
                    if (impaling.isPresent()) stack.addEnchantment(impaling.get(), 5);
                }
            } else if (isPiglin && slot == EquipmentSlot.MAINHAND) {
                // Piglins get gold sword unless apex, then Netherite
                if (isApex) {
                    stack = Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "netherite_sword")).getDefaultStack();
                } else {
                    stack = Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "golden_sword")).getDefaultStack();
                }
                // Enchant at high tier
                if (node.tierReq >= 5) {
                    var enchReg = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
                    var sharp = enchReg.getEntry(net.minecraft.util.Identifier.of("minecraft", "sharpness"));
                    var fire = enchReg.getEntry(net.minecraft.util.Identifier.of("minecraft", "fire_aspect"));
                    if (sharp.isPresent()) stack.addEnchantment(sharp.get(), 5);
                    if (fire.isPresent()) stack.addEnchantment(fire.get(), 2);
                }
            } else if (parts.length == 2) {
                stack = Registries.ITEM.get(net.minecraft.util.Identifier.of(parts[0], parts[1])).getDefaultStack();
            }
            if (stack != null) {
                mob.equipStack(slot, stack);
            }
        }
        // Apply potion effects
        for (Map.Entry<String, Integer> entry : node.effects.entrySet()) {
            String id = entry.getKey();
            String[] parts = id.split(":");
            if (parts.length == 2) {
                var registry = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.STATUS_EFFECT);
                var effectEntry = registry.getEntry(net.minecraft.util.Identifier.of(parts[0], parts[1]));
                if (effectEntry.isPresent()) {
                    mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        effectEntry.get(),
                        20 * 60 * 10, // 10 min
                        entry.getValue().intValue()
                    ));
                }
            }
        }
        // Apply enchantments
        for (Map.Entry<String, Integer> entry : node.enchantments.entrySet()) {
            String id = entry.getKey();
            String[] parts = id.split(":");
            if (parts.length == 2) {
                var registry = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
                var enchEntry = registry.getEntry(net.minecraft.util.Identifier.of(parts[0], parts[1]));
                if (enchEntry.isPresent()) {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack stack = mob.getEquippedStack(slot);
                        if (!stack.isEmpty()) {
                            stack.addEnchantment(enchEntry.get(), entry.getValue().intValue());
                        }
                    }
                }
            }
        }
        // Shield chance (if applicable)
        if (node.shieldChance > 0 && Math.random() < node.shieldChance) {
            ItemStack shield = Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "shield")).getDefaultStack();
            mob.equipStack(EquipmentSlot.OFFHAND, shield);
        }

        // Special: If this is the apex node, apply shield, all max enchants, and invis burst logic
        if (node != null && node.id.equals("universal_apex_10")) {
            // Equip shield with max enchants
            ItemStack shield = Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "shield")).getDefaultStack();
            var registry = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
            Map<String, Integer> enchants = Map.of(
                "minecraft:unbreaking", 3,
                "minecraft:mending", 1,
                "minecraft:thorns", 3
            );
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    var enchEntry = registry.getEntry(net.minecraft.util.Identifier.of(parts[0], parts[1]));
                    if (enchEntry.isPresent()) {
                        shield.addEnchantment(enchEntry.get(), entry.getValue());
                    }
                }
            }
            mob.equipStack(EquipmentSlot.OFFHAND, shield);
            // Invisibility burst: 10% chance, 20s duration
            if (mob.getWorld().random.nextFloat() < 0.10f) {
                var effectReg = mob.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.STATUS_EFFECT);
                var invis = effectReg.getEntry(net.minecraft.util.Identifier.of("minecraft", "invisibility"));
                if (invis.isPresent()) {
                    mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(invis.get(), 20 * 20, 0));
                }
            }
        }

        // Special: If this is the "horde_summon" upgrade, summon zombies nearby (deterministic, non-NBT, non-recursive)
        if (node != null && node.category.equals("horde_summon")) {
            if (mob.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                java.util.UUID mobId = mob.getUuid();
                if (HORDE_SUMMONED.contains(mobId)) return;
                HORDE_SUMMONED.add(mobId);
                // Use mob UUID and world seed for deterministic random
                long seed = serverWorld.getSeed() ^ mobId.getMostSignificantBits() ^ mobId.getLeastSignificantBits();
                java.util.Random rand = new java.util.Random(seed);
                int count = 2 + rand.nextInt(3); // 2-4 zombies
                for (int i = 0; i < count; i++) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double dist = 2.0 + rand.nextDouble() * 2.0;
                    double dx = mob.getX() + Math.cos(angle) * dist;
                    double dz = mob.getZ() + Math.sin(angle) * dist;
                    double dy = mob.getY();
                    var entityType = net.minecraft.entity.EntityType.ZOMBIE;
                    var newZombie = entityType.create(serverWorld);
                    if (newZombie != null) {
                        newZombie.refreshPositionAndAngles(dx, dy, dz, rand.nextFloat() * 360.0f, 0.0f);
                        // Mark summoned zombie so it can never horde summon
                        HORDE_SUMMONED.add(newZombie.getUuid());
                        serverWorld.spawnEntity(newZombie);
                    }
                }
            }
        }
    }

    // Map archetype to root nodes of their skill tree
    public static final Map<String, List<UpgradeNode>> archetypeSkillTrees = new HashMap<>();
    static {
        // --- Universal Skill Tree (General Path, now with true max tier) ---
        // Final apex node: full Netherite, all max vanilla enchants, shield, all effects, stat boosts, invis burst
        UpgradeNode universalApex = new UpgradeNode(
            "universal_apex_10", 100, 10, "apex",
            Map.of(
                "health", 120.0, // 6x base (20)
                "attack", 30.0,  // 6x base (5)
                "armor", 20.0,   // Netherite + boost
                "speed", 0.40,   // +100%
                "knockback", 1.0 // full resist
            ),
            Map.of(
                EquipmentSlot.HEAD, "minecraft:netherite_helmet",
                EquipmentSlot.CHEST, "minecraft:netherite_chestplate",
                EquipmentSlot.LEGS, "minecraft:netherite_leggings",
                EquipmentSlot.FEET, "minecraft:netherite_boots",
                EquipmentSlot.MAINHAND, "minecraft:netherite_sword"
            ),
            Map.of(
                "minecraft:strength", 5,
                "minecraft:speed", 5,
                "minecraft:resistance", 3,
                "minecraft:regeneration", 3,
                "minecraft:fire_resistance", 1,
                "minecraft:absorption", 1,
                "minecraft:health_boost", 1,
                "minecraft:water_breathing", 1
            ),
            Map.of(
                "minecraft:protection", 4,
                "minecraft:unbreaking", 3,
                "minecraft:thorns", 3,
                "minecraft:mending", 1,
                "minecraft:sharpness", 5,
                "minecraft:looting", 3,
                "minecraft:fire_aspect", 2,
                "minecraft:knockback", 2
            ),
            1.0, // shieldChance (guaranteed)
            120.0, // health cap
            List.of() // End node
        );
        UpgradeNode universalInvis = new UpgradeNode(
            "universal_invis_9", 90, 9, "invis_burst",
            Map.of(), Map.of(), Map.of("minecraft:invisibility", 1), Map.of(), 0.0, 120.0,
            List.of(universalApex)
        );
        UpgradeNode universalAoE = new UpgradeNode(
            "universal_aoe_8", 80, 8, "aoe",
            Map.of(), Map.of(), Map.of("minecraft:strength", 1), Map.of(), 0.0, 100.0,
            List.of(universalInvis)
        );
        UpgradeNode universalAllResist = new UpgradeNode(
            "universal_allresist_7", 70, 7, "allresist",
            Map.of(), Map.of(), Map.of("minecraft:fire_resistance", 1), Map.of(), 0.0, 90.0,
            List.of(universalAoE)
        );
        UpgradeNode universalImmunity = new UpgradeNode(
            "universal_immunity_6", 60, 6, "immunity",
            Map.of(), Map.of(), Map.of("minecraft:resistance", 1), Map.of(), 0.0, 80.0,
            List.of(universalAllResist)
        );
        UpgradeNode universalAggro = new UpgradeNode(
            "universal_aggro_5", 50, 5, "aggro",
            Map.of(), Map.of(), Map.of(), Map.of(), 0.0, 70.0,
            List.of(universalImmunity)
        );
        UpgradeNode universalKnockback = new UpgradeNode(
            "universal_knockback_4", 40, 4, "knockback",
            Map.of("knockback", 0.5), Map.of(), Map.of(), Map.of(), 0.0, 60.0,
            List.of(universalAggro)
        );
        UpgradeNode universalArmor = new UpgradeNode(
            "universal_armor_3", 30, 3, "armor",
            Map.of("armor", 8.0), Map.of(), Map.of(), Map.of(), 0.0, 50.0,
            List.of(universalKnockback)
        );
        UpgradeNode universalSpeed = new UpgradeNode(
            "universal_speed_2", 20, 2, "speed",
            Map.of("speed", 0.32), Map.of(), Map.of(), Map.of(), 0.0, 40.0,
            List.of(universalArmor)
        );
        UpgradeNode universalHealth = new UpgradeNode(
            "universal_health_1", 10, 1, "health",
            Map.of("health", 30.0), Map.of(), Map.of(), Map.of(), 0.0, 30.0,
            List.of(universalSpeed)
        );
        archetypeSkillTrees.put("universal", List.of(universalHealth));

        // --- Zombie Skill Tree ---
        UpgradeNode zombieHorde = new UpgradeNode(
            "zombie_horde_6", 60, 6, "horde_summon",
            Map.of(), Map.of(), Map.of(), Map.of(), 0.2, 100.0,
            List.of() // End node
        );
        UpgradeNode zombieFortitude = new UpgradeNode(
            "zombie_fortitude_5", 50, 5, "undead_fortitude",
            Map.of("health", 40.0), Map.of(), Map.of("minecraft:regeneration", 1), Map.of(), 0.15, 80.0,
            List.of(zombieHorde)
        );
        UpgradeNode zombieInfect = new UpgradeNode(
            "zombie_infect_4", 40, 4, "infectious_bite",
            Map.of(), Map.of(), Map.of("minecraft:hunger", 1), Map.of(), 0.1, 70.0,
            List.of(zombieFortitude)
        );
        UpgradeNode zombieArmor = new UpgradeNode(
            "zombie_armor_3", 30, 3, "armor",
            Map.of("armor", 4.0), Map.of(EquipmentSlot.CHEST, "minecraft:iron_chestplate"), Map.of(), Map.of("minecraft:protection", 1), 0.1, 60.0,
            List.of(zombieInfect)
        );
        UpgradeNode zombieDamage = new UpgradeNode(
            "zombie_damage_2", 20, 2, "damage",
            Map.of("attack", 7.0), Map.of(EquipmentSlot.MAINHAND, "minecraft:iron_sword"), Map.of(), Map.of("minecraft:sharpness", 1), 0.05, 50.0,
            List.of(zombieArmor)
        );
        UpgradeNode zombieHealth = new UpgradeNode(
            "zombie_health_1", 10, 1, "health",
            Map.of("health", 30.0), Map.of(), Map.of(), Map.of(), 0.0, 40.0,
            List.of(zombieDamage)
        );
        archetypeSkillTrees.put("zombie", List.of(zombieHealth));

        // --- Skeleton Skill Tree ---
        UpgradeNode skeletonSniper = new UpgradeNode(
            "skeleton_sniper_7", 70, 7, "sniper_shot",
            Map.of(), Map.of(), Map.of(), Map.of("minecraft:punch", 1), 0.0, 100.0,
            List.of()
        );
        UpgradeNode skeletonDodge = new UpgradeNode(
            "skeleton_dodge_6", 60, 6, "dodge",
            Map.of(), Map.of(), Map.of("minecraft:speed", 1), Map.of(), 0.0, 80.0,
            List.of(skeletonSniper)
        );
        UpgradeNode skeletonMultiShot = new UpgradeNode(
            "skeleton_multishot_5", 50, 5, "multishot",
            Map.of(), Map.of(), Map.of(), Map.of("minecraft:multishot", 1), 0.0, 70.0,
            List.of(skeletonDodge)
        );
        UpgradeNode skeletonPiercing = new UpgradeNode(
            "skeleton_piercing_4", 40, 4, "piercing_arrows",
            Map.of(), Map.of(), Map.of("minecraft:piercing", 1), Map.of(), 0.0, 60.0,
            List.of(skeletonMultiShot)
        );
        UpgradeNode skeletonArmor = new UpgradeNode(
            "skeleton_armor_3", 30, 3, "armor",
            Map.of("armor", 2.0), Map.of(EquipmentSlot.HEAD, "minecraft:iron_helmet"), Map.of(), Map.of(), 0.0, 50.0,
            List.of(skeletonPiercing)
        );
        UpgradeNode skeletonDamage = new UpgradeNode(
            "skeleton_damage_2", 20, 2, "damage",
            Map.of("attack", 6.0), Map.of(EquipmentSlot.MAINHAND, "minecraft:bow"), Map.of(), Map.of("minecraft:power", 1), 0.0, 40.0,
            List.of(skeletonArmor)
        );
        UpgradeNode skeletonSpeed = new UpgradeNode(
            "skeleton_speed_1", 10, 1, "speed",
            Map.of("speed", 0.28), Map.of(), Map.of(), Map.of(), 0.0, 30.0,
            List.of(skeletonDamage)
        );
        archetypeSkillTrees.put("skeleton", List.of(skeletonSpeed));

        // --- Creeper Skill Tree ---
        UpgradeNode creeperCharged = new UpgradeNode(
            "creeper_charged_6", 60, 6, "charged_explosion",
            Map.of(), Map.of(), Map.of("minecraft:resistance", 1), Map.of(), 0.0, 100.0,
            List.of()
        );
        UpgradeNode creeperChain = new UpgradeNode(
            "creeper_chain_5", 50, 5, "chain_explosion",
            Map.of(), Map.of(), Map.of("minecraft:fire_resistance", 1), Map.of(), 0.0, 80.0,
            List.of(creeperCharged)
        );
        UpgradeNode creeperBlast = new UpgradeNode(
            "creeper_blast_4", 40, 4, "blast_radius",
            Map.of(), Map.of(), Map.of("minecraft:strength", 1), Map.of(), 0.0, 70.0,
            List.of(creeperChain)
        );
        UpgradeNode creeperArmor = new UpgradeNode(
            "creeper_armor_3", 30, 3, "armor",
            Map.of("armor", 2.0), Map.of(EquipmentSlot.CHEST, "minecraft:leather_chestplate"), Map.of(), Map.of(), 0.0, 60.0,
            List.of(creeperBlast)
        );
        UpgradeNode creeperSpeed = new UpgradeNode(
            "creeper_speed_2", 20, 2, "speed",
            Map.of("speed", 0.28), Map.of(), Map.of(), Map.of(), 0.0, 50.0,
            List.of(creeperArmor)
        );
        UpgradeNode creeperHealth = new UpgradeNode(
            "creeper_health_1", 10, 1, "health",
            Map.of("health", 28.0), Map.of(), Map.of(), Map.of(), 0.0, 40.0,
            List.of(creeperSpeed)
        );
        archetypeSkillTrees.put("creeper", List.of(creeperHealth));

        // --- Spider Skill Tree ---
        UpgradeNode spiderVenom = new UpgradeNode(
            "spider_venom_7", 70, 7, "venom_cloud",
            Map.of(), Map.of(), Map.of("minecraft:poison", 2), Map.of(), 0.0, 100.0,
            List.of()
        );
        UpgradeNode spiderLeap = new UpgradeNode(
            "spider_leap_6", 60, 6, "leap_attack",
            Map.of(), Map.of(), Map.of("minecraft:strength", 1), Map.of(), 0.0, 80.0,
            List.of(spiderVenom)
        );
        UpgradeNode spiderWeb = new UpgradeNode(
            "spider_web_5", 50, 5, "web_trap",
            Map.of(), Map.of(), Map.of("minecraft:slowness", 1), Map.of(), 0.0, 64.0,
            List.of(spiderLeap)
        );
        UpgradeNode spiderPoison = new UpgradeNode(
            "spider_poison_4", 40, 4, "poison_bite",
            Map.of(), Map.of(), Map.of("minecraft:poison", 1), Map.of(), 0.0, 56.0,
            List.of(spiderWeb)
        );
        UpgradeNode spiderClimb = new UpgradeNode(
            "spider_climb_3", 30, 3, "climb",
            Map.of(), Map.of(), Map.of(), Map.of("minecraft:feather_falling", 2), 0.0, 48.0,
            List.of(spiderPoison)
        );
        UpgradeNode spiderJump = new UpgradeNode(
            "spider_jump_2", 20, 2, "jump",
            Map.of(), Map.of(), Map.of("minecraft:jump_boost", 1), Map.of(), 0.0, 40.0,
            List.of(spiderClimb)
        );
        UpgradeNode spiderSpeed = new UpgradeNode(
            "spider_speed_1", 10, 1, "speed",
            Map.of("speed", 0.30), Map.of(), Map.of(), Map.of(), 0.0, 32.0,
            List.of(spiderJump)
        );
        archetypeSkillTrees.put("spider", List.of(spiderSpeed));

        // --- Witch Skill Tree ---
        UpgradeNode witchMassHex = new UpgradeNode(
            "witch_mass_hex_7", 70, 7, "mass_hex",
            Map.of(), Map.of(), Map.of("minecraft:slowness", 2), Map.of(), 0.0, 100.0,
            List.of()
        );
        UpgradeNode witchHealAlly = new UpgradeNode(
            "witch_heal_ally_6", 60, 6, "heal_ally",
            Map.of(), Map.of(), Map.of("minecraft:regeneration", 2), Map.of(), 0.0, 90.0,
            List.of(witchMassHex)
        );
        UpgradeNode witchDebuff = new UpgradeNode(
            "witch_debuff_5", 50, 5, "debuff_potions",
            Map.of(), Map.of(), Map.of("minecraft:weakness", 1), Map.of(), 0.0, 80.0,
            List.of(witchHealAlly)
        );
        UpgradeNode witchSplash = new UpgradeNode(
            "witch_splash_4", 40, 4, "splash_range",
            Map.of(), Map.of(), Map.of("minecraft:instant_damage", 1), Map.of(), 0.0, 70.0,
            List.of(witchDebuff)
        );
        UpgradeNode witchResistance = new UpgradeNode(
            "witch_resistance_3", 30, 3, "resistance",
            Map.of("armor", 2.0), Map.of(EquipmentSlot.HEAD, "minecraft:iron_helmet"), Map.of(), Map.of(), 0.0, 60.0,
            List.of(witchSplash)
        );
        UpgradeNode witchPotionPower = new UpgradeNode(
            "witch_potion_power_2", 20, 2, "potion_power",
            Map.of(), Map.of(), Map.of("minecraft:strength", 1), Map.of(), 0.0, 50.0,
            List.of(witchResistance)
        );
        UpgradeNode witchHealth = new UpgradeNode(
            "witch_health_1", 10, 1, "health",
            Map.of("health", 32.0), Map.of(), Map.of(), Map.of(), 0.0, 40.0,
            List.of(witchPotionPower)
        );
        archetypeSkillTrees.put("witch", List.of(witchHealth));

        // --- Illager Skill Tree ---
        UpgradeNode illagerRaidBanner = new UpgradeNode(
            "illager_raid_banner_7", 70, 7, "raid_banner",
            Map.of(), Map.of(), Map.of("minecraft:strength", 2), Map.of(), 0.0, 100.0,
            List.of()
        );
        UpgradeNode illagerTotem = new UpgradeNode(
            "illager_totem_6", 60, 6, "totem_use",
            Map.of(), Map.of(), Map.of(), Map.of("minecraft:unbreaking", 2), 0.0, 90.0,
            List.of(illagerRaidBanner)
        );
        UpgradeNode illagerFangs = new UpgradeNode(
            "illager_fangs_5", 50, 5, "evoker_fangs",
            Map.of(), Map.of(), Map.of("minecraft:instant_damage", 2), Map.of(), 0.0, 80.0,
            List.of(illagerTotem)
        );
        UpgradeNode illagerSummon = new UpgradeNode(
            "illager_summon_4", 40, 4, "summon_ally",
            Map.of(), Map.of(), Map.of(), Map.of("minecraft:looting", 1), 0.0, 70.0,
            List.of(illagerFangs)
        );
        UpgradeNode illagerArmor = new UpgradeNode(
            "illager_armor_3", 30, 3, "armor",
            Map.of("armor", 4.0), Map.of(EquipmentSlot.CHEST, "minecraft:iron_chestplate"), Map.of(), Map.of(), 0.0, 60.0,
            List.of(illagerSummon)
        );
        UpgradeNode illagerDamage = new UpgradeNode(
            "illager_damage_2", 20, 2, "damage",
            Map.of("attack", 8.0), Map.of(EquipmentSlot.MAINHAND, "minecraft:iron_axe"), Map.of(), Map.of("minecraft:sharpness", 1), 0.0, 50.0,
            List.of(illagerArmor)
        );
        UpgradeNode illagerHealth = new UpgradeNode(
            "illager_health_1", 10, 1, "health",
            Map.of("health", 32.0), Map.of(), Map.of(), Map.of(), 0.0, 40.0,
            List.of(illagerDamage)
        );
        archetypeSkillTrees.put("illager", List.of(illagerHealth));

        // --- Nether Skill Tree ---
        UpgradeNode netherHellfire = new UpgradeNode(
            "nether_hellfire_7", 70, 7, "hellfire_burst",
            Map.of(), Map.of(), Map.of("minecraft:fire_resistance", 2), Map.of(), 0.0, 100.0,
            List.of()
        );
        UpgradeNode netherWither = new UpgradeNode(
            "nether_wither_6", 60, 6, "wither_touch",
            Map.of(), Map.of(), Map.of("minecraft:wither", 1), Map.of(), 0.0, 90.0,
            List.of(netherHellfire)
        );
        UpgradeNode netherAura = new UpgradeNode(
            "nether_aura_5", 50, 5, "nether_aura",
            Map.of(), Map.of(), Map.of("minecraft:strength", 2), Map.of(), 0.0, 80.0,
            List.of(netherWither)
        );
        UpgradeNode netherLava = new UpgradeNode(
            "nether_lava_4", 40, 4, "lava_walk",
            Map.of(), Map.of(), Map.of("minecraft:lava_resistance", 1), Map.of(), 0.0, 70.0,
            List.of(netherAura)
        );
        UpgradeNode netherArmor = new UpgradeNode(
            "nether_armor_3", 30, 3, "armor",
            Map.of("armor", 4.0), Map.of(EquipmentSlot.CHEST, "minecraft:golden_chestplate"), Map.of(), Map.of(), 0.0, 60.0,
            List.of(netherLava)
        );
        UpgradeNode netherDamage = new UpgradeNode(
            "nether_damage_2", 20, 2, "damage",
            Map.of("attack", 8.0), Map.of(EquipmentSlot.MAINHAND, "minecraft:golden_sword"), Map.of(), Map.of("minecraft:fire_aspect", 1), 0.0, 50.0,
            List.of(netherArmor)
        );
        UpgradeNode netherFireResist = new UpgradeNode(
            "nether_fire_resist_1", 10, 1, "fire_resist",
            Map.of(), Map.of(), Map.of("minecraft:fire_resistance", 1), Map.of(), 0.0, 40.0,
            List.of(netherDamage)
        );
        archetypeSkillTrees.put("nether", List.of(netherFireResist));

        // --- End Skill Tree ---
        UpgradeNode endVoidPulse = new UpgradeNode(
            "end_void_pulse_7", 70, 7, "void_pulse",
            Map.of(), Map.of(), Map.of("minecraft:resistance", 2), Map.of(), 0.0, 120.0,
            List.of()
        );
        UpgradeNode endBlindness = new UpgradeNode(
            "end_blindness_6", 60, 6, "blindness",
            Map.of(), Map.of(), Map.of("minecraft:blindness", 1), Map.of(), 0.0, 100.0,
            List.of(endVoidPulse)
        );
        UpgradeNode endSwarm = new UpgradeNode(
            "end_swarm_5", 50, 5, "ender_swarm",
            Map.of(), Map.of(), Map.of("minecraft:strength", 2), Map.of(), 0.0, 90.0,
            List.of(endBlindness)
        );
        UpgradeNode endLevitate = new UpgradeNode(
            "end_levitate_4", 40, 4, "levitate_attack",
            Map.of(), Map.of(), Map.of("minecraft:levitation", 2), Map.of(), 0.0, 80.0,
            List.of(endSwarm)
        );
        UpgradeNode endArmor = new UpgradeNode(
            "end_armor_3", 30, 3, "armor",
            Map.of("armor", 4.0), Map.of(EquipmentSlot.CHEST, "minecraft:diamond_chestplate"), Map.of(), Map.of(), 0.0, 70.0,
            List.of(endLevitate)
        );
        UpgradeNode endTeleport = new UpgradeNode(
            "end_teleport_2", 20, 2, "teleport",
            Map.of(), Map.of(), Map.of("minecraft:levitation", 1), Map.of(), 0.0, 60.0,
            List.of(endArmor)
        );
        UpgradeNode endHealth = new UpgradeNode(
            "end_health_1", 10, 1, "health",
            Map.of("health", 40.0), Map.of(), Map.of(), Map.of(), 0.0, 50.0,
            List.of(endTeleport)
        );
        archetypeSkillTrees.put("end", List.of(endHealth));

        // --- Warden Skill Tree ---
        UpgradeNode wardenApex = new UpgradeNode(
            "warden_apex_7", 70, 7, "apex_predator",
            Map.of(), Map.of(), Map.of("minecraft:absorption", 3), Map.of(), 0.0, 300.0,
            List.of()
        );
        UpgradeNode wardenEarthquake = new UpgradeNode(
            "warden_earthquake_6", 60, 6, "earthquake",
            Map.of(), Map.of(), Map.of("minecraft:resistance", 3), Map.of(), 0.0, 240.0,
            List.of(wardenApex)
        );
        UpgradeNode wardenRage = new UpgradeNode(
            "warden_rage_5", 50, 5, "blind_rage",
            Map.of(), Map.of(), Map.of("minecraft:strength", 3), Map.of(), 0.0, 200.0,
            List.of(wardenEarthquake)
        );
        UpgradeNode wardenScent = new UpgradeNode(
            "warden_scent_4", 40, 4, "scent_range",
            Map.of(), Map.of(), Map.of("minecraft:speed", 2), Map.of(), 0.0, 180.0,
            List.of(wardenRage)
        );
        UpgradeNode wardenArmor = new UpgradeNode(
            "warden_armor_3", 30, 3, "armor",
            Map.of("armor", 8.0), Map.of(EquipmentSlot.CHEST, "minecraft:netherite_chestplate"), Map.of(), Map.of(), 0.0, 160.0,
            List.of(wardenScent)
        );
        UpgradeNode wardenSonic = new UpgradeNode(
            "warden_sonic_2", 20, 2, "sonic_boom",
            Map.of(), Map.of(), Map.of("minecraft:sonic_boom", 1), Map.of(), 0.0, 140.0,
            List.of(wardenArmor)
        );
        UpgradeNode wardenHealth = new UpgradeNode(
            "warden_health_1", 10, 1, "health",
            Map.of("health", 100.0), Map.of(), Map.of(), Map.of(), 0.0, 120.0,
            List.of(wardenSonic)
        );
        archetypeSkillTrees.put("warden", List.of(wardenHealth));
    }

    /**
     * Deterministically selects upgrades for a mob using the skill tree, world-seeded randomness, and tier/point logic.
     * Stores all chosen upgrades in PowerProfile.chosenUpgrades.
     * If reroll is triggered (10% on tier-up), priorityPath may change.
     */
    public static void selectUpgrades(PowerProfile profile, String archetype, long worldSeed) {
        profile.chosenUpgrades.clear();
        List<UpgradeNode> roots = archetypeSkillTrees.getOrDefault(archetype, archetypeSkillTrees.get("universal"));
        int points = (int) profile.totalPoints;
        int tier = 0;
        UpgradeNode current = roots.get(0); // Always start at root for now
        long seed = worldSeed
            ^ Double.doubleToLongBits(profile.baseHealth)
            ^ Double.doubleToLongBits(profile.baseDamage)
            ^ Double.doubleToLongBits(profile.baseArmor)
            ^ Double.doubleToLongBits(profile.baseSpeed);
        Random rand = new Random(seed);
        while (current != null && points >= current.cost) {
            profile.chosenUpgrades.add(current.id);
            points -= current.cost;
            tier = Math.max(tier, current.tierReq);
            if (current.children.isEmpty()) break;
            // Randomly select next child (deterministic by worldSeed)
            current = current.children.get(rand.nextInt(current.children.size()));
        }
        profile.tierLevel = tier;
    }

    /**
     * 10% chance to reroll priority path on tier-up, using world-seeded randomness.
     */
    public static void maybeRerollPriorityPath(PowerProfile profile, long worldSeed) {
        long seed = worldSeed
            ^ Double.doubleToLongBits(profile.baseHealth)
            ^ Double.doubleToLongBits(profile.baseDamage)
            ^ Double.doubleToLongBits(profile.baseArmor)
            ^ Double.doubleToLongBits(profile.baseSpeed)
            ^ profile.tierLevel;
        Random rand = new Random(seed);
        if (rand.nextDouble() < 0.10) {
            // For now, just pick a random available archetype as new path
            List<String> keys = new java.util.ArrayList<>(archetypeSkillTrees.keySet());
            profile.priorityPath = keys.get(rand.nextInt(keys.size()));
        }
    }
}
