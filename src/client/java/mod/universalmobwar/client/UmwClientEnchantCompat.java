package mod.universalmobwar.client;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.net.UmwClientEnchantmentsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class UmwClientEnchantCompat {

    private UmwClientEnchantCompat() {
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                Registry<Enchantment> enchantRegistry = handler.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                List<Identifier> ids = new ArrayList<>(enchantRegistry.getIds());
                ClientPlayNetworking.send(new UmwClientEnchantmentsPayload(ids));
            } catch (Exception e) {
                UniversalMobWarMod.LOGGER.warn("[EnchantCompat] Failed to send client enchantment list", e);
            }
        });
    }
}
