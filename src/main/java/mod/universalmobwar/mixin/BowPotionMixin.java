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
        
        int chance = profile.specialSkills.getOrDefault("bow_potion_chance", 0);
        if (chance <= 0) return;
        
        if (skeleton.getRandom().nextInt(100) < chance) {
            // Pick effect
            RegistryEntry<Potion> potion = Potions.POISON;
            int pick = skeleton.getRandom().nextInt(5);
            switch (pick) {
                case 0 -> potion = Potions.SLOWNESS;
                case 1 -> potion = Potions.WEAKNESS;
                case 2 -> potion = Potions.POISON;
                case 3 -> potion = Potions.HARMING;
                case 4 -> potion = Potions.STRONG_POISON; 
            }
            
            ItemStack tippedStack = new ItemStack(Items.TIPPED_ARROW);
            if (pick == 4) {
                 // Custom Wither effect if needed, or just Strong Poison
                 // Let's just use Strong Poison for simplicity as per switch
                 tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));
            } else {
                 tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potion));
            }

            // Create Tipped Arrow with the stack
            ArrowEntity tippedArrow = new ArrowEntity(skeleton.getWorld(), skeleton, tippedStack, new ItemStack(Items.BOW));
            
            cir.setReturnValue(tippedArrow);
        }
    }
}
