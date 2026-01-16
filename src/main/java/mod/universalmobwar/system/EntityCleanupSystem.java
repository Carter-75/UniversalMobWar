package mod.universalmobwar.system;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.mixin.PersistentProjectileEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Periodic cleanup of high-volume entities that can accumulate and cause lag.
 *
 * Currently targets non-player-owned ground projectiles (e.g., arrows/tridents)
 * that have come to rest.
 */
public final class EntityCleanupSystem {
	private EntityCleanupSystem() {
	}

	public static int cleanupNonPlayerGroundProjectiles(MinecraftServer server) {
		ModConfig config = ModConfig.getInstance();
		if (!config.modEnabled || !config.cleanupNonPlayerGroundProjectiles) {
			return 0;
		}

		final int minAgeTicks = Math.max(0, config.cleanupNonPlayerGroundProjectilesMinAgeTicks);
		final int maxPerWorld = Math.max(0, config.cleanupNonPlayerGroundProjectilesMaxPerWorldPerRun);

		int removedTotal = 0;
		for (ServerWorld world : server.getWorlds()) {
			int removedThisWorld = 0;
			for (Entity entity : world.iterateEntities()) {
				if (maxPerWorld > 0 && removedThisWorld >= maxPerWorld) {
					break;
				}

				if (!(entity instanceof PersistentProjectileEntity projectile)) {
					continue;
				}

				// Keep any player-shot/thrown projectiles.
				Entity owner = projectile.getOwner();
				if (owner instanceof PlayerEntity) {
					continue;
				}

				if (projectile.age < minAgeTicks) {
					continue;
				}

				// Only delete when the projectile is resting (in-ground). This avoids impacting combat.
				boolean inGround;
				try {
					inGround = ((PersistentProjectileEntityAccessor) projectile).universalmobwar$isInGround();
				} catch (Throwable t) {
					// If mappings ever change and the accessor fails, fall back to a conservative check.
					inGround = projectile.isOnGround();
				}

				if (!inGround) {
					continue;
				}

				projectile.discard();
				removedThisWorld++;
			}

			removedTotal += removedThisWorld;
		}

		if (removedTotal > 0 && ModConfig.getInstance().debugLogging) {
			UniversalMobWarMod.LOGGER.info("Entity cleanup removed {} non-player ground projectiles", removedTotal);
		}

		return removedTotal;
	}
}
