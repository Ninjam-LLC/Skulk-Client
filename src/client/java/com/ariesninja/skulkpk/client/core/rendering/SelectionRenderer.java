package com.ariesninja.skulkpk.client.core.rendering;

import com.ariesninja.skulkpk.client.core.BlockSelector;
import com.ariesninja.skulkpk.client.core.JumpAnalyzer;
import me.x150.renderer.render.Renderer3d;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class SelectionRenderer {

    private static boolean highlightsVisible = true;

    public static void hideAllHighlights() {
        highlightsVisible = false;
    }

    public static void showHighlights() {
        highlightsVisible = true;
    }

    static {
        Renderer3d.renderThroughWalls();
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

                Renderer3d.renderEdged(matrixStack, new Color(255, 180, 180, 120), new Color(255, 0, 0), targetToHighlight.toCenterPos().add(-0.5, -0.5, -0.5), new Vec3d(1.0, 1.0, 1.0));

                // Highlight the jump-from block in blue
                BlockPos jumpFromBlock = JumpAnalyzer.getJumpFromBlock();
                if (jumpFromBlock != null && !jumpFromBlock.equals(targetToHighlight)) {
                    Renderer3d.renderEdged(matrixStack, new Color(180, 180, 255, 120), new Color(0, 0, 255), jumpFromBlock.toCenterPos().add(-0.5, -0.5, -0.5), new Vec3d(1.0, 1.0, 1.0));
                }

                // Render the momentum path in yellow
                Vec3d momentumPoint = JumpAnalyzer.getMomentumPoint();
                Vec3d jumpPoint = JumpAnalyzer.getJumpPoint();
                if (momentumPoint != null && jumpPoint != null) {
                    Renderer3d.renderLine(matrixStack, new Color(255, 255, 0), momentumPoint, jumpPoint);
                }
            }
        });
    }

}
