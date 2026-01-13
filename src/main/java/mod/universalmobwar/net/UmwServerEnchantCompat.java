package mod.universalmobwar.net;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.system.ScalingSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UmwServerEnchantCompat {

    private static final ConcurrentHashMap<UUID, Set<Identifier>> CLIENT_ENCHANTMENTS = new ConcurrentHashMap<>();
    private static volatile MinecraftServer SERVER;

    private UmwServerEnchantCompat() {
    }

    public static void init() {
        // Register payload type
        PayloadTypeRegistry.playC2S().register(UmwClientEnchantmentsPayload.ID, UmwClientEnchantmentsPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
            // Default allowlist when no players are connected.
            recomputeAllowlist(null);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SERVER = null;
            CLIENT_ENCHANTMENTS.clear();
            ScalingSystem.setEnchantmentNetworkAllowlist(Collections.emptySet());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SERVER = server;
            // Until we receive the client's registry list, assume vanilla-only to prevent join-time crashes.
            CLIENT_ENCHANTMENTS.put(handler.player.getUuid(), null);
            recomputeAllowlist(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SERVER = server;
            CLIENT_ENCHANTMENTS.remove(handler.player.getUuid());
            recomputeAllowlist(handler.player);
        });

        ServerPlayNetworking.registerGlobalReceiver(UmwClientEnchantmentsPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            Set<Identifier> ids = new HashSet<>(payload.enchantmentIds());
            CLIENT_ENCHANTMENTS.put(player.getUuid(), Collections.unmodifiableSet(ids));
            recomputeAllowlist(player);
        });
    }

    private static void recomputeAllowlist(ServerPlayerEntity debugPlayer) {
        ModConfig config = ModConfig.getInstance();

        MinecraftServer server = debugPlayer != null ? debugPlayer.getServer() : SERVER;
        if (server == null) {
            ScalingSystem.setEnchantmentNetworkAllowlist(Collections.emptySet());
            return;
        }

        // If modded enchants are disabled, restrict to minecraft namespace via ScalingSystem.
        // (We still publish a superset allowlist so the filtering is centralized.)
        Registry<Enchantment> enchantRegistry = server.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        Set<Identifier> serverIds = new HashSet<>(enchantRegistry.getIds());

        // Intersect with each client's reported ids. If client hasn't reported yet (null), treat as vanilla-only.
        Set<Identifier> intersection = new HashSet<>(serverIds);
        for (Set<Identifier> clientIds : CLIENT_ENCHANTMENTS.values()) {
            if (clientIds == null) {
                // vanilla-only: keep only minecraft namespace
                intersection.removeIf(id -> id == null || !"minecraft".equals(id.getNamespace()));
                continue;
            }
            intersection.retainAll(clientIds);
        }

        ScalingSystem.setEnchantmentNetworkAllowlist(Collections.unmodifiableSet(intersection));

        if ((config.debugLogging || config.debugUpgradeLog) && debugPlayer != null) {
            UniversalMobWarMod.LOGGER.info(
                "[EnchantCompat] allowModdedEnchantments={}, clients={}, allowlistSize={} (triggered by {})",
                config.allowModdedEnchantments,
                CLIENT_ENCHANTMENTS.size(),
                intersection.size(),
                debugPlayer.getName().getString()
            );
        }
    }
}
