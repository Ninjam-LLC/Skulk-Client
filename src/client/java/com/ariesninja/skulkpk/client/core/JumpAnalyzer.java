package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.rendering.SelectionRenderer;
import com.ariesninja.skulkpk.client.util.ChatMessageUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class JumpAnalyzer {

    private static BlockPos currentStandingBlock = null;
    private static BlockPos jumpFromBlock = null;
    private static BlockPos optimizedTargetBlock = null;
    private static BlockPos momentumStartBlock = null;

    // Fine-grained position variables for precise jump calculations
    private static Vec3d jumpPoint = null; // Precise jump position (0.3 blocks past jumpFromBlock edge)
    private static Vec3d momentumPoint = null; // Precise momentum start position

    public static void analyzeJump(BlockPos target) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        World world = client.world;

        if (player == null || world == null) return;

        // Get the block we are standing on
        currentStandingBlock = getStandingBlock(player);

        // Find the closest reachable block to the target without jumping
        jumpFromBlock = findClosestReachableBlock(player, world, target);

        if (jumpFromBlock == null) {
            showError("The block you are standing on is not yet supported!");
            return;
        }

        // Find the optimal target block
        optimizedTargetBlock = findOptimalTargetBlock(world, target, jumpFromBlock);

        // Find the optimal momentum starting position (only if we have valid jump points)
        if (jumpFromBlock != null && optimizedTargetBlock != null) {
            momentumStartBlock = findMomentumStartPosition(world, jumpFromBlock, optimizedTargetBlock);

            // Calculate precise jump point (0.3 blocks past jumpFromBlock edge toward target)
            if (jumpFromBlock != null && optimizedTargetBlock != null) {
                jumpPoint = calculateJumpPoint(jumpFromBlock, optimizedTargetBlock);
            }
        }
    }

    private static BlockPos getStandingBlock(PlayerEntity player) {
        Vec3d playerPos = player.getPos();
        // Get the block position below the player's feet
        return BlockPos.ofFloored(playerPos.x, playerPos.y - 0.1, playerPos.z);
    }

    private static BlockPos findClosestReachableBlock(PlayerEntity player, World world, BlockPos target) {
        Vec3d playerPos = player.getPos();
        BlockPos playerBlock = BlockPos.ofFloored(playerPos);

        // Check if the initial player block is walkable, if not find the nearest walkable block
        if (!isWalkableSurface(world, playerBlock)) {
            // Find the nearest walkable block around the player's position
            playerBlock = findNearestWalkableBlock(world, playerBlock);
            if (playerBlock == null) {
                // No walkable block found nearby, return null
                return null;
            }
        }

        // Search in expanding radius around player
        int maxRadius = 10; // Search within 10 blocks
        double closestDistance = Double.MAX_VALUE;
        BlockPos closestBlock = playerBlock;

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Only check the perimeter of current radius
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius && radius > 0) continue;

                    BlockPos checkPos = playerBlock.add(dx, 0, dz);

                    // Check if this block is reachable (walkable surface)
                    if (isWalkableSurface(world, checkPos)) {
                        // Check if we can reach this block without jumping (simple pathfinding)
                        if (canReachWithoutJumping(world, playerBlock, checkPos)) {
                            double distance = checkPos.getSquaredDistance(target);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestBlock = checkPos;
                            }
                        }
                    }
                }
            }
        }

        return closestBlock;
    }

    private static BlockPos findNearestWalkableBlock(World world, BlockPos startPos) {
        // Search in expanding radius to find the nearest walkable block
        int maxRadius = 2; // Search within 3 blocks of the starting position

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -1; dy <= 1; dy++) { // Check one block up and down as well
                    for (int dz = -radius; dz <= radius; dz++) {
                        // Only check the perimeter of current radius (except for radius 0)
                        if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

                        BlockPos checkPos = startPos.add(dx, dy, dz);

                        if (isWalkableSurface(world, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        // No walkable block found
        return null;
    }

    private static boolean isWalkableSurface(World world, BlockPos pos) {
        // Check if the block below is solid and the block at pos and above are passable
        // OR if the current position has a ladder (can climb ladders)
        return (world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) &&
                !world.getBlockState(pos).isSolidBlock(world, pos) &&
                !world.getBlockState(pos.up()).isSolidBlock(world, pos.up())) ||
//                isLadderClimbable(world, pos);
                isLadderProp(world, pos);
    }

    private static boolean canReachWithoutJumping(World world, BlockPos start, BlockPos end) {
        // Simple check: can we walk there on the same Y level or using ladders/steps?
        // For now, just check if there's a clear horizontal path at the same Y level

        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));

        if (steps == 0) return true;

        double stepX = (double) dx / steps;
        double stepZ = (double) dz / steps;

        for (int i = 0; i <= steps; i++) {
            int x = start.getX() + (int) Math.round(i * stepX);
            int z = start.getZ() + (int) Math.round(i * stepZ);
            BlockPos checkPos = new BlockPos(x, start.getY(), z);

            // Check if we can walk through this position
            if (!isWalkableSurface(world, checkPos)) {
                return false;
            }
        }

        return true;
    }

    private static BlockPos findOptimalTargetBlock(World world, BlockPos target, BlockPos jumpFrom) {
        // Search around the walkable target to find the closest position to our jump-from block
        // with proper staircase constraints: each step outward can only drop by 1 block max

        BlockPos walkableTarget = target; // Start with the original target
        while (!isWalkableSurface(world, walkableTarget)) {
            // If the target is not walkable, try moving up until we find a walkable surface
            walkableTarget = walkableTarget.up();
            if (walkableTarget.getY() > world.getHeight() + 1) {
                // If we exceed the world height, break to avoid infinite loop and message the user
                showError("Target is too high - no valid walkable surface found above the target");
                return null;
            }
        }

        // Use BFS-style layer search to ensure proper staircase pathfinding
        return findClosestReachablePosition(world, jumpFrom, walkableTarget);
    }

    private static BlockPos findClosestReachablePosition(World world, BlockPos jumpFrom, BlockPos targetArea) {
        // BFS-style search starting from the TARGET AREA to find the closest reachable position to the jump-from block
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, Integer> distances = new HashMap<>();

        // Start from the target area, not the jump-from position!
        queue.offer(targetArea);
        visited.add(targetArea);
        distances.put(targetArea, 0);

        BlockPos bestTarget = targetArea;
        double closestDistanceToJumpFrom = targetArea.getSquaredDistance(jumpFrom);
        boolean originalTargetIsJumpable = isJumpReachable(world, jumpFrom, targetArea);

        // Check if the target is "connected" (reachable by just walking/climbing stairs)
        if (isConnectedByWalking(world, jumpFrom, targetArea)) {
            showError("Target is reachable by walking - no jump required!");
            return null;
        }

        int maxSearchRadius = 5; // Increase search radius to find jumpable alternatives

        // If the original target is not jumpable, we MUST find a jumpable alternative
        boolean foundJumpableAlternative = originalTargetIsJumpable;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int currentDistance = distances.get(current);

            // Stop if we've searched too far
            if (currentDistance >= maxSearchRadius) continue;

            // Check if this position is jumpable from our jump-from position
            boolean currentIsJumpable = isJumpReachable(world, jumpFrom, current);

            // Determine if this is a better target
            boolean shouldUpdate = false;
            double distanceToJumpFrom = current.getSquaredDistance(jumpFrom);

            if (!originalTargetIsJumpable) {
                // Original target is not jumpable - we MUST find a jumpable position
                if (currentIsJumpable) {
                    if (!foundJumpableAlternative || distanceToJumpFrom < closestDistanceToJumpFrom) {
                        shouldUpdate = true;
                        foundJumpableAlternative = true;
                    }
                }
                // If current position is not jumpable and we haven't found a jumpable alternative, skip
            } else {
                // Original target is jumpable - prefer closer positions that are also jumpable
                if (currentIsJumpable && distanceToJumpFrom < closestDistanceToJumpFrom) {
                    shouldUpdate = true;
                }
                // Don't accept non-jumpable positions even if closer when original target is jumpable
            }

            if (shouldUpdate) {
                closestDistanceToJumpFrom = distanceToJumpFrom;
                bestTarget = current;
            }

            // Explore adjacent positions with staircase constraints
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue; // Skip the current position

                    // Check positions at same level, 1 up, and 1 down
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos next = current.add(dx, dy, dz);

                        if (visited.contains(next)) continue;

                        // Height constraint: no more than 1 block higher than jump-from position
                        if (next.getY() > jumpFrom.getY() + 1) {
                            continue;
                        }

                        // Check if this position is walkable
                        if (!isWalkableSurface(world, next)) {
                            continue;
                        }

                        visited.add(next);
                        distances.put(next, currentDistance + 1);
                        queue.offer(next);
                    }
                }
            }
        }

        // Check if we found a valid solution
        if (!originalTargetIsJumpable && !foundJumpableAlternative) {
            showError("No jumpable landing spot found near target!");
            return null;
        }

        return bestTarget;
    }

    private static boolean isJumpReachable(World world, BlockPos jumpFrom, BlockPos landingSpot) {
        // Check if we can jump from jumpFrom to landingSpot
        // Account for player positioning within blocks - players can be up to 0.3 blocks from block edge

        int dy = landingSpot.getY() - jumpFrom.getY();

        // If the block 1 below is a ladder, it's not a valid end point for a jump
        BlockState blockBelow = world.getBlockState(landingSpot.down());
        if (blockBelow.getBlock() instanceof LadderBlock) {
            return false; // Can't land on a ladder block
        }

        // Calculate the closest possible horizontal distance between the blocks
//        double minHorizontalDistance = calculateMinHorizontalDistance(jumpFrom, landingSpot);

        // Basic jump constraints for Minecraft:
        // - Maximum horizontal distance: ~4.3 blocks for running jump (from closest edges)
        // - Maximum upward jump: 1.25 blocks (so landing 1 block higher is possible)
        // - Can fall any reasonable distance

//        if (minHorizontalDistance > 4.3) {
//            return false;
//        }

        if (dy > 1) {
            return false;
        }

        // Check if there are blocks in the way (simplified - just check a few points along the arc)
        // In a real implementation, you'd simulate the full jump trajectory
        if (!hasJumpClearance(world, jumpFrom, landingSpot)) {
            return false;
        }

        return true;
    }

    private static double calculateMinHorizontalDistance(BlockPos from, BlockPos to) {
        // Calculate the minimum horizontal distance between two blocks
        // considering that the player can be positioned anywhere within each block

        // Get the block boundaries
        double fromMinX = from.getX();
        double fromMaxX = from.getX() + 1.0;
        double fromMinZ = from.getZ();
        double fromMaxZ = from.getZ() + 1.0;

        double toMinX = to.getX();
        double toMaxX = to.getX() + 1.0;
        double toMinZ = to.getZ();
        double toMaxZ = to.getZ() + 1.0;

        // Calculate minimum distance in X direction
        double minDx;
        if (fromMaxX <= toMinX) {
            // from block is completely to the left of to block
            minDx = toMinX - fromMaxX;
        } else if (toMaxX <= fromMinX) {
            // to block is completely to the left of from block
            minDx = fromMinX - toMaxX;
        } else {
            // blocks overlap in X direction
            minDx = 0.0;
        }

        // Calculate minimum distance in Z direction
        double minDz;
        if (fromMaxZ <= toMinZ) {
            // from block is completely behind to block
            minDz = toMinZ - fromMaxZ;
        } else if (toMaxZ <= fromMinZ) {
            // to block is completely behind from block
            minDz = fromMinZ - toMaxZ;
        } else {
            // blocks overlap in Z direction
            minDz = 0.0;
        }

        // Return the Euclidean distance
        return Math.sqrt(minDx * minDx + minDz * minDz);
    }

    private static boolean hasJumpClearance(World world, BlockPos start, BlockPos end) {
        // Simplified clearance check - ensure no blocks are blocking the jump path
        // Check a few points along the jump trajectory for obstacles

//        int steps = Math.max(Math.abs(end.getX() - start.getX()), Math.abs(end.getZ() - start.getZ()));
//        if (steps == 0) return true;
//
//        for (int i = 1; i <= steps; i++) {
//            double progress = (double) i / steps;
//            int x = (int) Math.round(start.getX() + progress * (end.getX() - start.getX()));
//            int z = (int) Math.round(start.getZ() + progress * (end.getZ() - start.getZ()));
//
//            // Assume jump arc goes up 1-2 blocks at the midpoint
//            int y = Math.max(start.getY(), end.getY()) + (i == steps / 2 ? 2 : 1);
//
//            BlockPos checkPos = new BlockPos(x, y, z);
//            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos) ||
//                    world.getBlockState(checkPos.up()).isSolidBlock(world, checkPos.up())) {
//                return false;
//            }
//        }
//
//        return true;
        return true;
    }

    public static BlockPos getCurrentStandingBlock() {
        return currentStandingBlock;
    }

    public static BlockPos getJumpFromBlock() {
        return jumpFromBlock;
    }

    public static BlockPos getOptimizedTargetBlock() {
        return optimizedTargetBlock;
    }

    public static BlockPos getMomentumStartBlock() {
        return momentumStartBlock;
    }

    // Getters for fine-grained positions
    public static Vec3d getJumpPoint() {
        return jumpPoint;
    }

    public static Vec3d getMomentumPoint() {
        return momentumPoint;
    }

    private static boolean isConnectedByWalking(World world, BlockPos start, BlockPos target) {
        // Check if we can reach the target by just walking and climbing single blocks (no jumping required)
        // This uses a simple BFS to see if there's a walking path

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);

        int maxHorizontalDistance = 20; // Maximum horizontal distance to search
        int searchedBlocks = 0;
        int maxSearchBlocks = 2000; // Increase total search limit

        while (!queue.isEmpty() && searchedBlocks < maxSearchBlocks) {
            BlockPos current = queue.poll();
            searchedBlocks++;

            // If we reached the target, it's connected
            if (current.equals(target)) {
                return true;
            }

            // Don't search too far horizontally from the start
            int horizontalDistance = Math.max(Math.abs(current.getX() - start.getX()), Math.abs(current.getZ() - start.getZ()));
            if (horizontalDistance > maxHorizontalDistance) {
                continue;
            }

            // Explore adjacent walkable positions
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    // Try moving to adjacent horizontal positions
                    BlockPos horizontalNext = current.add(dx, 0, dz);

                    // For each horizontal position, find the correct Y level to stand on
                    // Search from current level up 1, then down to 19 blocks below current
                    for (int searchY = current.getY() + 1; searchY >= current.getY() - 19; searchY--) {
                        BlockPos candidatePos = new BlockPos(horizontalNext.getX(), searchY, horizontalNext.getZ());

                        if (visited.contains(candidatePos)) continue;

                        // Check if this position is walkable
                        if (!isWalkableSurface(world, candidatePos)) continue;

                        // Check if we can step from current to this candidate position
                        if (!canWalkStep(world, current, candidatePos)) continue;

                        visited.add(candidatePos);
                        queue.offer(candidatePos);

                        // Only take the first (highest) valid position for this horizontal location
                        break;
                    }
                }
            }
        }

        return false;
    }

    private static boolean canWalkStep(World world, BlockPos from, BlockPos to) {
        // Check if we can walk/climb/fall from 'from' to 'to' without jumping
        // Now includes ladder climbing support
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int dy = to.getY() - from.getY();

        // Must be adjacent horizontally or vertically aligned for ladder climbing
        if (dx > 1 || dz > 1) return false;

        // Special case: ladder climbing
        if (isLadderClimbable(world, from) && isLadderClimbable(world, to)) {
            return canClimbLadder(world, from, to);
        }

        // If either position is a ladder but we can't climb between them,
        // check if we can still use normal movement rules
        if (isLadderClimbable(world, from) || isLadderClimbable(world, to)) {
            // Allow transitioning from/to ladders with more flexible height rules
            // Can go up to 2 blocks when involving ladders (climbing on/off)
            if (dy > 2) return false;
            if (dy < -10) return false; // Can fall further from ladders

            // Can't move diagonally when transitioning to/from ladders if going up
            if ((dx == 1 && dz == 1) && dy > 0) return false;

            return true;
        }

        // Normal walking rules (no ladders involved):
        // Can't move diagonally AND up at the same time
        if ((dx == 1 && dz == 1) && dy > 0) return false;

        // Standard walking rules:
        // - Can go up 1 block (climbing/stepping up)
        // - Can stay at same level
        // - Can fall down up to 3 blocks
        if (dy > 1) return false;  // Can't go up more than 1 block
        if (dy < -3) return false;  // Can't fall more than 3 blocks

        return true;
    }

    private static void showError(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ChatMessageUtil.sendError(client, message);
        }
        System.out.println("ERROR: Skulk > " + message);

        // Hide all highlights immediately
        SelectionRenderer.hideAllHighlights();

        // Clear ALL variables
        optimizedTargetBlock = null;
        jumpFromBlock = null;
        currentStandingBlock = null;
        momentumStartBlock = null;
        jumpPoint = null;
        momentumPoint = null;
    }

    private static BlockPos findMomentumStartPosition(World world, BlockPos jumpFrom, BlockPos target) {
        // Calculate the direction from jumpFrom to target
        Vec3d jumpToTarget = new Vec3d(
                target.getX() - jumpFrom.getX(),
                0, // Only consider horizontal direction for momentum
                target.getZ() - jumpFrom.getZ()
        ).normalize();

        // Calculate the base opposite direction (180 degrees) for momentum building
        Vec3d baseOppositeDirection = jumpToTarget.multiply(-1);

        System.out.println("=== Momentum Analysis ===");
        System.out.println("Jump from: " + jumpFrom + " to target: " + target);
        System.out.println("Base momentum direction: " + String.format("(%.2f, %.2f)", baseOppositeDirection.x, baseOppositeDirection.z));

        Vec3d bestMomentumStart = null;
        double maxMomentumDistance = 0;
        double bestAngle = 0;

        // Search in a 180-degree span (±90 degrees from the opposite direction)
        int angleSteps = 90; // Interval = 360 / angleSteps

        for (int angleStep = 0; angleStep < angleSteps; angleStep++) {
            // Calculate angle offset from -90 to +90 degrees
            double angleOffset = ((double) angleStep / (angleSteps - 1) - 0.5) * Math.PI; // -π/2 to +π/2

            // Rotate the base direction by the angle offset
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);

            Vec3d momentumDirection = new Vec3d(
                    baseOppositeDirection.x * cos - baseOppositeDirection.z * sin,
                    0,
                    baseOppositeDirection.x * sin + baseOppositeDirection.z * cos
            );

            if (angleStep % 15 == 0) { // Log every 15 steps for performance
                System.out.println("Checking angle " + Math.toDegrees(angleOffset) + "°: direction (" +
                        String.format("%.2f, %.2f)", momentumDirection.x, momentumDirection.z) + ")");
            }

            // Search for the longest straight line in this momentum direction
            double currentMomentumDistance = 0;
            Vec3d currentBestStart = new Vec3d(jumpFrom.getX() + 0.5, jumpFrom.getY(), jumpFrom.getZ() + 0.5);
            int currentY = jumpFrom.getY(); // Track Y level to prevent uphill paths

            // Test different distances along this momentum direction with finer granularity
            for (double distance = 0.1; distance <= 20; distance += 0.1) {
                // Calculate position at this distance using fine-grained coordinates
                double x = jumpFrom.getX() + 0.5 + momentumDirection.x * distance;
                double z = jumpFrom.getZ() + 0.5 + momentumDirection.z * distance;

                Vec3d candidatePos = new Vec3d(x, currentY, z);

                // Skip if we haven't moved significantly from the last position
                if (candidatePos.distanceTo(currentBestStart) < 0.05) continue;

                // Check if this position is walkable using block coordinates for world checks
                BlockPos blockPos = BlockPos.ofFloored(candidatePos.x, candidatePos.y, candidatePos.z);
                if (!isWalkableSurfaceEnhanced(world, blockPos)) {
                    // Try one block up in case there's a step
                    BlockPos upperPos = blockPos.up();
                    if (isWalkableSurfaceEnhanced(world, upperPos)) {
                        // If the upper position is walkable, use that Y level
                        currentY = upperPos.getY();
                        candidatePos = new Vec3d(candidatePos.x, currentY, candidatePos.z);
                    } else {
                        break;
                    }
                }

                // Path is clear, update our best momentum position
                currentBestStart = candidatePos;
                currentMomentumDistance = distance;
            }

            // Check if this direction gave us a better momentum path
            if (currentMomentumDistance > maxMomentumDistance) {
                maxMomentumDistance = currentMomentumDistance;
                bestMomentumStart = currentBestStart;
                bestAngle = Math.toDegrees(angleOffset);
                System.out.println("New best momentum path: " + String.format("%.2f", currentMomentumDistance) + " blocks at " +
                        String.format("%.1f°", bestAngle) + " to " + String.format("(%.2f, %.0f, %.2f)", currentBestStart.x, currentBestStart.y, currentBestStart.z));
            }
        }

        // Only return a momentum start if we found at least 0.5 blocks of runway
        if (maxMomentumDistance >= 0.5) {
            System.out.println("Momentum analysis complete: Found " + String.format("%.2f", maxMomentumDistance) + " blocks of runway at " +
                    String.format("%.1f°", bestAngle) + " angle");
            System.out.println("Best momentum start: " + String.format("(%.2f, %.0f, %.2f)", bestMomentumStart.x, bestMomentumStart.y, bestMomentumStart.z));

            // Set the precise momentum point (don't round it)
            momentumPoint = bestMomentumStart.add(0.0, 0.5, 0.0); // Centered at the block edge

            // Convert back to BlockPos for compatibility with existing code
            return BlockPos.ofFloored(bestMomentumStart.x, bestMomentumStart.y, bestMomentumStart.z);
        } else {
            System.out.println("Momentum analysis complete: Insufficient runway (" + String.format("%.2f", maxMomentumDistance) + " blocks, need 0.5+)");
            momentumPoint = null;
            return null;
        }
    }

    private static boolean hasDirectPathVec3d(World world, Vec3d start, Vec3d end) {
        // Check if there's a clear straight path between start and end using fine-grained coordinates
        // Uses enhanced walkable surface detection that accounts for 0.3 block buffer
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        int steps = (int) Math.ceil(distance * 4); // 4 checks per block for fine granularity

        if (steps == 0) return true;

        double stepX = dx / steps;
        double stepZ = dz / steps;

        for (int i = 0; i <= steps; i++) {
            double x = start.x + i * stepX;
            double z = start.z + i * stepZ;

            // Check at the same Y level first using enhanced detection
            BlockPos checkPos = BlockPos.ofFloored(x, start.y, z);
            if (isWalkableSurfaceEnhanced(world, checkPos)) {
                continue;
            }

            // Try one block up if current level is blocked
            BlockPos upperPos = checkPos.up();
            if (isWalkableSurfaceEnhanced(world, upperPos)) {
                continue;
            }

            // Path is blocked
            return false;
        }

        return true;
    }

    private static Vec3d calculateJumpPoint(BlockPos jumpFrom, BlockPos target) {
        // Calculate the precise jump point: 0.3 blocks past the jumpFromBlock edge toward the target

        // Get the momentum direction (from momentum start to jump block center)
        Vec3d momentumPoint = getMomentumPoint();
        Vec3d jumpFromCenter = new Vec3d(jumpFrom.getX() + 0.5, jumpFrom.getY(), jumpFrom.getZ() + 0.5);

        Vec3d momentumDirection = null;
        if (momentumPoint != null) {
            momentumDirection = jumpFromCenter.subtract(momentumPoint).normalize();
        }

        // Calculate the direction vector from jumpFrom to target
        Vec3d targetDirection = new Vec3d(
                target.getX() - jumpFrom.getX(),
                0, // Ignore vertical component for jump point calculation
                target.getZ() - jumpFrom.getZ()
        ).normalize();

        // Calculate the jump point position
        Vec3d jumpPoint;

        if (momentumDirection != null) {
            // Calculate the angle between momentum direction and target direction
            double dotProduct = momentumDirection.x * targetDirection.x + momentumDirection.z * targetDirection.z;
            // Clamp dot product to avoid numerical errors with acos
            dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));
            double theta = Math.acos(dotProduct);

            // We want to go 0.3 blocks past the edge in the target direction
            // From block center, we need to go 0.5 blocks to reach the edge, then 0.3 more
            // Total distance in target direction: 0.8 blocks
            double distanceInTargetDirection = 0.8;

            // Calculate distance in momentum direction using trigonometry
            // If theta is 0 (same direction), we go 0.8 in momentum direction
            // If theta is 90° (perpendicular), we go 0 in momentum direction
            double distanceInMomentumDirection = distanceInTargetDirection * Math.cos(theta);

            // Calculate the jump point using both components
            jumpPoint = new Vec3d(
                    jumpFromCenter.x + targetDirection.x * distanceInTargetDirection + momentumDirection.x * (distanceInMomentumDirection - distanceInTargetDirection),
                    jumpFrom.getY() + 0.5, // Keep at block center height
                    jumpFromCenter.z + targetDirection.z * distanceInTargetDirection + momentumDirection.z * (distanceInMomentumDirection - distanceInTargetDirection)
            );
        } else {
            // Fallback: no momentum direction available, just go 0.8 blocks in target direction
            jumpPoint = new Vec3d(
                    jumpFromCenter.x + targetDirection.x * 0.8,
                    jumpFrom.getY() + 0.5,
                    jumpFromCenter.z + targetDirection.z * 0.8
            );
        }

        return jumpPoint;
    }

    private static boolean isWalkableSurfaceEnhanced(World world, BlockPos pos) {
        // Check if the player can stand anywhere within 0.3 blocks of the given position
        // This accounts for the fact that players can stand near block edges
        // Also include ladder climbing positions

        // First check if this is a ladder climbing position
        if (isLadderClimbable(world, pos)) {
            return true;
        }

        // Check a 0.6x0.6 region (±0.3 blocks) around the position at 0.05 block intervals
        for (double offsetX = -0.3; offsetX <= 0.3; offsetX += 0.05) {
            for (double offsetZ = -0.3; offsetZ <= 0.3; offsetZ += 0.05) {
                // Calculate the actual position to check
                double checkX = pos.getX() + 0.5 + offsetX; // Center of block + offset
                double checkZ = pos.getZ() + 0.5 + offsetZ; // Center of block + offset

                BlockPos checkPos = BlockPos.ofFloored(checkX, pos.getY(), checkZ);

                // Check if this specific position is walkable:
                // - Block below must be solid (to stand on)
                // - Block at position must be passable (air/non-solid)
                // - Block above must be passable (head room)
                // - OR it's a ladder position
                if ((world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down()) &&
                        !world.getBlockState(checkPos).isSolidBlock(world, checkPos) &&
                        !world.getBlockState(checkPos.up()).isSolidBlock(world, checkPos.up())) ||
                        isLadderClimbable(world, checkPos)) {

                    // Found at least one valid standing position within the region
                    return true;
                }
            }
        }

        // No valid standing position found within the 0.3-block region
        return false;
    }

    // Helper method to detect if a position is climbable via ladder
    private static boolean isLadderClimbable(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);

        // Check if the current block is a ladder
        if (blockState.getBlock() instanceof LadderBlock) {
            // Ensure there's head room (block above is passable)
            return !world.getBlockState(pos.up()).isSolidBlock(world, pos.up());
        }

        return false;
    }

    // Helper method to detect if a position is a ladder prop (1 block above a ladder)
    private static boolean isLadderProp(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);

        // Check if the current block is air
        if (blockState.isAir()) {
            // Check if the block below is air
            return world.getBlockState(pos.down()).getBlock() instanceof LadderBlock;
        }

        return false;
    }

    // Helper method to check if we can climb between two ladder positions
    private static boolean canClimbLadder(World world, BlockPos from, BlockPos to) {
        // Both positions must be ladder-climbable
        if (!isLadderClimbable(world, from) || !isLadderClimbable(world, to)) {
            return false;
        }

        int dy = to.getY() - from.getY();
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());

        // Must be vertically aligned or adjacent horizontally
        if (dx > 1 || dz > 1) {
            return false;
        }

        // Can climb up or down any reasonable distance on ladders
        if (Math.abs(dy) > 10) { // Reasonable limit to prevent infinite climbing
            return false;
        }

        // Check that there's a continuous ladder path (simplified check)
        int steps = Math.abs(dy);
        if (steps > 0) {
            int stepY = dy > 0 ? 1 : -1;
            for (int i = 1; i < steps; i++) {
                BlockPos intermediatePos = from.add(0, i * stepY, 0);
                if (!isLadderClimbable(world, intermediatePos)) {
                    return false;
                }
            }
        }

        return true;
    }
}
