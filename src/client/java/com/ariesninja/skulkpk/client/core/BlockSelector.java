package com.ariesninja.skulkpk.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class BlockSelector {
    private static BlockPos selectedBlock;

    public static void selectBlock(BlockHitResult hitResult, MinecraftClient client) {
        if (hitResult != null && client.player != null) {
            selectedBlock = hitResult.getBlockPos();
            client.player.sendMessage(Text.literal("Selected block at: " + selectedBlock.toShortString()), false);
        }
    }

    public static BlockPos getSelectedBlock() {
        return selectedBlock;
    }
} 