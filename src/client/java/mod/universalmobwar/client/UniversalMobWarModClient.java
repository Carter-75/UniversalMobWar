package mod.universalmobwar.client;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.net.UmwRequiredClientPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Client-side initialization for Universal Mob War.
 * Registers the custom witch renderer for the Mob Warlord boss.
 */
public class UniversalMobWarModClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // Register the payload type so this client advertises "I have UniversalMobWar" to servers.
        try {
            PayloadTypeRegistry.playS2C().register(UmwRequiredClientPayload.ID, UmwRequiredClientPayload.CODEC);
        } catch (IllegalArgumentException alreadyRegistered) {
            // ok
        }

        UmwClientEnchantCompat.init();

        // Register custom witch renderer for the Mob Warlord
        EntityRendererRegistry.register(UniversalMobWarMod.MOB_WARLORD, MobWarlordRenderer::new);
        
        // Register world render callback for visual features
        WorldRenderEvents.LAST.register(new MobVisualRenderer());
        
        UniversalMobWarMod.LOGGER.info("Universal Mob War client initialized - Custom witch renderer and visual features registered!");
    }
}

