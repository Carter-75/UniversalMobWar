package mod.universalmobwar.client;

import mod.universalmobwar.entity.MobWarlordEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.WitchEntityModel;
import net.minecraft.util.Identifier;

/**
 * Custom renderer for the Mob Warlord that uses the witch model.
 * This renders the boss as a giant witch without the particle crashes.
 */
public class MobWarlordRenderer extends MobEntityRenderer<MobWarlordEntity, WitchEntityModel<MobWarlordEntity>> {
    
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/witch.png");
    
    public MobWarlordRenderer(EntityRendererFactory.Context context) {
        super(context, new WitchEntityModel<>(context.getPart(EntityModelLayers.WITCH)), 0.5f * 2.0f); // Shadow radius 2x
    }
    
    @Override
    public Identifier getTexture(MobWarlordEntity entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(MobWarlordEntity entity, net.minecraft.client.util.math.MatrixStack matrices, float amount) {
        // Scale the model to 2x size
        matrices.scale(2.0f, 2.0f, 2.0f);
        super.scale(entity, matrices, amount);
    }
}

