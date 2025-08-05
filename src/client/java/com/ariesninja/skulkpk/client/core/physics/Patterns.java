package com.ariesninja.skulkpk.client.core.physics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class Patterns {

    private static boolean matchesPattern(MinecraftClient client, BlockPos pos, int patternValue) {
        if (client == null || client.world == null || pos == null) {
            return false; // Invalid state, cannot check pattern
        }
        // Check if the block at the position matches the pattern value
        // 0 = empty space, 1 = solid block, 9 = doesn't matter
        if (patternValue == 0) {
            return !client.world.getBlockState(pos).isSolidBlock(client.world, pos);
        } else if (patternValue == 1) {
            return client.world.getBlockState(pos).isSolidBlock(client.world, pos);
        } else if (patternValue == 9) {
            return true;
        }
        return false;
    }

    public static boolean doesJumpMatchPattern(MinecraftClient client, BlockPos startPos, BlockPos endPos, int[][] pattern) {
        // A pattern is a 2d array where:
        // 0 = empty space
        // 1 = solid block
        // 9 = doesn't matter
        // The pattern is checked against the blocks behind the start pos, i.e. the middle value of the first row represents the start block.
        int patternLength = pattern.length;
        int patternWidth = pattern[0].length;
        // If width is even, throw
        if (patternWidth % 2 == 0) {
            throw new IllegalArgumentException("Pattern width must be odd");
        }
        // Get the y coordinate of the start position
        int startY = startPos.getY() - 1;
        // Using the delta between the start and end positions, find which direction in the world correlates to the pattern's width
        int deltaX = startPos.getX() - endPos.getX();
        int deltaZ = startPos.getZ() - endPos.getZ();
        // Determine the direction of the pattern based on the delta
        int patternDirectionX = 0;
        int patternDirectionZ = 0;
        if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            // X direction is dominant
            patternDirectionX = Integer.signum(deltaX);
        } else if (Math.abs(deltaZ) > Math.abs(deltaX)) {
            // Z direction is dominant
            patternDirectionZ = Integer.signum(deltaZ);
        } else {
            // Both deltas are equal, treat as diagonal, invalid for this pattern check
            throw new IllegalArgumentException("Pattern check does not support diagonal jumps");
        }
        // Calculate the starting position for the pattern check
        int startX = startPos.getX();
        int startZ = startPos.getZ();
        // Begin checking the pattern systematically where i and j represent some combination of X and Z (or their negatives). Y stays constant.
        for (int i = 0; i < patternLength; i++) {
            for (int j = 0; j < patternWidth; j++) {
                // Calculate the current block position based on the pattern
                int currentX = startX + (i * patternDirectionX) + (j - (patternWidth / 2)) * patternDirectionZ;
                int currentZ = startZ + (i * patternDirectionZ) + (j - (patternWidth / 2)) * patternDirectionX;
                BlockPos currentPos = new BlockPos(currentX, startY, currentZ);

                // Check if the block matches the pattern
                if (!matchesPattern(client, currentPos, pattern[i][j])) {
                    return false; // Pattern does not match
                }
            }
        }
        return true; // All blocks match the pattern
    }

}
