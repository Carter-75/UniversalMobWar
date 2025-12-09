package mod.universalmobwar.mixin.mob;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Breeze - Hostile mob with ranged tree
 * 
 * Breeze from skilltree.txt:
 * - type: hostile
 * - weapon: none
 * - armor: none
 * - shield: false
 * - trees: ["r"] (ranged tree)
 * 
 * Available upgrades:
 * 1. hostile_and_neutral_potion_effects:
 *    - healing (5 levels), health_boost (10 levels), resistance (3 levels),
 *    - strength (4 levels), speed (3 levels), invisibility_on_hit (5 levels)
 * 
 * 2. Ranged tree (shared_trees.ranged_r):
 *    - Piercing_Shot: 4 levels (cost 8, 12, 16, 20) -> pierce 1-4
 *    - Bow_Potion_Mastery: 5 levels (cost 10, 15, 20, 25, 30) -> potion effects on projectiles
 *    - Multishot: 3 levels (cost 15, 25, 35) -> +1, +2, +3 extra projectiles
 * 
 * NO item masteries (no equipment)
 */
@Mixin(BreezeEntity.class)
public abstract class BreezeMixin {

    // ========== NBT Data Storage ==========
    @Unique
    private static final String NBT_KEY = "UniversalMobWar_Breeze";
    
    @Unique
    private int totalPoints = 0;
    
    @Unique
    private int spentPoints = 0;
    
    // Hostile/Neutral potion effect levels
    @Unique
    private int healingLevel = 0;        // 0-5
    @Unique
    private int healthBoostLevel = 0;    // 0-10
    @Unique
    private int resistanceLevel = 0;     // 0-3
    @Unique
    private int strengthLevel = 0;       // 0-4
    @Unique
    private int speedLevel = 0;          // 0-3
    @Unique
    private int invisOnHitLevel = 0;     // 0-5
    
    // Ranged tree levels
    @Unique
    private int piercingShotLevel = 0;       // 0-4
    @Unique
    private int bowPotionMasteryLevel = 0;   // 0-5
    @Unique
    private int multishotLevel = 0;          // 0-3
    
    // Cooldown tracking
    @Unique
    private long lastDamageRegenTrigger = 0;
    @Unique
    private long lastInvisibilityTrigger = 0;

    // ========== Point System ==========
    
    @Unique
    private int calculateWorldAgePoints(World world) {
        long worldTime = world.getTimeOfDay();
        int worldDays = (int) (worldTime / 24000L);
        
        double points = 0.0;
        for (int day = 1; day <= worldDays; day++) {
            if (day <= 10) {
                points += 0.1;
            } else if (day <= 15) {
                points += 0.5;
            } else if (day <= 20) {
                points += 1.0;
            } else if (day <= 25) {
                points += 1.5;
            } else if (day <= 30) {
                points += 3.0;
            } else {
                points += 5.0;
            }
        }
        return (int) points;
    }
    
    @Unique
    private int getBudget() {
        return totalPoints - spentPoints;
    }

    // ========== Upgrade Costs ==========
    
    // Hostile/Neutral potion effects
    @Unique
    private int getHealingCost(int level) {
        if (level >= 1 && level <= 5) return level;
        return Integer.MAX_VALUE;
    }
    
    @Unique
    private int getHealthBoostCost(int level) {
        if (level >= 1 && level <= 10) return level + 1;
        return Integer.MAX_VALUE;
    }
    
