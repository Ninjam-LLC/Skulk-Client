package com.ariesninja.skulkpk.client.core.physics;

import com.ariesninja.skulkpk.client.core.JumpPlanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class Obstructions {

    // Check if the jump is a Neo jump, which requires specific conditions
    public static boolean isNeoJump(MinecraftClient client, JumpPlanner.JumpLogistics logistics) {
        return neoProps(client, logistics) > 0;
    }

    public static boolean isMinorNeo(MinecraftClient client, JumpPlanner.JumpLogistics logistics) {
        // Minor Neo jumps are those with 1-2 props
        int props = neoProps(client, logistics);
        return props > 0 && props < 3;
    }

    public static int neoProps(MinecraftClient client, JumpPlanner.JumpLogistics logistics) {
        // World check
        if (client.world == null || client.player == null) {
            return 0; // No world or player context
        }
        // Offset check
        if (logistics.getOffset() != 0) {
            return 0;
        }
        // Check distance is between 1 and 3 inclusive
        double distance = logistics.getDistance();
        if (distance < 1 || distance > 3) {
            return 0;
        }
        // Check that blocks at the players head height are obstructing the jump
        BlockPos jumpPosHead = logistics.getJumpBlockPos().up();
        BlockPos targetPosHead = logistics.getTargetBlockPos().up();

        // Start counting props
        int props = 0;

        // Check each block in the path along X axis
        int minX = Math.min(jumpPosHead.getX(), targetPosHead.getX());
        int maxX = Math.max(jumpPosHead.getX(), targetPosHead.getX());
        for (int x = minX; x <= maxX; x++) {
            BlockPos pos = new BlockPos(x, jumpPosHead.getY(), targetPosHead.getZ());
            if (client.world.getBlockState(pos).isSolidBlock(client.world, pos)) {
                props++; // Found an obstruction
            }
        }

        // Check each block in the path along Z axis
        int minZ = Math.min(jumpPosHead.getZ(), targetPosHead.getZ());
        int maxZ = Math.max(jumpPosHead.getZ(), targetPosHead.getZ());
        for (int z = minZ; z <= maxZ; z++) {
            BlockPos pos = new BlockPos(jumpPosHead.getX(), jumpPosHead.getY(), z);
            if (client.world.getBlockState(pos).isSolidBlock(client.world, pos)) {
                props++; // Found an obstruction
            }
        }

        // Return the number of props found
        System.out.println("DEBUG: Neo jump props found: " + props);
        return props;
    }

    public static BlockPos getLastNeoProp(MinecraftClient client, JumpPlanner.JumpLogistics logistics) {
        // Get the number of props
        int props = neoProps(client, logistics);
        if (props == 0) {
            return null; // No props found
        }

        // Get the jump and target positions
        BlockPos jumpPos = logistics.getJumpBlockPos();
        BlockPos targetPos = logistics.getTargetBlockPos();

        BlockPos storedPos = null;

        // Check along X axis first
        int minX = Math.min(jumpPos.getX(), targetPos.getX());
        int maxX = Math.max(jumpPos.getX(), targetPos.getX());
        for (int x = minX; x <= maxX; x++) {
            BlockPos pos = new BlockPos(x, jumpPos.getY() + 1, targetPos.getZ());
            if (client.world.getBlockState(pos).isSolidBlock(client.world, pos)) {
                storedPos = pos; // Found a prop
            }
        }

        // Check along Z axis next
        int minZ = Math.min(jumpPos.getZ(), targetPos.getZ());
        int maxZ = Math.max(jumpPos.getZ(), targetPos.getZ());
        for (int z = minZ; z <= maxZ; z++) {
            BlockPos pos = new BlockPos(jumpPos.getX(), jumpPos.getY() + 1, z);
            if (client.world.getBlockState(pos).isSolidBlock(client.world, pos)) {
                storedPos = pos; // Found a prop
            }
        }

        return storedPos;
    }

    public static BlockPos getBlockAfterLastProp(MinecraftClient client, JumpPlanner.JumpLogistics logistics) {
        // Get the last prop position
        BlockPos lastProp = getLastNeoProp(client, logistics);
        if (lastProp == null) {
            return null; // No props found
        }

        // Get the jump and target positions
        BlockPos jumpPos = logistics.getJumpBlockPos();
        BlockPos targetPos = logistics.getTargetBlockPos();

        // Determine direction of the jump
        int dx = Integer.signum(targetPos.getX() - jumpPos.getX());
        int dz = Integer.signum(targetPos.getZ() - jumpPos.getZ());

        // Calculate the next block after the last prop
        return lastProp.add(dx, 0, dz);
    }
}
