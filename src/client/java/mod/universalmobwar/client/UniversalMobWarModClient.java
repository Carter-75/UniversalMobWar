package mod.universalmobwar.client;

import mod.universalmobwar.UniversalMobWarMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Client-side initialization for Universal Mob War.
 * Registers the custom witch renderer for the Mob Warlord.
 */
public class UniversalMobWarModClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // Register custom witch renderer for the boss
        EntityRendererRegistry.register(UniversalMobWarMod.MOB_WARLORD, MobWarlordRenderer::new);
        
        UniversalMobWarMod.LOGGER.info("Universal Mob War client initialized - Witch renderer registered!");
    }
}

