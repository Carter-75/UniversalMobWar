package mod.universalmobwar.mixin.mob;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BoggedEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Bogged - Hostile mob with bow, full armor, shield, and zombie + ranged trees
 * 
 * Bogged from skilltree.txt:
 * - type: hostile
 * - weapon: bow
 * - armor: full_normal (helmet, chestplate, leggings, boots)
 * - shield: true
 * - trees: ["z", "r"] (zombie tree and ranged tree)
 * 
 * Available upgrades:
 * 1. hostile_and_neutral_potion_effects (healing, health_boost, resistance, strength, speed, invis_on_hit)
 * 2. item_masteries (drop_mastery, durability_mastery) - since has equipment
 * 3. Zombie tree (shared_trees.zombie_z):
 *    - Horde_Summon: 5 levels (cost 10-30) -> 10-50% chance
 *    - Hunger_Attack: 3 levels (cost 6, 10, 14) -> Hunger I-III on hit
 * 4. Ranged tree (shared_trees.ranged_r):
 *    - Piercing_Shot: 4 levels (cost 8-20) -> pierce 1-4
 *    - Bow_Potion_Mastery: 5 levels (cost 10-30) -> potion effects on arrows
 *    - Multishot: 3 levels (cost 15-35) -> +1-3 extra projectiles
 * 5. Weapon progression (bow)
 * 6. Armor progression (full_normal)
 * 7. Shield (cost 10)
 * 8. Enchantments for all equipment
 */
@Mixin(BoggedEntity.class)
public abstract class BoggedMixin {

    // ========== NBT Data Storage ==========
    @Unique
    private static final String NBT_KEY = "UniversalMobWar_Bogged";
    
    @Unique private int totalPoints = 0;
    @Unique private int spentPoints = 0;
    
    // Hostile/Neutral potion effect levels
    @Unique private int healingLevel = 0;        // 0-5
    @Unique private int healthBoostLevel = 0;    // 0-10
    @Unique private int resistanceLevel = 0;     // 0-3
    @Unique private int strengthLevel = 0;       // 0-4
    @Unique private int speedLevel = 0;          // 0-3
    @Unique private int invisOnHitLevel = 0;     // 0-5
    
    // Item masteries (apply to all equipment)
    @Unique private int dropMasteryLevel = 0;       // 0-10
    @Unique private int durabilityMasteryLevel = 0; // 0-10
    
    // Zombie tree
    @Unique private int hordeSummonLevel = 0;    // 0-5
    @Unique private int hungerAttackLevel = 0;   // 0-3
    
    // Ranged tree
    @Unique private int piercingShotLevel = 0;       // 0-4
    @Unique private int bowPotionMasteryLevel = 0;   // 0-5
    @Unique private int multishotLevel = 0;          // 0-3
    
    // Equipment tiers (0 = none, then material tiers)
    @Unique private int bowTier = 0;             // 0 = none, 1 = bow
    @Unique private int helmetTier = 0;          // 0-5 (none, leather, chainmail, iron, diamond, netherite)
    @Unique private int chestplateTier = 0;      // 0-5
    @Unique private int leggingsTier = 0;        // 0-5
    @Unique private int bootsTier = 0;           // 0-5
    @Unique private boolean hasShield = false;
    
    // Cooldown tracking
    @Unique private long lastDamageRegenTrigger = 0;
    @Unique private long lastInvisibilityTrigger = 0;

    // ========== Point System ==========
    
    @Unique
    private int calculateWorldAgePoints(World world) {
        long worldTime = world.getTimeOfDay();
        int worldDays = (int) (worldTime / 24000L);
        
        double points = 0.0;
        for (int day = 1; day <= worldDays; day++) {
            if (day <= 10) points += 0.1;
            else if (day <= 15) points += 0.5;
            else if (day <= 20) points += 1.0;
            else if (day <= 25) points += 1.5;
            else if (day <= 30) points += 3.0;
            else points += 5.0;
        }
        return (int) points;
    }
    
