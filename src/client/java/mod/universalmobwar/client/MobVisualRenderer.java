package mod.universalmobwar.client;

import mod.universalmobwar.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Handles rendering of visual features: target lines, health bars, mob labels.
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
        VertexConsumerProvider vertexConsumers = context.consumers();
        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();

        // Get all entities in render distance
        Iterable<Entity> entities = client.world.getEntities();

        for (Entity entity : entities) {
            if (!(entity instanceof MobEntity mob)) continue;

            // Check distance for performance
            double distance = entity.getPos().distanceTo(cameraPos);
            if (distance > 64.0) continue; // Only render within 64 blocks

            renderForMob(matrices, vertexConsumers, camera, mob, distance);
        }
    }

    private void renderForMob(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, MobEntity mob, double distance) {
        Vec3d mobPos = mob.getPos();
        Vec3d cameraPos = camera.getPos();

        // Target lines
        if (CONFIG.showTargetLines) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                drawTargetLine(matrices, vertexConsumers, camera, mobPos, target.getPos());
            }
        }

        // Health bars and labels (only if close enough)
        if (distance < 32.0) {
            matrices.push();
            matrices.translate(mobPos.x - cameraPos.x, mobPos.y - cameraPos.y + mob.getHeight() + 0.5, mobPos.z - cameraPos.z);
            matrices.multiply(camera.getRotation());

            // Health bar
            if (CONFIG.showHealthBars) {
                drawHealthBar(matrices, vertexConsumers, camera, mob);
            }

            // Mob labels
            if (CONFIG.showMobLabels) {
                drawMobLabel(matrices, vertexConsumers, mob);
            }

            matrices.pop();
        }
    }

    private void drawTargetLine(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, Vec3d start, Vec3d end) {
        matrices.push();
        Vec3d cameraPos = camera.getPos();

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Line from mob to target
        consumer.vertex(matrix, (float)(start.x - cameraPos.x), (float)(start.y - cameraPos.y + 1.0), (float)(start.z - cameraPos.z))
                .color(1, 0, 0, 1).normal(0, 1, 0);
        consumer.vertex(matrix, (float)(end.x - cameraPos.x), (float)(end.y - cameraPos.y + 1.0), (float)(end.z - cameraPos.z))
                .color(1, 0, 0, 1).normal(0, 1, 0);

        matrices.pop();
    }

    private void drawHealthBar(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, MobEntity mob) {
        float health = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        if (maxHealth <= 0) return;
        float healthRatio = Math.max(0, Math.min(1, health / maxHealth));

        Vec3d cameraPos = camera.getPos();
        Vec3d mobPos = mob.getPos().add(0, mob.getHeight() + 0.5, 0);

        matrices.push();
        matrices.translate(mobPos.x - cameraPos.x, mobPos.y - cameraPos.y, mobPos.z - cameraPos.z);
        matrices.multiply(camera.getRotation()); // Billboard

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float barWidth = 1.0f;
        float barHeight = 0.1f;
        float halfWidth = barWidth / 2;

        // Background bar (black)
        drawLine(consumer, matrix, -halfWidth, 0, 0, halfWidth, 0, 0, 0, 0, 0, 1);
        drawLine(consumer, matrix, halfWidth, 0, 0, halfWidth, barHeight, 0, 0, 0, 0, 1);
        drawLine(consumer, matrix, halfWidth, barHeight, 0, -halfWidth, barHeight, 0, 0, 0, 0, 1);
        drawLine(consumer, matrix, -halfWidth, barHeight, 0, -halfWidth, 0, 0, 0, 0, 0, 1);

        // Health bar (green/yellow/red)
        float healthWidth = barWidth * healthRatio;
        float halfHealthWidth = healthWidth / 2;
        int color = healthRatio > 0.5 ? 0x00FF00 : healthRatio > 0.25 ? 0xFFFF00 : 0xFF0000;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        drawLine(consumer, matrix, -halfHealthWidth, 0, 0, halfHealthWidth, 0, 0, r, g, b, 1);
        drawLine(consumer, matrix, halfHealthWidth, 0, 0, halfHealthWidth, barHeight, 0, r, g, b, 1);
        drawLine(consumer, matrix, halfHealthWidth, barHeight, 0, -halfHealthWidth, barHeight, 0, r, g, b, 1);
        drawLine(consumer, matrix, -halfHealthWidth, barHeight, 0, -halfHealthWidth, 0, 0, r, g, b, 1);

        matrices.pop();
    }

    private void drawLine(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
    }

    private void drawMobLabel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, MobEntity mob) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        // Get mob name
        String name = mob.getType().getTranslationKey().replace("entity.minecraft.", "");
        String label = name;

        matrices.push();
        matrices.translate(0, 0.5, 0);
        matrices.scale(0.02f, 0.02f, 0.02f); // Scale down

        // Center the text
        float x = -textRenderer.getWidth(label) / 2.0f;
        textRenderer.draw(label, x, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);

        matrices.pop();
    }

    private void drawQuad(VertexConsumer consumer, MatrixStack matrices, float x, float y, float width, float height, int color) {
        // Removed for now
    }
}