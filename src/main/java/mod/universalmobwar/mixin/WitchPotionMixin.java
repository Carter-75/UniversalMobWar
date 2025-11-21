package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(WitchEntity.class)
public abstract class WitchPotionMixin {

    @Inject(method = "shootAt", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$onAttack(LivingEntity target, float pullProgress, CallbackInfo ci) {
        WitchEntity witch = (WitchEntity)(Object)this;
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(witch);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("witch_potion_mastery", 0);
        if (level <= 0) return;
        
        // Chance: "normal curve" 0% -> 100%
        double p = 1.0 / (1.0 + Math.exp(-1.0 * (level - 5.0)));
        
        if (witch.getRandom().nextDouble() < p) {
            // Throw special potion
            RegistryEntry<Potion> potion = Potions.POISON;
            
            double center = (level / 10.0) * 7.0;
            int pick = (int) Math.round(center + witch.getRandom().nextGaussian() * 1.5);
            if (pick < 0) pick = 0;
            if (pick > 7) pick = 7;

            switch (pick) {
                case 0 -> potion = Potions.WEAKNESS;
                case 1 -> potion = Potions.SLOWNESS;
                case 2 -> potion = Potions.POISON;
                case 3 -> potion = Potions.HARMING;
                case 4 -> potion = Potions.STRONG_SLOWNESS;
                case 5 -> potion = Potions.WEAKNESS; // Blindness custom
                case 6 -> potion = Potions.WEAKNESS; // Nausea custom
                case 7 -> potion = Potions.WEAKNESS; // Wither custom (Decay not in Potions)
            }
            
            ItemStack stack = new ItemStack(Items.SPLASH_POTION);
            
            if (pick == 5) { // Blindness
                 stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(potion), Optional.empty(), 
                     List.of(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.BLINDNESS, 200, 0))));
            } else if (pick == 6) { // Nausea
                 stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(potion), Optional.empty(), 
                     List.of(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.NAUSEA, 200, 0))));
            } else if (pick == 7) { // Wither
                 stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(potion), Optional.empty(), 
                     List.of(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WITHER, 200, 0))));
            } else {
                 stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));
            }
            
            PotionEntity potionEntity = new PotionEntity(witch.getWorld(), witch);
            potionEntity.setItem(stack);
            
            // Velocity logic from WitchEntity
            double d = target.getX() + target.getVelocity().x - witch.getX();
            double e = target.getEyeY() - 1.100000023841858D - witch.getY();
            double f = target.getZ() + target.getVelocity().z - witch.getZ();
            double g = Math.sqrt(d * d + f * f);
            potionEntity.setVelocity(d, e + g * 0.20000000298023224D, f, 0.75F, 8.0F);
            
            witch.getWorld().spawnEntity(potionEntity);
            
            ci.cancel(); // Skip vanilla attack
        }
    }
}
