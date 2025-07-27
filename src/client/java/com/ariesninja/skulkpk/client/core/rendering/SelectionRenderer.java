package com.ariesninja.skulkpk.client.core.rendering;

import com.ariesninja.skulkpk.client.core.BlockSelector;
import com.ariesninja.skulkpk.client.core.JumpAnalyzer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

public class SelectionRenderer {

    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
            "skulkpk_lines_no_depth",
            RenderLayer.getLines().getVertexFormat(),
            RenderLayer.getLines().getDrawMode(),
            256,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(10.0)))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .target(RenderPhase.MAIN_TARGET)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private static boolean highlightsVisible = true;

    public static void setHighlightsVisible(boolean visible) {
        highlightsVisible = visible;
    }

    public static void hideAllHighlights() {
        highlightsVisible = false;
    }

    public static void showHighlights() {
        highlightsVisible = true;
    }

    public static void highlightBlock(BlockPos pos, float r, float g, float b, float thickness, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, Camera camera) {
        matrixStack.push();
        matrixStack.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);

        VertexConsumer vertexConsumer = immediate.getBuffer(LINES_NO_DEPTH);
        drawBox(matrixStack, vertexConsumer, new Box(pos), r, g, b, 1.0F);
        immediate.draw(LINES_NO_DEPTH);

        matrixStack.pop();
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!highlightsVisible) {
                return; // Early exit if highlights should be hidden
            }

            BlockPos selectedBlock = BlockSelector.getSelectedBlock();
            if (selectedBlock != null && context.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
                MatrixStack matrixStack = context.matrixStack();
                Camera camera = context.camera();

                // Highlight the optimized target block in red (if available, otherwise use selected block)
                BlockPos targetToHighlight = JumpAnalyzer.getOptimizedTargetBlock();
                if (targetToHighlight == null) {
                    targetToHighlight = selectedBlock;
                }
                highlightBlock(targetToHighlight, 1.0F, 0F, 0F, 3.0F, matrixStack, immediate, camera);

                // Highlight the jump-from block in blue
                BlockPos jumpFromBlock = JumpAnalyzer.getJumpFromBlock();
                if (jumpFromBlock != null && !jumpFromBlock.equals(targetToHighlight)) {
                    highlightBlock(jumpFromBlock, 0F, 0F, 1.0F, 3.0F, matrixStack, immediate, camera);
                }

                // Render the momentum path in yellow
                BlockPos momentumStartBlock = JumpAnalyzer.getMomentumStartBlock();
                if (momentumStartBlock != null && jumpFromBlock != null) {
                    // Use fine-grained positions if available, otherwise fall back to block positions
                    drawMomentumPath(matrixStack, immediate, camera);
                }
            }
        });
    }

    private static void drawBox(MatrixStack matrices, VertexConsumer vertices, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom face
        vertices.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);

        // Top face
        vertices.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);

        // Connecting lines
        vertices.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);

        vertices.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertices.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
    }

    private static void drawMomentumPath(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, Camera camera) {
        matrixStack.push();
        matrixStack.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);

        VertexConsumer vertexConsumer = immediate.getBuffer(LINES_NO_DEPTH);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        // Use fine-grained positions if available, otherwise fall back to block positions
        Vec3d startPos = JumpAnalyzer.getMomentumPoint();
        Vec3d endPos = JumpAnalyzer.getJumpPoint();

        if (startPos != null && endPos != null) {
            // Use precise Vec3d positions for the momentum line
            float startX = (float) startPos.x;
            float startY = (float) startPos.y;
            float startZ = (float) startPos.z;

            float endX = (float) endPos.x;
            float endY = (float) endPos.y;
            float endZ = (float) endPos.z;

            // Draw the precise momentum line in yellow
            vertexConsumer.vertex(matrix, startX, startY, startZ).color(1.0f, 1.0f, 0F, 1.0f).normal(0, 1, 0);
            vertexConsumer.vertex(matrix, endX, endY, endZ).color(1.0f, 1.0f, 0F, 1.0f).normal(0, 1, 0);
        } else {
            // Fall back to block positions if fine-grained positions are not available
            BlockPos startBlock = JumpAnalyzer.getMomentumStartBlock();
            BlockPos endBlock = JumpAnalyzer.getJumpFromBlock();

            if (startBlock != null && endBlock != null) {
                // Use block center positions as fallback
                float startX = startBlock.getX() + 0.5f;
                float startY = startBlock.getY() + 0.5f;
                float startZ = startBlock.getZ() + 0.5f;

                float endX = endBlock.getX() + 0.5f;
                float endY = endBlock.getY() + 0.5f;
                float endZ = endBlock.getZ() + 0.5f;

                // Draw the fallback momentum line in yellow
                vertexConsumer.vertex(matrix, startX, startY, startZ).color(1.0f, 1.0f, 0F, 1.0f).normal(0, 1, 0);
                vertexConsumer.vertex(matrix, endX, endY, endZ).color(1.0f, 1.0f, 0F, 1.0f).normal(0, 1, 0);
            }
        }

        immediate.draw(LINES_NO_DEPTH);
        matrixStack.pop();
    }
}
