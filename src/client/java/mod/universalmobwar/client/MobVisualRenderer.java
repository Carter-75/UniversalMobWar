package mod.universalmobwar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Handles rendering of visual features: target lines, health bars, mob labels.
 * Updated for Minecraft 1.21.1 rendering API
 */
public class MobVisualRenderer implements WorldRenderEvents.Last {

    private static final ModConfig CONFIG = ModConfig.getInstance();

    @Override
    public void onLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Performance check: Skip if FPS is too low
        if (client.getCurrentFps() < CONFIG.minFpsForVisuals) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();

        // Get all entities in render distance
        Iterable<Entity> entities = client.world.getEntities();

        for (Entity entity : entities) {
            if (!(entity instanceof MobEntity mob)) continue;

            // Check distance for performance
            double distance = entity.getPos().distanceTo(cameraPos);
            if (distance > 64.0) continue; // Only render within 64 blocks

            renderForMob(matrices, context, camera, mob, distance);
        }
    }

    private void renderForMob(MatrixStack matrices, WorldRenderContext context, Camera camera, MobEntity mob, double distance) {
        Vec3d mobPos = mob.getPos();
        Vec3d cameraPos = camera.getPos();

        // Target lines
        if (CONFIG.showTargetLines) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                drawTargetLine(matrices, context, camera, mobPos, target.getPos());
            }
        }

        // Health bars and labels (only if close enough)
        if (distance < 32.0) {
            matrices.push();
            matrices.translate(mobPos.x - cameraPos.x, mobPos.y - cameraPos.y + mob.getHeight() + 0.5, mobPos.z - cameraPos.z);
            
            // Correct billboard rotation for 1.21.1
            Quaternionf rotation = camera.getRotation();
            matrices.multiply(rotation);
            matrices.scale(-1.0f, -1.0f, 1.0f);

            // Health bar
            if (CONFIG.showHealthBars) {
                drawHealthBar(matrices, context, mob);
            }

            // Mob labels
            if (CONFIG.showMobLabels) {
                drawMobLabel(matrices, context, mob);
            }

            matrices.pop();
        }
    }

    private void drawTargetLine(MatrixStack matrices, WorldRenderContext context, Camera camera, Vec3d start, Vec3d end) {
        matrices.push();
        Vec3d cameraPos = camera.getPos();

        // Use LINES render layer (correct for 1.21.1)
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Line from mob to target (red)
        buffer.vertex(matrix, (float)(start.x - cameraPos.x), (float)(start.y - cameraPos.y + 1.0), (float)(start.z - cameraPos.z))
                .color(255, 0, 0, 255);
        buffer.vertex(matrix, (float)(end.x - cameraPos.x), (float)(end.y - cameraPos.y + 1.0), (float)(end.z - cameraPos.z))
                .color(255, 0, 0, 255);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        matrices.pop();
    }

    private void drawHealthBar(MatrixStack matrices, WorldRenderContext context, MobEntity mob) {
        float health = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        if (maxHealth <= 0) return;
        float healthRatio = Math.max(0, Math.min(1, health / maxHealth));

        matrices.push();
        matrices.translate(0, 0.3, 0); // Position above mob label
        matrices.scale(0.02f, 0.02f, 0.02f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float barWidth = 40.0f;
        float barHeight = 4.0f;
        float halfWidth = barWidth / 2;

        // Background bar (dark gray)
        drawQuad(buffer, matrix, -halfWidth, 0, barWidth, barHeight, 0, 0, 0, 200);

        // Health bar (green/yellow/red)
        float healthWidth = barWidth * healthRatio;
        int color = healthRatio > 0.5 ? 0x00FF00 : healthRatio > 0.25 ? 0xFFFF00 : 0xFF0000;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        drawQuad(buffer, matrix, -halfWidth, 0, healthWidth, barHeight, r, g, b, 255);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        matrices.pop();
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float x, float y, float width, float height, int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y, 0).color(r, g, b, a);
        buffer.vertex(matrix, x, y + height, 0).color(r, g, b, a);
        buffer.vertex(matrix, x + width, y + height, 0).color(r, g, b, a);
        buffer.vertex(matrix, x + width, y, 0).color(r, g, b, a);
    }

    private void drawMobLabel(MatrixStack matrices, WorldRenderContext context, MobEntity mob) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        MobWarData data = MobWarData.get(mob);
        if (data == null) return;

        // Get display name from MobWarVisuals
        Text displayName = MobWarVisuals.getMobDisplayName(mob);
        
        matrices.push();
        matrices.scale(0.02f, -0.02f, 0.02f); // Fixed scale for 1.21.1

        // Center the text
        float x = -textRenderer.getWidth(displayName) / 2.0f;
        
        // Draw with see-through mode (correct for 1.21.1)
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        textRenderer.draw(displayName, x, 0, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), 
                         immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        immediate.draw();

        matrices.pop();
    }
}
