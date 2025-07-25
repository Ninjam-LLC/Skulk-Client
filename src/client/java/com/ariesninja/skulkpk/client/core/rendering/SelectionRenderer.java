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
                    drawMomentumPath(momentumStartBlock, jumpFromBlock, 1.0F, 1.0F, 0F, matrixStack, immediate, camera);
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

    private static void drawMomentumPath(BlockPos start, BlockPos end, float r, float g, float b, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, Camera camera) {
        matrixStack.push();
        matrixStack.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);

        VertexConsumer vertexConsumer = immediate.getBuffer(LINES_NO_DEPTH);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        // Calculate the center points of each block for the line
        float startX = start.getX() + 0.5f;
        float startY = start.getY() + 0.5f;
        float startZ = start.getZ() + 0.5f;

        float endX = end.getX() + 0.5f;
        float endY = end.getY() + 0.5f;
        float endZ = end.getZ() + 0.5f;

        // Draw a thick line by drawing multiple parallel lines
        float thickness = 0.1f;

        // Draw main line
        vertexConsumer.vertex(matrix, startX, startY, startZ).color(r, g, b, 1.0f).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, endX, endY, endZ).color(r, g, b, 1.0f).normal(0, 1, 0);

        // Draw additional lines for thickness (offset slightly)
        for (int i = 0; i < 4; i++) {
            float offsetX = (i % 2 == 0 ? thickness : -thickness);
            float offsetZ = (i < 2 ? thickness : -thickness);

            vertexConsumer.vertex(matrix, startX + offsetX, startY, startZ + offsetZ).color(r, g, b, 1.0f).normal(0, 1, 0);
            vertexConsumer.vertex(matrix, endX + offsetX, endY, endZ + offsetZ).color(r, g, b, 1.0f).normal(0, 1, 0);
        }

        immediate.draw(LINES_NO_DEPTH);
        matrixStack.pop();
    }
}
