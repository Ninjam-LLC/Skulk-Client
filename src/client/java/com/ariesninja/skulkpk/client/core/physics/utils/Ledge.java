package com.ariesninja.skulkpk.client.core.physics.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import com.google.common.collect.Streams;
import java.util.stream.Stream;

public class Ledge {

    /**
     * Checks if the player qualifies for auto-jump based on their current state and surroundings.
     *
     * @param player The player to check
     * @param edgeDistance The distance from the edge to check for blocks
     * @return true if the player should auto-jump, false otherwise
     */
    public static boolean shouldAutoJump(PlayerEntity player, double edgeDistance) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Don't auto-jump if player is not on ground or jump key is pressed
        if (!player.isOnGround() || mc.options.jumpKey.isPressed()) {
            return false;
        }

        // Don't auto-jump if player is sneaking or sneak key is pressed
        if (player.isSneaking() || mc.options.sneakKey.isPressed()) {
            return false;
        }

        // Check for blocks at the edge
        Box box = player.getBoundingBox();
        Box adjustedBox = box.offset(0, -0.5, 0).expand(-edgeDistance, 0, -edgeDistance);

        Stream<VoxelShape> blockCollisions = Streams.stream(mc.world.getBlockCollisions(player, adjustedBox));

        // If there are block collisions, don't auto-jump
        if (blockCollisions.findAny().isPresent()) {
            return false;
        }

        // All conditions met - player should auto-jump
        return true;
    }

    /**
     * Convenience method that uses the current player and a default edge distance.
     *
     * @param edgeDistance The distance from the edge to check for blocks
     * @return true if the current player should auto-jump, false otherwise
     */
    public static boolean shouldAutoJump(double edgeDistance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return false;
        }
        return shouldAutoJump(mc.player, edgeDistance);
    }
}