    @Unique private int getBudget() { return totalPoints - spentPoints; }

    // ========== Upgrade Costs ==========
    
    // Potion effects
    @Unique private int getHealingCost(int level) { return (level >= 1 && level <= 5) ? level : Integer.MAX_VALUE; }
    @Unique private int getHealthBoostCost(int level) { return (level >= 1 && level <= 10) ? level + 1 : Integer.MAX_VALUE; }
    @Unique private int getResistanceCost(int level) { return switch(level) { case 1->4; case 2->6; case 3->8; default->Integer.MAX_VALUE; }; }
    @Unique private int getStrengthCost(int level) { return switch(level) { case 1->3; case 2->5; case 3->7; case 4->9; default->Integer.MAX_VALUE; }; }
    @Unique private int getSpeedCost(int level) { return switch(level) { case 1->6; case 2->9; case 3->12; default->Integer.MAX_VALUE; }; }
    @Unique private int getInvisOnHitCost(int level) { return switch(level) { case 1->8; case 2->12; case 3->16; case 4->20; case 5->25; default->Integer.MAX_VALUE; }; }
    
    // Item masteries
    @Unique private int getDropMasteryCost(int level) { return switch(level) { case 1->5; case 2->7; case 3->9; case 4->11; case 5->13; case 6->15; case 7->17; case 8->19; case 9->21; case 10->23; default->Integer.MAX_VALUE; }; }
    @Unique private int getDurabilityMasteryCost(int level) { return switch(level) { case 1->10; case 2->12; case 3->14; case 4->16; case 5->18; case 6->20; case 7->22; case 8->24; case 9->26; case 10->28; default->Integer.MAX_VALUE; }; }
    
    // Zombie tree
    @Unique private int getHordeSummonCost(int level) { return switch(level) { case 1->10; case 2->15; case 3->20; case 4->25; case 5->30; default->Integer.MAX_VALUE; }; }
    @Unique private int getHungerAttackCost(int level) { return switch(level) { case 1->6; case 2->10; case 3->14; default->Integer.MAX_VALUE; }; }
    
    // Ranged tree
    @Unique private int getPiercingShotCost(int level) { return switch(level) { case 1->8; case 2->12; case 3->16; case 4->20; default->Integer.MAX_VALUE; }; }
    @Unique private int getBowPotionMasteryCost(int level) { return switch(level) { case 1->10; case 2->15; case 3->20; case 4->25; case 5->30; default->Integer.MAX_VALUE; }; }
    @Unique private int getMultishotCost(int level) { return switch(level) { case 1->15; case 2->25; case 3->35; default->Integer.MAX_VALUE; }; }
    
    // Equipment - Bow is free for Bogged (they spawn with it)
    @Unique private int getShieldCost() { return 10; }
    @Unique private int getArmorTierCost(int tier) { return switch(tier) { case 1->3; case 2->4; case 3->5; case 4->6; case 5->7; default->Integer.MAX_VALUE; }; }

    // ========== Spending Logic ==========
    
