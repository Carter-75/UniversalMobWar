package mod.universalmobwar.mixin;

/**
 * Bridge interface implemented by projectile mixins so we can mark
 * projectiles that already processed Universal Mob War ability logic.
 */
public interface ProjectileAbilityBridge {
    void universalmobwar$setAbilitiesApplied(boolean applied);
}
