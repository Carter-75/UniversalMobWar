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
        // Register payload type (can be invoked in both client+server runtimes; tolerate double init)
        try {
            PayloadTypeRegistry.playC2S().register(UmwClientEnchantmentsPayload.ID, UmwClientEnchantmentsPayload.CODEC);
        } catch (IllegalArgumentException alreadyRegistered) {
            UniversalMobWarMod.LOGGER.debug(
                "[EnchantCompat] Payload type already registered: {}",
                UmwClientEnchantmentsPayload.ID.id()
            );
        }

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
            try {
                ServerPlayerEntity player = handler.player;
                if (player == null) {
                    UniversalMobWarMod.LOGGER.warn("[EnchantCompat] JOIN fired with null player; skipping");
                    recomputeAllowlist(null);
                    return;
                }

                UUID uuid = player.getUuid();
                if (uuid == null) {
                    UniversalMobWarMod.LOGGER.warn("[EnchantCompat] Player UUID is null for {}; skipping", player.getName().getString());
                    recomputeAllowlist(player);
                    return;
                }

                // Until we receive the client's registry list, assume vanilla-only to prevent join-time crashes.
                // ConcurrentHashMap does not permit null values, so use an empty-set sentinel.
                CLIENT_ENCHANTMENTS.put(uuid, Collections.emptySet());
                recomputeAllowlist(player);
            } catch (Throwable t) {
                // Never fail the login pipeline due to an optional compat layer.
                UniversalMobWarMod.LOGGER.error("[EnchantCompat] Exception during JOIN handler; continuing login", t);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SERVER = server;
            try {
                ServerPlayerEntity player = handler.player;
                if (player == null) {
                    recomputeAllowlist(null);
                    return;
                }

                UUID uuid = player.getUuid();
                if (uuid != null) {
                    CLIENT_ENCHANTMENTS.remove(uuid);
                }
                recomputeAllowlist(player);
            } catch (Throwable t) {
                UniversalMobWarMod.LOGGER.error("[EnchantCompat] Exception during DISCONNECT handler", t);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(UmwClientEnchantmentsPayload.ID, (payload, context) -> {
            try {
                ServerPlayerEntity player = context.player();
                if (player == null) {
                    return;
                }

                UUID uuid = player.getUuid();
                if (uuid == null) {
                    return;
                }

                Set<Identifier> ids = new HashSet<>(payload.enchantmentIds());
                ids.removeIf(id -> id == null);
                CLIENT_ENCHANTMENTS.put(uuid, Collections.unmodifiableSet(ids));
                recomputeAllowlist(player);
            } catch (Throwable t) {
                UniversalMobWarMod.LOGGER.error("[EnchantCompat] Exception handling client enchantments payload", t);
            }
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
            // Empty-set is our "not reported yet" sentinel; treat it as vanilla-only.
            if (clientIds == null || clientIds.isEmpty()) {
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
