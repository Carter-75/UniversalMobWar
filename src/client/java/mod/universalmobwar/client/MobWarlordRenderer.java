package mod.universalmobwar.client;

import mod.universalmobwar.entity.MobWarlordEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.WitchEntityModel;
import net.minecraft.util.Identifier;

/**
 * Custom renderer for the Mob Warlord that uses the witch model.
 * This avoids the Iris Shaders crash by not extending WitchEntity directly.
 */
public class MobWarlordRenderer extends MobEntityRenderer<MobWarlordEntity, WitchEntityModel<MobWarlordEntity>> {
    
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/witch.png");
    
    public MobWarlordRenderer(EntityRendererFactory.Context context) {
        super(context, new WitchEntityModel<>(context.getPart(EntityModelLayers.WITCH)), 0.5f);
    }
    
    @Override
    public Identifier getTexture(MobWarlordEntity entity) {
        return TEXTURE;
    }
    
    /**
     * Override to scale the boss to 2x size.
     */
    @Override
    protected float getAnimationProgress(MobWarlordEntity entity, float tickDelta) {
        return entity.age + tickDelta;
    }
}

