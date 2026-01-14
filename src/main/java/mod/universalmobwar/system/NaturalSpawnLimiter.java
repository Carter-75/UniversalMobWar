package mod.universalmobwar.system;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class NaturalSpawnLimiter {

    private static final ConcurrentHashMap<World, AtomicInteger> NATURAL_MOB_COUNTS = new ConcurrentHashMap<>();

    private NaturalSpawnLimiter() {
    }

    public static void onMobLoaded(ServerWorld world, boolean isNaturalSpawnedMob) {
        if (!isNaturalSpawnedMob) {
            return;
        }
        NATURAL_MOB_COUNTS.computeIfAbsent(world, w -> new AtomicInteger()).incrementAndGet();
    }

    public static void onMobUnloaded(ServerWorld world, boolean isNaturalSpawnedMob) {
        if (!isNaturalSpawnedMob) {
            return;
        }
        AtomicInteger counter = NATURAL_MOB_COUNTS.get(world);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    public static int getNaturalMobCount(ServerWorld world) {
        AtomicInteger counter = NATURAL_MOB_COUNTS.get(world);
        return counter != null ? Math.max(0, counter.get()) : 0;
    }
}
