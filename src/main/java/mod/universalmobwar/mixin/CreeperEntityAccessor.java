package mod.universalmobwar.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accesses Creeper explosion radius so scaling abilities can adjust it on demand.
 */
@Mixin(CreeperEntity.class)
public interface CreeperEntityAccessor {
    @Accessor("explosionRadius")
    int universalmobwar$getExplosionRadius();

    @Accessor("explosionRadius")
    void universalmobwar$setExplosionRadius(int radius);
}
