package mod.universalmobwar.client;

import mod.universalmobwar.UniversalMobWarMod;
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
        UmwClientEnchantCompat.init();

        // Register custom witch renderer for the Mob Warlord
        EntityRendererRegistry.register(UniversalMobWarMod.MOB_WARLORD, MobWarlordRenderer::new);
        
        // Register world render callback for visual features
        WorldRenderEvents.LAST.register(new MobVisualRenderer());
        
        UniversalMobWarMod.LOGGER.info("Universal Mob War client initialized - Custom witch renderer and visual features registered!");
    }
}

