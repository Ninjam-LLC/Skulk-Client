package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.rendering.SelectionRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class JumpAnalyzer {

    private static BlockPos currentStandingBlock = null;
    private static BlockPos jumpFromBlock = null;
    private static BlockPos optimizedTargetBlock = null;
    private static BlockPos momentumStartBlock = null;

    public static void analyzeJump(BlockPos target) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        World world = client.world;

        if (player == null || world == null) return;

        // Get the block we are standing on
        currentStandingBlock = getStandingBlock(player);

        // Find the closest reachable block to the target without jumping
        jumpFromBlock = findClosestReachableBlock(player, world, target);

        // Find the optimal target block
        optimizedTargetBlock = findOptimalTargetBlock(world, target, jumpFromBlock);

        // Find the optimal momentum starting position (only if we have valid jump points)
        if (jumpFromBlock != null && optimizedTargetBlock != null) {
            momentumStartBlock = findMomentumStartPosition(world, jumpFromBlock, optimizedTargetBlock);
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

    private static boolean isWalkableSurface(World world, BlockPos pos) {
        // Check if the block below is solid and the block at pos and above are passable
        return world.getBlockState(pos.down()).isSolidBlock(world, pos.down()) &&
               !world.getBlockState(pos).isSolidBlock(world, pos) &&
               !world.getBlockState(pos.up()).isSolidBlock(world, pos.up());
    }

    private static boolean canReachWithoutJumping(World world, BlockPos start, BlockPos end) {
        // Simple check: can we walk there on the same Y level or going down?
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

        // Calculate the closest possible horizontal distance between the blocks
        double minHorizontalDistance = calculateMinHorizontalDistance(jumpFrom, landingSpot);

        // Basic jump constraints for Minecraft:
        // - Maximum horizontal distance: ~4.3 blocks for running jump (from closest edges)
        // - Maximum upward jump: 1.25 blocks (so landing 1 block higher is possible)
        // - Can fall any reasonable distance

        if (minHorizontalDistance > 4.3) {
            return false;
        }

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

        int steps = Math.max(Math.abs(end.getX() - start.getX()), Math.abs(end.getZ() - start.getZ()));
        if (steps == 0) return true;

        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            int x = (int) Math.round(start.getX() + progress * (end.getX() - start.getX()));
            int z = (int) Math.round(start.getZ() + progress * (end.getZ() - start.getZ()));

            // Assume jump arc goes up 1-2 blocks at the midpoint
            int y = Math.max(start.getY(), end.getY()) + (i == steps / 2 ? 2 : 1);

            BlockPos checkPos = new BlockPos(x, y, z);
            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos) ||
                world.getBlockState(checkPos.up()).isSolidBlock(world, checkPos.up())) {
                return false;
            }
        }

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
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int dy = to.getY() - from.getY();

        // Must be adjacent horizontally
        if (dx > 1 || dz > 1) return false;

        // Can't move diagonally AND up at the same time
        if ((dx == 1 && dz == 1) && dy > 0) return false;

        // Walking rules:
        // - Can go up 1 block (climbing/stepping up)
        // - Can stay at same level
        // - Can fall down up to 19 blocks
        if (dy > 1) return false;  // Can't go up more than 1 block
        if (dy < -19) return false;  // Can't fall more than 19 blocks

        return true;
    }

    private static void showError(String message) {
        String prefix = "Skulk"; // Configurable prefix
        String arrow = " > "; // Gray arrow
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Create formatted text: aqua bold "Skulk" + gray " > " + red message
            Text prefixText = Text.literal(prefix).formatted(Formatting.AQUA, Formatting.BOLD);
            Text arrowText = Text.literal(arrow).formatted(Formatting.GRAY);
            Text messageText = Text.literal(message).formatted(Formatting.RED);

            Text fullMessage = Text.empty().append(prefixText).append(arrowText).append(messageText);
            client.player.sendMessage(fullMessage, false);
        }
        System.out.println("ERROR: " + prefix + arrow + message);

        // Hide all highlights immediately
        SelectionRenderer.hideAllHighlights();

        // Clear ALL variables
        optimizedTargetBlock = null;
        jumpFromBlock = null;
        currentStandingBlock = null;
        momentumStartBlock = null;
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

        BlockPos bestMomentumStart = null;
        int maxMomentumDistance = 0;
        double bestAngle = 0;

        // Search in a 180-degree span (±90 degrees from the opposite direction)
        int angleSteps = 72; // Check every 10 degrees (180 degrees / 18 = 10 degrees per step)

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

            if (angleStep % 6 == 0) { // Log every 60 degrees
                System.out.println("Checking angle " + Math.toDegrees(angleOffset) + "°: direction (" +
                    String.format("%.2f, %.2f", momentumDirection.x, momentumDirection.z) + ")");
            }

            // Search for the longest straight line in this momentum direction
            int currentMomentumDistance = 0;
            BlockPos currentBestStart = jumpFrom;

            // Test different distances along this momentum direction with finer granularity
            for (double distance = 0.5; distance <= 20; distance += 0.5) {
                // Calculate position at this distance
                double x = jumpFrom.getX() + momentumDirection.x * distance;
                double z = jumpFrom.getZ() + momentumDirection.z * distance;

                BlockPos candidatePos = new BlockPos((int) Math.round(x), jumpFrom.getY(), (int) Math.round(z));

                // Skip if we've already checked this exact position in this direction
                if (candidatePos.equals(currentBestStart)) continue;

                // Check if this position is walkable
                if (!isWalkableSurface(world, candidatePos)) {
                    // Try one block up in case there's a step
                    BlockPos upperPos = candidatePos.up();
                    if (isWalkableSurface(world, upperPos)) {
                        candidatePos = upperPos;
                    } else {
                        // Hit an obstacle, stop searching in this direction
                        break;
                    }
                }

                // Check if we can walk from jumpFrom to this position in a straight line
                if (hasDirectPath(world, jumpFrom, candidatePos)) {
                    currentBestStart = candidatePos;
                    currentMomentumDistance = (int) Math.round(distance);
                } else {
                    // Path is blocked, stop searching
                    break;
                }
            }

            // Check if this direction gave us a better momentum path
            if (currentMomentumDistance > maxMomentumDistance) {
                maxMomentumDistance = currentMomentumDistance;
                bestMomentumStart = currentBestStart;
                bestAngle = Math.toDegrees(angleOffset);
                System.out.println("New best momentum path: " + currentMomentumDistance + " blocks at " +
                    String.format("%.1f°", bestAngle) + " to " + currentBestStart);
            }
        }

        // Only return a momentum start if we found at least 2 blocks of runway
        if (maxMomentumDistance >= 2) {
            System.out.println("Momentum analysis complete: Found " + maxMomentumDistance + " blocks of runway at " +
                String.format("%.1f°", bestAngle) + " angle");
            System.out.println("Best momentum start: " + bestMomentumStart);
            return bestMomentumStart;
        } else {
            System.out.println("Momentum analysis complete: Insufficient runway (" + maxMomentumDistance + " blocks, need 2+)");
            return null;
        }
    }

    private static boolean hasDirectPath(World world, BlockPos start, BlockPos end) {
        // Check if there's a clear straight path between start and end
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));

        if (steps == 0) return true;

        double stepX = (double) dx / steps;
        double stepZ = (double) dz / steps;

        for (int i = 0; i <= steps; i++) {
            int x = start.getX() + (int) Math.round(i * stepX);
            int z = start.getZ() + (int) Math.round(i * stepZ);

            // Check at the same Y level first
            BlockPos checkPos = new BlockPos(x, start.getY(), z);
            if (isWalkableSurface(world, checkPos)) {
                continue;
            }

            // Try one block up if current level is blocked
            BlockPos upperPos = checkPos.up();
            if (isWalkableSurface(world, upperPos)) {
                continue;
            }

            // Path is blocked
            return false;
        }

        return true;
    }

    public static BlockPos getMomentumStartBlock() {
        return momentumStartBlock;
    }
}
