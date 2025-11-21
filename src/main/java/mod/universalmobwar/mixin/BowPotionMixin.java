package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeletonEntity.class)
public abstract class BowPotionMixin {

    @Inject(method = "createArrowProjectile", at = @At("RETURN"), cancellable = true)
    private void universalmobwar$onShoot(ItemStack arrow, float damageModifier, CallbackInfoReturnable<PersistentProjectileEntity> cir) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity)(Object)this;
        if (skeleton.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(skeleton);
        if (profile == null) return;
        
        int chance = profile.specialSkills.getOrDefault("bow_potion_chance", 0);
        if (chance <= 0) return;
        
        if (skeleton.getRandom().nextInt(100) < chance) {
            // Create Tipped Arrow
            ArrowEntity tippedArrow = new ArrowEntity(skeleton.getWorld(), skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            // Note: Constructor might vary. In 1.21 it's (World, LivingEntity, ItemStack, ItemStack weapon) or similar.
            // Actually, AbstractSkeletonEntity usually creates PersistentProjectileEntity.
            // We want to replace it with a Tipped Arrow (ArrowEntity) with effects.
            
            // Pick effect
            // slowness, weakness, poison, harming, decay
            RegistryEntry<Potion> potion = Potions.POISON;
            int pick = skeleton.getRandom().nextInt(5);
            switch (pick) {
                case 0 -> potion = Potions.SLOWNESS;
                case 1 -> potion = Potions.WEAKNESS;
                case 2 -> potion = Potions.POISON;
                case 3 -> potion = Potions.HARMING;
                case 4 -> potion = Potions.STRONG_POISON; // Decay/Wither not standard for arrows usually, use Strong Poison or Wither effect
            }
            
            tippedArrow.setPotion(potion);
            if (pick == 4) {
                // Add Wither effect manually if needed, but Strong Poison is fine for now as placeholder for Decay
                // Or use custom effect
                tippedArrow.addEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WITHER, 200, 1));
            }
            
            cir.setReturnValue(tippedArrow);
        }
    }
}
