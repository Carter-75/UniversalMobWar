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

import java.util.List;
import java.util.Optional;

@Mixin(AbstractSkeletonEntity.class)
public abstract class BowPotionMixin {

    @Inject(method = "createArrowProjectile", at = @At("RETURN"), cancellable = true)
    private void universalmobwar$onShoot(ItemStack arrow, float damageModifier, ItemStack shotFrom, CallbackInfoReturnable<PersistentProjectileEntity> cir) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity)(Object)this;
        if (skeleton.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(skeleton);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("bow_potion_mastery", 0);
        if (level <= 0 || level > 5) return; // Max 5 levels
        
        // Progressive chance: L1=20%, L2=40%, L3=60%, L4=80%, L5=100%
        int chance = level * 20;
        if (skeleton.getRandom().nextInt(100) >= chance) return;
        
        // Progressive potion effects based on level
        // L1: Slowness I (10s)
        // L2: Slowness II (15s) or Weakness I (10s)
        // L3: Poison I (10s) or Weakness I (15s)
        // L4: Poison II (15s) or Instant Damage I
        // L5: Poison II (20s) or Instant Damage II or Wither I (10s)
        
        ItemStack tippedStack = new ItemStack(Items.TIPPED_ARROW);
        
        if (level == 1) {
            tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                Optional.of(Potions.SLOWNESS), Optional.empty(), List.of(
                    new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 200, 0)
                )));
        } else if (level == 2) {
            if (skeleton.getRandom().nextBoolean()) {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.LONG_SLOWNESS), Optional.empty(), List.of(
                        new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 300, 1)
                    )));
            } else {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.WEAKNESS), Optional.empty(), List.of()));
            }
        } else if (level == 3) {
            if (skeleton.getRandom().nextBoolean()) {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.POISON), Optional.empty(), List.of()));
            } else {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.LONG_WEAKNESS), Optional.empty(), List.of()));
            }
        } else if (level == 4) {
            if (skeleton.getRandom().nextBoolean()) {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.STRONG_POISON), Optional.empty(), List.of()));
            } else {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.HARMING), Optional.empty(), List.of()));
            }
        } else { // level 5
            int pick = skeleton.getRandom().nextInt(3);
            if (pick == 0) {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.LONG_POISON), Optional.empty(), List.of(
                    new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.POISON, 400, 1)
                )));
            } else if (pick == 1) {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(Potions.STRONG_HARMING), Optional.empty(), List.of()));
            } else {
                tippedStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.POISON), Optional.empty(), List.of(
                        new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WITHER, 200, 0)
                    )));
            }
        }

        // Create Tipped Arrow with the stack
        ArrowEntity tippedArrow = new ArrowEntity(skeleton.getWorld(), skeleton, tippedStack, shotFrom != null ? shotFrom : new ItemStack(Items.BOW));
        
        cir.setReturnValue(tippedArrow);
    }
}