    @Unique
    private int getResistanceCost(int level) {
        return switch (level) {
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 8;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getStrengthCost(int level) {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 9;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getSpeedCost(int level) {
        return switch (level) {
            case 1 -> 6;
            case 2 -> 9;
            case 3 -> 12;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getInvisOnHitCost(int level) {
        return switch (level) {
            case 1 -> 8;
            case 2 -> 12;
            case 3 -> 16;
            case 4 -> 20;
            case 5 -> 25;
            default -> Integer.MAX_VALUE;
        };
    }
    
    // Ranged tree costs
    @Unique
    private int getPiercingShotCost(int level) {
        return switch (level) {
            case 1 -> 8;
            case 2 -> 12;
            case 3 -> 16;
            case 4 -> 20;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getBowPotionMasteryCost(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 15;
            case 3 -> 20;
            case 4 -> 25;
            case 5 -> 30;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getMultishotCost(int level) {
        return switch (level) {
            case 1 -> 15;
            case 2 -> 25;
            case 3 -> 35;
            default -> Integer.MAX_VALUE;
        };
    }

    // ========== Spending Logic ==========
    
    @Unique
    private void spendPoints(BreezeEntity self) {
        java.util.Random random = new java.util.Random();
        
        while (getBudget() > 0) {
            java.util.List<Runnable> affordableUpgrades = new java.util.ArrayList<>();
            
            // Hostile/Neutral potion effects
            int nextHealLevel = healingLevel + 1;
            if (nextHealLevel <= 5 && getBudget() >= getHealingCost(nextHealLevel)) {
                final int cost = getHealingCost(nextHealLevel);
                affordableUpgrades.add(() -> { healingLevel++; spentPoints += cost; });
            }
            
            int nextHealthLevel = healthBoostLevel + 1;
            if (nextHealthLevel <= 10 && getBudget() >= getHealthBoostCost(nextHealthLevel)) {
                final int cost = getHealthBoostCost(nextHealthLevel);
                affordableUpgrades.add(() -> { healthBoostLevel++; spentPoints += cost; });
            }
            
            int nextResistLevel = resistanceLevel + 1;
            if (nextResistLevel <= 3 && getBudget() >= getResistanceCost(nextResistLevel)) {
                final int cost = getResistanceCost(nextResistLevel);
                affordableUpgrades.add(() -> { resistanceLevel++; spentPoints += cost; });
            }
            
            int nextStrengthLevel = strengthLevel + 1;
            if (nextStrengthLevel <= 4 && getBudget() >= getStrengthCost(nextStrengthLevel)) {
                final int cost = getStrengthCost(nextStrengthLevel);
                affordableUpgrades.add(() -> { strengthLevel++; spentPoints += cost; });
            }
            
            int nextSpeedLevel = speedLevel + 1;
            if (nextSpeedLevel <= 3 && getBudget() >= getSpeedCost(nextSpeedLevel)) {
                final int cost = getSpeedCost(nextSpeedLevel);
                affordableUpgrades.add(() -> { speedLevel++; spentPoints += cost; });
            }
            
            int nextInvisLevel = invisOnHitLevel + 1;
            if (nextInvisLevel <= 5 && getBudget() >= getInvisOnHitCost(nextInvisLevel)) {
                final int cost = getInvisOnHitCost(nextInvisLevel);
                affordableUpgrades.add(() -> { invisOnHitLevel++; spentPoints += cost; });
            }
            
            // Ranged tree
            int nextPierceLevel = piercingShotLevel + 1;
            if (nextPierceLevel <= 4 && getBudget() >= getPiercingShotCost(nextPierceLevel)) {
                final int cost = getPiercingShotCost(nextPierceLevel);
                affordableUpgrades.add(() -> { piercingShotLevel++; spentPoints += cost; });
            }
            
            int nextBowPotionLevel = bowPotionMasteryLevel + 1;
            if (nextBowPotionLevel <= 5 && getBudget() >= getBowPotionMasteryCost(nextBowPotionLevel)) {
                final int cost = getBowPotionMasteryCost(nextBowPotionLevel);
                affordableUpgrades.add(() -> { bowPotionMasteryLevel++; spentPoints += cost; });
            }
            
            int nextMultiLevel = multishotLevel + 1;
            if (nextMultiLevel <= 3 && getBudget() >= getMultishotCost(nextMultiLevel)) {
                final int cost = getMultishotCost(nextMultiLevel);
                affordableUpgrades.add(() -> { multishotLevel++; spentPoints += cost; });
            }
            
            if (affordableUpgrades.isEmpty()) {
                break;
            }
            
            if (random.nextDouble() < 0.20) {
                break;
            }
            
            int index = random.nextInt(affordableUpgrades.size());
            affordableUpgrades.get(index).run();
        }
        
        applyEffects(self);
    }
    
    // ========== Effect Application ==========
    
    @Unique
    private void applyEffects(BreezeEntity self) {
        // Healing
        if (healingLevel >= 1) {
            int regenAmplifier = Math.min(healingLevel - 1, 1);
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.REGENERATION, Integer.MAX_VALUE, regenAmplifier, false, false, true
            ));
        }
        
        // Health Boost
        if (healthBoostLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.HEALTH_BOOST, Integer.MAX_VALUE, healthBoostLevel - 1, false, false, true
            ));
        }
        
        // Resistance
        if (resistanceLevel > 0) {
            int resistAmplifier = Math.min(resistanceLevel - 1, 1);
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE, Integer.MAX_VALUE, resistAmplifier, false, false, true
            ));
            if (resistanceLevel >= 3) {
                self.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true
                ));
            }
        }
        
        // Strength
        if (strengthLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.STRENGTH, Integer.MAX_VALUE, strengthLevel - 1, false, false, true
            ));
        }
        
        // Speed
        if (speedLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED, Integer.MAX_VALUE, speedLevel - 1, false, false, true
            ));
        }
        
        // Ranged tree effects are applied during projectile creation (not status effects)
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
        modData.putInt("piercingShotLevel", piercingShotLevel);
        modData.putInt("bowPotionMasteryLevel", bowPotionMasteryLevel);
        modData.putInt("multishotLevel", multishotLevel);
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
            piercingShotLevel = modData.getInt("piercingShotLevel");
            bowPotionMasteryLevel = modData.getInt("bowPotionMasteryLevel");
            multishotLevel = modData.getInt("multishotLevel");
            lastDamageRegenTrigger = modData.getLong("lastDamageRegenTrigger");
            lastInvisibilityTrigger = modData.getLong("lastInvisibilityTrigger");
            
            applyEffects((BreezeEntity) (Object) this);
        }
    }

    // ========== Tick Handling ==========
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        BreezeEntity self = (BreezeEntity) (Object) this;
        World world = self.getWorld();
        
        if (world.isClient()) {
            return;
        }
        
        int newTotalPoints = calculateWorldAgePoints(world);
        
        if (newTotalPoints > totalPoints) {
            totalPoints = newTotalPoints;
            if (getBudget() > 0) {
                spendPoints(self);
            }
        }
    }
    
    // ========== Getter methods for ranged abilities (for integration later) ==========
    
    @Unique
    public int getPiercingShotLevel() { return piercingShotLevel; }
    
    @Unique
    public int getBowPotionMasteryLevel() { return bowPotionMasteryLevel; }
    
    @Unique
    public int getMultishotLevel() { return multishotLevel; }
    
    @Unique
    public int getInvisOnHitLevel() { return invisOnHitLevel; }
}
