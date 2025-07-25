package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.rendering.SelectionRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

        String prefix = "Skulk";
        String arrow = " > ";
        String message = "Selected block at: " + blockPos.toShortString();

        // Create formatted text: aqua bold "Skulk" + gray " > " + green message
        Text prefixText = Text.literal(prefix).formatted(Formatting.AQUA, Formatting.BOLD);
        Text arrowText = Text.literal(arrow).formatted(Formatting.GRAY);
        Text messageText = Text.literal(message).withColor(0x97c29e);

        Text fullMessage = Text.empty().append(prefixText).append(arrowText).append(messageText);
        client.player.sendMessage(fullMessage, false);
    }

    public static BlockPos getSelectedBlock() {
        return selectedBlock;
    }
}
