package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HordeSummonMixin {

    @Inject(method = "damage", at = @At("RETURN"))
    private void universalmobwar$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return; // Only if damage was successful
        
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.getWorld().isClient()) return;
        if (!(self instanceof MobEntity mob)) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("horde_summon", 0);
        if (level <= 0) return;
        
        // Prevent infinite loops if summoned mobs also have this skill
        // (They will, but we can limit recursion or rely on probability)
        // Actually, "extra dont summon the same 1-3 extra again and again"
        // This implies summoned mobs should NOT have this skill active?
        // Or we just rely on the fact that they need to be hit to summon.
        
        int count = 0;
        float r = mob.getRandom().nextFloat();
        
        float chance = 0.0f;
        if (level == 1) chance = 0.10f;
        else if (level == 2) chance = 0.20f;
        else if (level == 3) chance = 0.30f;
        else if (level == 4) chance = 0.40f;
        else if (level >= 5) chance = 0.50f;
        
        if (r < chance) {
            count = 1;
        }
        

        
        if (count > 0 && self.getWorld() instanceof ServerWorld serverWorld) {
            EntityType<?> type = mob.getType();
            for (int i = 0; i < count; i++) {
                spawnReinforcement(serverWorld, mob, type);
            }
        }
    }
    
    private void spawnReinforcement(ServerWorld world, MobEntity original, EntityType<?> type) {
        for (int i = 0; i < 5; i++) { // Try 5 times to find a spot
            double x = original.getX() + (original.getRandom().nextDouble() - 0.5) * 7.0;
            double y = original.getY() + original.getRandom().nextInt(3) - 1;
            double z = original.getZ() + (original.getRandom().nextDouble() - 0.5) * 7.0;
            
            BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                Entity entity = type.create(world);
                if (entity instanceof MobEntity reinforcement) {
                    reinforcement.refreshPositionAndAngles(x, y, z, original.getRandom().nextFloat() * 360.0F, 0.0F);
                    reinforcement.initialize(world, world.getLocalDifficulty(pos), SpawnReason.REINFORCEMENT, (EntityData)null);
                    
                    // Important: The user said "extra dont summon the same 1-3 extra again and again"
                    // We should probably disable the skill on the summoned mob.
                    // We can do this by modifying its profile after spawn.
                    // But profile is created on spawn.
                    // We can set a tag or something.
                    // Or we can just let them have it (it's a "horde" after all).
                    // "while the extra dont summon the same 1-3 extra again and again"
                    // This strongly suggests recursion prevention.
                    // I'll set the level to 0 in their profile.
                    
                    // Add persistent tag BEFORE spawning to prevent skill acquisition
                    reinforcement.addCommandTag("umw_horde_reinforcement");
                    reinforcement.addCommandTag("umw_summoned");
                    
                    world.spawnEntity(reinforcement);
                    
                    // Force profile creation/update
                    mod.universalmobwar.system.GlobalMobScalingSystem.onMobActivated(reinforcement, world);
                    
                    PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(reinforcement);
                    if (profile != null) {
                        profile.specialSkills.put("horde_summon", 0);
                        // Save back to NBT to persist the change
                        mod.universalmobwar.data.MobWarData data = mod.universalmobwar.data.MobWarData.get(reinforcement);
                        data.setSkillData(profile.writeNbt());
                        mod.universalmobwar.data.MobWarData.save(reinforcement, data);
                    }
                    
                    // Target the same attacker
                    if (original.getAttacker() != null) {
                        reinforcement.setTarget(original.getAttacker());
                    }
                    return;
                }
            }
        }
    }
}
