package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
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
        
        int level = profile.specialSkills.getOrDefault("bow_potion_mastery", 0);
        if (level <= 0) return;
        
        // Chance: "normal curve" 0% -> 100%
        // Using sigmoid-like curve based on level 1-10
        // Level 1: ~1%, Level 5: ~50%, Level 10: ~99%
        double p = 1.0 / (1.0 + Math.exp(-1.0 * (level - 5.0)));
        
        if (skeleton.getRandom().nextDouble() < p) {
            // Pick effect
            RegistryEntry<Potion> potion = Potions.POISON;
            int pick = skeleton.getRandom().nextInt(5);
            switch (pick) {
                case 0 -> potion = Potions.SLOWNESS;
                case 1 -> potion = Potions.WEAKNESS;
                case 2 -> potion = Potions.POISON;
                case 3 -> potion = Potions.HARMING;
                case 4 -> potion = Potions.STRONG_POISON; // Decay/Wither not always available as Potion, using Strong Poison as fallback or Wither if possible
            }
            // Try to find Decay/Wither if possible, but standard Potions class might not expose it directly as a field if it's not a brewing recipe.
            // However, we can try to use the registry if we really want "decay".
            // For now, keeping Strong Poison as it's close (damage).
            
            ItemStack tippedStack = new ItemStack(Items.TIPPED_ARROW);
            tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));

            // Create Tipped Arrow with the stack
            ArrowEntity tippedArrow = new ArrowEntity(skeleton.getWorld(), skeleton, tippedStack, new ItemStack(Items.BOW));
            
            cir.setReturnValue(tippedArrow);
        }
    }
}