    @Unique
    private void spendPoints(BoggedEntity self) {
        java.util.Random random = new java.util.Random();
        
        while (getBudget() > 0) {
            java.util.List<Runnable> affordableUpgrades = new java.util.ArrayList<>();
            
            // Potion effects
            addIfAffordable(affordableUpgrades, healingLevel + 1, 5, this::getHealingCost, () -> healingLevel++);
            addIfAffordable(affordableUpgrades, healthBoostLevel + 1, 10, this::getHealthBoostCost, () -> healthBoostLevel++);
            addIfAffordable(affordableUpgrades, resistanceLevel + 1, 3, this::getResistanceCost, () -> resistanceLevel++);
            addIfAffordable(affordableUpgrades, strengthLevel + 1, 4, this::getStrengthCost, () -> strengthLevel++);
            addIfAffordable(affordableUpgrades, speedLevel + 1, 3, this::getSpeedCost, () -> speedLevel++);
            addIfAffordable(affordableUpgrades, invisOnHitLevel + 1, 5, this::getInvisOnHitCost, () -> invisOnHitLevel++);
            
            // Item masteries
            addIfAffordable(affordableUpgrades, dropMasteryLevel + 1, 10, this::getDropMasteryCost, () -> dropMasteryLevel++);
            addIfAffordable(affordableUpgrades, durabilityMasteryLevel + 1, 10, this::getDurabilityMasteryCost, () -> durabilityMasteryLevel++);
            
            // Zombie tree
            addIfAffordable(affordableUpgrades, hordeSummonLevel + 1, 5, this::getHordeSummonCost, () -> hordeSummonLevel++);
            addIfAffordable(affordableUpgrades, hungerAttackLevel + 1, 3, this::getHungerAttackCost, () -> hungerAttackLevel++);
            
            // Ranged tree
            addIfAffordable(affordableUpgrades, piercingShotLevel + 1, 4, this::getPiercingShotCost, () -> piercingShotLevel++);
            addIfAffordable(affordableUpgrades, bowPotionMasteryLevel + 1, 5, this::getBowPotionMasteryCost, () -> bowPotionMasteryLevel++);
            addIfAffordable(affordableUpgrades, multishotLevel + 1, 3, this::getMultishotCost, () -> multishotLevel++);
            
            // Equipment
            addIfAffordable(affordableUpgrades, helmetTier + 1, 5, this::getArmorTierCost, () -> helmetTier++);
            addIfAffordable(affordableUpgrades, chestplateTier + 1, 5, this::getArmorTierCost, () -> chestplateTier++);
            addIfAffordable(affordableUpgrades, leggingsTier + 1, 5, this::getArmorTierCost, () -> leggingsTier++);
            addIfAffordable(affordableUpgrades, bootsTier + 1, 5, this::getArmorTierCost, () -> bootsTier++);
            
            // Shield
            if (!hasShield && getBudget() >= getShieldCost()) {
                final int cost = getShieldCost();
                affordableUpgrades.add(() -> { hasShield = true; spentPoints += cost; });
            }
            
            if (affordableUpgrades.isEmpty()) break;
            if (random.nextDouble() < 0.20) break;
            
            int index = random.nextInt(affordableUpgrades.size());
            affordableUpgrades.get(index).run();
        }
        
        applyEffects(self);
    }
    
    @Unique
    private void addIfAffordable(java.util.List<Runnable> list, int nextLevel, int maxLevel, 
                                  java.util.function.IntFunction<Integer> costFunc, Runnable upgrade) {
        if (nextLevel <= maxLevel && getBudget() >= costFunc.apply(nextLevel)) {
            final int cost = costFunc.apply(nextLevel);
            list.add(() -> { upgrade.run(); spentPoints += cost; });
        }
    }
    
    // ========== Effect Application ==========
    
