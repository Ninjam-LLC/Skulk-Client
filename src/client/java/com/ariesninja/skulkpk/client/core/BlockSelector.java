package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.rendering.SelectionRenderer;
import com.ariesninja.skulkpk.client.util.ChatMessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class BlockSelector {
    private static BlockPos selectedBlock;

    public static void selectBlock(BlockHitResult hitResult, MinecraftClient client) {
        if (hitResult != null && client.player != null) {
            selectedBlock = hitResult.getBlockPos();

            // Analyze the jump and check if there were any errors
            JumpAnalyzer.analyzeJump(selectedBlock);

            // Only show success message if analysis completed without errors
            if (JumpAnalyzer.getOptimizedTargetBlock() != null) {
                showSuccessMessage(client, selectedBlock);
            }
        }
    }

    private static void showSuccessMessage(MinecraftClient client, BlockPos blockPos) {
        SelectionRenderer.showHighlights();
        ChatMessageUtil.sendSuccess(client, "Selected block at: " + blockPos.toShortString());
    }

    public static BlockPos getSelectedBlock() {
        return selectedBlock;
    }

    public static void clearSelection() {
        selectedBlock = null;
        SelectionRenderer.hideAllHighlights();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ChatMessageUtil.sendWarn(client, "Selection cleared");
        }
    }
}
