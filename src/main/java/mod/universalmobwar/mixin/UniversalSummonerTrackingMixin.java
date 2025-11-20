package mod.universalmobwar.mixin;

import mod.universalmobwar.util.SummonerTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Automatically tracks summoner-summoned relationships for ALL mobs.
 * Works for vanilla and modded mobs that use SpawnReason.MOB_SUMMONED.
 */
@Mixin(MobEntity.class)
public abstract class UniversalSummonerTrackingMixin {
    
    /**
     * Intercepts mob initialization to detect and track summoned mobs.
     * This is called when any mob spawns, including:
     * - Evoker summoning Vexes
     * - Illusioner summoning duplicates
     * - Any modded mob summoning others
     */
    @Inject(method = "initialize", at = @At("RETURN"))
    private void universalmobwar$trackSummonedMob(
        ServerWorldAccess world, 
        LocalDifficulty difficulty, 
        SpawnReason spawnReason, 
        EntityData entityData,
        CallbackInfoReturnable<EntityData> cir
    ) {
        MobEntity self = (MobEntity)(Object)this;
        
        // Only track if this mob was summoned AND we're in a ServerWorld
        if ((spawnReason == SpawnReason.MOB_SUMMONED || spawnReason == SpawnReason.EVENT) 
            && world instanceof ServerWorld serverWorld) {
            // Try to find the summoner by looking for nearby mobs that could have summoned this one
            // Common summoners: Evoker, Illusioner, Warlord, etc.
            
            // Look for potential summoners within 16 blocks
            MobEntity closestSummoner = serverWorld.getEntitiesByClass(
                MobEntity.class,
                self.getBoundingBox().expand(16.0),
                entity -> entity != self && 
                         entity.isAlive() &&
                         entity.squaredDistanceTo(self) < 256.0 && // Within 16 blocks
                         couldSummon(entity)
            ).stream()
                .min((a, b) -> Double.compare(a.squaredDistanceTo(self), b.squaredDistanceTo(self)))
                .orElse(null);
            
            if (closestSummoner != null) {
                SummonerTracker.registerSummoned(self.getUuid(), closestSummoner.getUuid());
            }
        }
    }
    
    /**
     * Checks if a mob could potentially summon other mobs.
     * This includes known vanilla summoners and is permissive for modded mobs.
     */
    private static boolean couldSummon(Entity entity) {
        String entityType = entity.getType().toString();
        
        // Known vanilla summoners
        if (entityType.contains("evoker") || 
            entityType.contains("illusioner") ||
            entityType.contains("warlord") ||
            entityType.contains("witch") ||
            entityType.contains("necromancer") || // Modded
            entityType.contains("summoner")) {    // Modded
            return true;
        }
        
        // For safety, assume any hostile mob with high max health could be a summoner
        // This catches modded bosses
        if (entity instanceof MobEntity mob) {
            return mob.getMaxHealth() > 100.0f; // Likely a boss or special mob
        }
        
        return false;
    }
}