    @Unique
    private void applyEffects(BoggedEntity self) {
        // Healing
        if (healingLevel >= 1) {
            int regenAmplifier = Math.min(healingLevel - 1, 1);
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, regenAmplifier, false, false, true));
        }
        
        // Health Boost
        if (healthBoostLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, Integer.MAX_VALUE, healthBoostLevel - 1, false, false, true));
        }
        
        // Resistance
        if (resistanceLevel > 0) {
            int resistAmplifier = Math.min(resistanceLevel - 1, 1);
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, resistAmplifier, false, false, true));
            if (resistanceLevel >= 3) {
                self.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
            }
        }
        
        // Strength
        if (strengthLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, strengthLevel - 1, false, false, true));
        }
        
        // Speed
        if (speedLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, speedLevel - 1, false, false, true));
        }
        
        // Equipment application would be handled by integration code later
    }

    // ========== NBT Persistence ==========
    
    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound modData = new NbtCompound();
        modData.putInt("totalPoints", totalPoints);
        modData.putInt("spentPoints", spentPoints);
        modData.putInt("healingLevel", healingLevel);
        modData.putInt("healthBoostLevel", healthBoostLevel);
        modData.putInt("resistanceLevel", resistanceLevel);
        modData.putInt("strengthLevel", strengthLevel);
        modData.putInt("speedLevel", speedLevel);
        modData.putInt("invisOnHitLevel", invisOnHitLevel);
        modData.putInt("dropMasteryLevel", dropMasteryLevel);
        modData.putInt("durabilityMasteryLevel", durabilityMasteryLevel);
        modData.putInt("hordeSummonLevel", hordeSummonLevel);
        modData.putInt("hungerAttackLevel", hungerAttackLevel);
        modData.putInt("piercingShotLevel", piercingShotLevel);
        modData.putInt("bowPotionMasteryLevel", bowPotionMasteryLevel);
        modData.putInt("multishotLevel", multishotLevel);
        modData.putInt("bowTier", bowTier);
        modData.putInt("helmetTier", helmetTier);
        modData.putInt("chestplateTier", chestplateTier);
        modData.putInt("leggingsTier", leggingsTier);
        modData.putInt("bootsTier", bootsTier);
        modData.putBoolean("hasShield", hasShield);
        modData.putLong("lastDamageRegenTrigger", lastDamageRegenTrigger);
        modData.putLong("lastInvisibilityTrigger", lastInvisibilityTrigger);
        nbt.put(NBT_KEY, modData);
    }
    
    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(NBT_KEY)) {
            NbtCompound modData = nbt.getCompound(NBT_KEY);
            totalPoints = modData.getInt("totalPoints");
            spentPoints = modData.getInt("spentPoints");
            healingLevel = modData.getInt("healingLevel");
            healthBoostLevel = modData.getInt("healthBoostLevel");
            resistanceLevel = modData.getInt("resistanceLevel");
            strengthLevel = modData.getInt("strengthLevel");
            speedLevel = modData.getInt("speedLevel");
            invisOnHitLevel = modData.getInt("invisOnHitLevel");
            dropMasteryLevel = modData.getInt("dropMasteryLevel");
            durabilityMasteryLevel = modData.getInt("durabilityMasteryLevel");
            hordeSummonLevel = modData.getInt("hordeSummonLevel");
            hungerAttackLevel = modData.getInt("hungerAttackLevel");
            piercingShotLevel = modData.getInt("piercingShotLevel");
            bowPotionMasteryLevel = modData.getInt("bowPotionMasteryLevel");
            multishotLevel = modData.getInt("multishotLevel");
            bowTier = modData.getInt("bowTier");
            helmetTier = modData.getInt("helmetTier");
            chestplateTier = modData.getInt("chestplateTier");
            leggingsTier = modData.getInt("leggingsTier");
            bootsTier = modData.getInt("bootsTier");
            hasShield = modData.getBoolean("hasShield");
            lastDamageRegenTrigger = modData.getLong("lastDamageRegenTrigger");
            lastInvisibilityTrigger = modData.getLong("lastInvisibilityTrigger");
            
            applyEffects((BoggedEntity) (Object) this);
        }
    }

    // ========== Tick Handling ==========
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        BoggedEntity self = (BoggedEntity) (Object) this;
        World world = self.getWorld();
        
        if (world.isClient()) return;
        
        int newTotalPoints = calculateWorldAgePoints(world);
        if (newTotalPoints > totalPoints) {
            totalPoints = newTotalPoints;
            if (getBudget() > 0) spendPoints(self);
        }
    }
    
    // ========== Getters for integration ==========
    @Unique public int getHordeSummonLevel() { return hordeSummonLevel; }
    @Unique public int getHungerAttackLevel() { return hungerAttackLevel; }
    @Unique public int getPiercingShotLevel() { return piercingShotLevel; }
    @Unique public int getBowPotionMasteryLevel() { return bowPotionMasteryLevel; }
    @Unique public int getMultishotLevel() { return multishotLevel; }
    @Unique public int getDropMasteryLevel() { return dropMasteryLevel; }
    @Unique public int getDurabilityMasteryLevel() { return durabilityMasteryLevel; }
}
