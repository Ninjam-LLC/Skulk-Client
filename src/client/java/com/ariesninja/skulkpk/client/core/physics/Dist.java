package com.ariesninja.skulkpk.client.core.physics;

import net.minecraft.util.math.BlockPos;

public class Dist {

    // Calculates the real distance between two edges
    public static double realDistance(BlockPos pos1, BlockPos pos2) {
        double x1 = pos1.getX();
        double y1 = pos1.getY();
        double z1 = pos1.getZ();
        double x2 = pos2.getX();
        double y2 = pos2.getY();
        double z2 = pos2.getZ();
        double deltaX = Math.max(0.0, Math.abs(x1 - x2) - 1.0);
        double deltaY = Math.max(0.0, Math.abs(y1 - y2) - 1.0);
        double deltaZ = Math.max(0.0, Math.abs(z1 - z2) - 1.0);
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    // Calculates the offset (distance between the shorter of x and z)
    public static double offset(BlockPos pos1, BlockPos pos2) {
        double x1 = pos1.getX();
        double z1 = pos1.getZ();
        double x2 = pos2.getX();
        double z2 = pos2.getZ();
        double deltaX = Math.abs(x1 - x2);
        double deltaZ = Math.abs(z1 - z2);
        return Math.min(deltaX, deltaZ);
    }

    // Get the real distance between two edges with the height difference considered
    public static double realDistanceWithHeight(BlockPos pos1, BlockPos pos2) {
        double horizontalDistance = realDistance(pos1, pos2);
        double verticalDistance = Math.abs(pos1.getY() - pos2.getY());
        return Math.sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance);
    }

}
