package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.command.RaidBossSpawnCommand;
import mod.universalmobwar.entity.MobWarlordEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the Mob Warlord boss into the final raid wave.
 * Very rare spawn chance (1-2%) for ultimate challenge.
 */
@Mixin(Raid.class)
public abstract class RaidSpawningMixin {
    
    @Shadow
    private int wavesSpawned;
    
    @Shadow
    private ServerWorld world;
    
    @Shadow
    private BlockPos center;
    
    /**
     * Injects boss spawning logic into the raid wave spawning.
     * Only spawns on final wave (wave 6+) with 1.5% chance.
     * Can be forced with /mobwar raid forceboss command.
     */
    @Inject(method = "spawnNextWave", at = @At("TAIL"))
    private void universalmobwar$spawnWarlordBoss(BlockPos pos, CallbackInfo ci) {
        // Only spawn on final waves (6+, which is typically the last wave)
        if (this.wavesSpawned < 6) return;
        
        // Check if force spawn is active (from command)
        boolean forceSpawn = RaidBossSpawnCommand.shouldForceSpawn();
        
        // Very rare spawn chance - 1.5% (1 in 67 raids) OR forced by command
        if (!forceSpawn && Math.random() > 0.015) return;
        
        try {
            // Find suitable spawn position near raid center
            BlockPos spawnPos = findSuitableSpawnPos(this.center);
            if (spawnPos == null) return;
            
            // Create and spawn the Mob Warlord
            MobWarlordEntity warlord = new MobWarlordEntity(UniversalMobWarMod.MOB_WARLORD, this.world);
            warlord.refreshPositionAndAngles(
                spawnPos.getX() + 0.5, 
                spawnPos.getY(), 
                spawnPos.getZ() + 0.5, 
                0.0f, 
                0.0f
            );
            warlord.initialize(this.world, this.world.getLocalDifficulty(spawnPos), SpawnReason.EVENT, null);
            
            // Mark as raid boss - this changes targeting behavior
            warlord.setRaidBoss(true);
            warlord.setPersistent();
            
            boolean spawned = this.world.spawnEntity(warlord);
            
            if (spawned) {
                // Broadcast to all players
                this.world.getPlayers().forEach(player -> {
                    player.sendMessage(
                        net.minecraft.text.Text.literal("")
                            .append(net.minecraft.text.Text.literal("═══════════════════════════════════").styled(style -> style.withColor(net.minecraft.util.Formatting.DARK_RED).withBold(true)))
                            .append(net.minecraft.text.Text.literal("\n"))
                            .append(net.minecraft.text.Text.literal("    💀 THE MOB WARLORD HAS JOINED THE RAID! 💀").styled(style -> style.withColor(net.minecraft.util.Formatting.RED).withBold(true)))
                            .append(net.minecraft.text.Text.literal("\n"))
                            .append(net.minecraft.text.Text.literal("═══════════════════════════════════").styled(style -> style.withColor(net.minecraft.util.Formatting.DARK_RED).withBold(true))),
                        false
                    );
                    
                    // Play dramatic sound (server-side)
                    player.playSoundToPlayer(
                        net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN,
                        net.minecraft.sound.SoundCategory.HOSTILE,
                        1.0f,
                        0.5f
                    );
                });
                
                UniversalMobWarMod.LOGGER.info("Mob Warlord spawned in raid at {}", spawnPos);
            }
        } catch (Exception e) {
            UniversalMobWarMod.LOGGER.error("Failed to spawn Mob Warlord in raid", e);
        }
    }
    
    /**
     * Finds a suitable spawn position near the raid center.
     */
    private BlockPos findSuitableSpawnPos(BlockPos center) {
        // Try to find a valid spawn position within 32 blocks of center
        for (int attempt = 0; attempt < 10; attempt++) {
            int offsetX = (int)((Math.random() - 0.5) * 64); // ±32 blocks
            int offsetZ = (int)((Math.random() - 0.5) * 64);
            
            BlockPos testPos = center.add(offsetX, 0, offsetZ);
            
            // Find ground level
            BlockPos groundPos = this.world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, testPos);
            
            // Check if position is valid (solid ground, enough space above)
            if (this.world.getBlockState(groundPos.down()).isSolidBlock(this.world, groundPos.down())) {
                if (this.world.isAir(groundPos) && this.world.isAir(groundPos.up()) && this.world.isAir(groundPos.up(2))) {
                    return groundPos;
                }
            }
        }
        
        // Fallback to center if no valid position found
        return this.world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, center);
    }
}

