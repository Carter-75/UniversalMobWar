package mod.universalmobwar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mod.universalmobwar.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Handles rendering of visual features: target lines.
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

}
