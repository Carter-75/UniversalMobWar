package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Axolotl - Neutral mob
 * 
 * Uses MobWarData for persistence (handled by MobDataMixin on MobEntity)
 * 
 * Available upgrades (hostile_and_neutral_potion_effects):
 * - healing: 5 levels (cost 1-5)
 * - health_boost: 10 levels (cost 2-11)
 * - resistance: 3 levels (cost 4, 6, 8)
 * - strength: 4 levels (cost 3, 5, 7, 9)
 * - speed: 3 levels (cost 6, 9, 12)
 */
@Mixin(AxolotlEntity.class)
public abstract class AxolotlMixin {

    @Unique
    private static final String DATA_KEY = "axolotl_upgrades";
    
    @Unique
    private long lastProcessTick = 0;

    // ========== Point Calculation ==========
    
    @Unique
    private int calculateWorldAgePoints(World world) {
        long worldTime = world.getTime();
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

    // ========== Upgrade Costs ==========
    
    @Unique
    private int getHealingCost(int level) {
        return level >= 1 && level <= 5 ? level : Integer.MAX_VALUE;
    }
    
    @Unique
    private int getHealthBoostCost(int level) {
        return level >= 1 && level <= 10 ? level + 1 : Integer.MAX_VALUE;
    }
    
    @Unique
    private int getResistanceCost(int level) {
        return switch (level) {
            case 1 -> 4; case 2 -> 6; case 3 -> 8;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getStrengthCost(int level) {
        return switch (level) {
            case 1 -> 3; case 2 -> 5; case 3 -> 7; case 4 -> 9;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getSpeedCost(int level) {
        return switch (level) {
            case 1 -> 6; case 2 -> 9; case 3 -> 12;
            default -> Integer.MAX_VALUE;
        };
    }

    // ========== Tick Processing ==========
    
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        AxolotlEntity self = (AxolotlEntity) (Object) this;
        World world = self.getWorld();
        
        if (world.isClient()) return;
        
        long currentTick = world.getTime();
        
        // Only process every 100 ticks (5 seconds) for performance
        if (currentTick - lastProcessTick < 100) return;
        lastProcessTick = currentTick;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        NbtCompound skillData = data.getSkillData();
        
        // Initialize if needed
        if (!skillData.contains(DATA_KEY)) {
            NbtCompound upgrades = new NbtCompound();
            upgrades.putInt("healing", 0);
            upgrades.putInt("health_boost", 0);
            upgrades.putInt("resistance", 0);
            upgrades.putInt("strength", 0);
            upgrades.putInt("speed", 0);
            skillData.put(DATA_KEY, upgrades);
        }
        
        // Calculate available points
        int totalPoints = calculateWorldAgePoints(world);
        int spentPoints = (int) data.getSpentPoints();
        int budget = totalPoints - spentPoints;
        
        if (budget <= 0) {
            applyEffects(self, skillData.getCompound(DATA_KEY));
            return;
        }
        
        // Spending logic
        NbtCompound upgrades = skillData.getCompound(DATA_KEY);
        java.util.Random random = new java.util.Random();
        
        while (budget > 0) {
            java.util.List<Runnable> affordable = new java.util.ArrayList<>();
            
            int healLvl = upgrades.getInt("healing");
            if (healLvl < 5 && budget >= getHealingCost(healLvl + 1)) {
                final int cost = getHealingCost(healLvl + 1);
                affordable.add(() -> {
                    upgrades.putInt("healing", healLvl + 1);
                    data.setSpentPoints(data.getSpentPoints() + cost);
                });
            }
            
            int hpLvl = upgrades.getInt("health_boost");
            if (hpLvl < 10 && budget >= getHealthBoostCost(hpLvl + 1)) {
                final int cost = getHealthBoostCost(hpLvl + 1);
                affordable.add(() -> {
                    upgrades.putInt("health_boost", hpLvl + 1);
                    data.setSpentPoints(data.getSpentPoints() + cost);
                });
            }
            
            int resLvl = upgrades.getInt("resistance");
            if (resLvl < 3 && budget >= getResistanceCost(resLvl + 1)) {
                final int cost = getResistanceCost(resLvl + 1);
                affordable.add(() -> {
                    upgrades.putInt("resistance", resLvl + 1);
                    data.setSpentPoints(data.getSpentPoints() + cost);
                });
            }
            
            int strLvl = upgrades.getInt("strength");
            if (strLvl < 4 && budget >= getStrengthCost(strLvl + 1)) {
                final int cost = getStrengthCost(strLvl + 1);
                affordable.add(() -> {
                    upgrades.putInt("strength", strLvl + 1);
                    data.setSpentPoints(data.getSpentPoints() + cost);
                });
            }
            
            int spdLvl = upgrades.getInt("speed");
            if (spdLvl < 3 && budget >= getSpeedCost(spdLvl + 1)) {
                final int cost = getSpeedCost(spdLvl + 1);
                affordable.add(() -> {
                    upgrades.putInt("speed", spdLvl + 1);
                    data.setSpentPoints(data.getSpentPoints() + cost);
                });
            }
            
            if (affordable.isEmpty()) break;
            if (random.nextDouble() < 0.20) break; // 20% save chance
            
            affordable.get(random.nextInt(affordable.size())).run();
            budget = totalPoints - (int) data.getSpentPoints();
        }
        
        // Save and apply
        skillData.put(DATA_KEY, upgrades);
        data.setSkillData(skillData);
        MobWarData.save((MobEntity) self, data);
        applyEffects(self, upgrades);
    }
    
    @Unique
    private void applyEffects(AxolotlEntity self, NbtCompound upgrades) {
        int healing = upgrades.getInt("healing");
        int healthBoost = upgrades.getInt("health_boost");
        int resistance = upgrades.getInt("resistance");
        int strength = upgrades.getInt("strength");
        int speed = upgrades.getInt("speed");
        
        // Apply regeneration
        if (healing > 0) {
            int amp = Math.min(healing - 1, 1); // Regen I or II
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, amp, false, false, true));
        }
        
        // Apply health boost
        if (healthBoost > 0) {
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 200, healthBoost - 1, false, false, true));
        }
        
        // Apply resistance
        if (resistance > 0) {
            int amp = Math.min(resistance - 1, 1);
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, amp, false, false, true));
            if (resistance >= 3) {
                self.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0, false, false, true));
            }
        }
        
        // Apply strength
        if (strength > 0) {
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, strength - 1, false, false, true));
        }
        
        // Apply speed
        if (speed > 0) {
            self.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, speed - 1, false, false, true));
        }
    }
}
