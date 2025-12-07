package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class ProjectileSkillMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void universalmobwar$onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof MobEntity mob) {
                PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
                if (profile == null) return;

                // Piercing
                if (projectile instanceof PersistentProjectileEntity persistent) {
                    int piercing = profile.specialSkills.getOrDefault("piercing_shot", 0);
                    if (piercing > 0) {
                        ((PersistentProjectileEntityAccessor) persistent).invokeSetPierceLevel((byte) piercing);
                    }
                    
                    // Tipped Arrows (Bow Potion Mastery)
                    int potionMastery = profile.specialSkills.getOrDefault("bow_potion_mastery", 0);
                    if (potionMastery > 0 && persistent instanceof net.minecraft.entity.projectile.ArrowEntity arrow) {
                         // Chance: "normal curve" 0% -> 100%
                        double p = 1.0 / (1.0 + Math.exp(-1.0 * (potionMastery - 5.0)));
                        
                        if (mob.getRandom().nextDouble() < p) {
                            double center = (potionMastery / 10.0) * 4.0; // 0 to 4
                            int pick = (int) Math.round(center + mob.getRandom().nextGaussian() * 1.0);
                            if (pick < 0) pick = 0;
                            if (pick > 4) pick = 4;
                            
                            net.minecraft.entity.effect.StatusEffectInstance effect = switch (pick) {
                                case 0 -> new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 200, 0);
                                case 1 -> new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WEAKNESS, 200, 0);
                                case 2 -> new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.POISON, 200, 0);
                                case 3 -> new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.INSTANT_DAMAGE, 1, 0);
                                case 4 -> new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WITHER, 200, 0);
                                default -> null;
                            };
                            
                            if (effect != null) {
                                arrow.addEffect(effect);
                                // arrow.setColor(effect.getEffectType().value().getColor()); // Let vanilla handle color
                            }
                        }
                    }
                }

                // Multishot (Only trigger if this is the "main" projectile to avoid recursion)
                // We can check if the projectile has a tag, or just use a thread-local flag?
                // Or we can check if it's "just created".
                // A safer way is to check if we have already processed this projectile.
                // But we can't easily attach data to it yet.
                // Let's assume the "main" projectile is the one spawned by the mob's AI.
                // If we spawn extras, we should mark them.
                // But we can't mark them *before* spawning easily without ATs or casting.
                // Actually, we can just check if the projectile is *already* in the world? No, this is spawn.
                
                // Let's use a custom tag in the entity's NBT or a transient field if possible.
                // Since we can't add fields easily, let's use a Set in this class to track "processed" entities?
                // No, that leaks memory.
                
                // Alternative: Multishot usually happens at the *shoot* call site.
                // But we are doing this globally.
                // Let's try to spawn extras, but ensure extras don't trigger this.
                // We can set a custom tag on the extras.
                
                int multishot = profile.specialSkills.getOrDefault("multishot_skill", 0);
                if (multishot > 0 && !entity.getCommandTags().contains("umw_multishot_extra") && !entity.getCommandTags().contains("umw_processed")) {
                    entity.addCommandTag("umw_processed"); // Mark main as processed
                    
                    ServerWorld world = (ServerWorld)(Object)this;
                    for (int i = 0; i < multishot; i++) {
                        // Create a copy?
                        // We can't easily "copy" an entity.
                        // We have to create a new one of the same type.
                        Entity extra = entity.getType().create(world);
                        if (extra instanceof ProjectileEntity extraProj) {
                            extraProj.setOwner(owner);
                            extraProj.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
                            
                            // Spread
                            float spread = 10.0f * (i + 1);
                            if (i % 2 == 1) spread = -spread;
                            
                            extraProj.setYaw(entity.getYaw() + spread);
                            extraProj.setVelocity(entity.getVelocity().rotateY((float)Math.toRadians(spread)));
                            
                            if (extraProj instanceof PersistentProjectileEntity extraPers && projectile instanceof PersistentProjectileEntity mainPers) {
                                if (extraPers instanceof PersistentProjectileEntityAccessor extraAcc && mainPers instanceof PersistentProjectileEntityAccessor mainAcc) {
                                     extraAcc.invokeSetPierceLevel(mainAcc.invokeGetPierceLevel());
                                } else {
                                     // Fallback if mixin fails or casting fails (shouldn't happen if mixin applied)
                                     // But we can't cast to interface on the object directly unless we cast the object.
                                     ((PersistentProjectileEntityAccessor)extraPers).invokeSetPierceLevel(((PersistentProjectileEntityAccessor)mainPers).invokeGetPierceLevel());
                                }
                                // Copy damage?
                                extraPers.setDamage(mainPers.getDamage());
                            }
                            
                            extra.addCommandTag("umw_multishot_extra");
                            world.spawnEntity(extra);
                        }
                    }
                }
            }
        }
    }
}
