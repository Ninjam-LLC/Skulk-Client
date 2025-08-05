package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.data.Step;
import com.ariesninja.skulkpk.client.core.JumpPlanner.JumpLogistics;
import com.ariesninja.skulkpk.client.core.physics.Dist;
import com.ariesninja.skulkpk.client.core.physics.Obstructions;
import com.ariesninja.skulkpk.client.core.physics.utils.Ledge;
import com.ariesninja.skulkpk.client.core.utils.ChatMessageUtil;
import com.ariesninja.skulkpk.client.pk.AutoJumpHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class PlayerController {

    // Rotation state variables
    private static int moveTimer = 0;
    private static int rotationTimer = 0;
    private static int sneaktimer = 0;
    private static int backtraceTimer = 0;
    private static int genericTimer = 0;

    private static float startYaw = 0.0f;
    private static float targetYaw = 0.0f;

    // Specific constants
    private static final int ROTATION_DURATION = 5; // ticks (50ms)
    private static final float NEO_ROTATION_OFFSET = 3.2f; // Degrees

    // Step execution state
    private static Step currentStep = null;
    private static JumpLogistics currentLogistics = null;
    private static RoughActionState roughState = null;
    private static GenericActionState genericState = null;

    // Rough action state tracking
    private static class RoughActionState {
        boolean isActive = false;
        int phase = 0; // Different phases for multi-phase actions
        Vec3d targetPosition = null;
        Vec3d nextStepPosition = null;
        boolean rotationComplete = false;
        boolean movementComplete = false;

        // ROUGH_MOMENTUM specific state
        boolean hasEnteredThreshold = false;
        Vec3d lastValidPosition = null;

        // Thresholds for stop conditions
        static final double ROUGH_START_NEARBY_THRESHOLD = 0.5; // blocks
        static final double ROUGH_START_PRECISE_THRESHOLD = 0.1; // blocks
        static final double ROUGH_MOMENTUM_THRESHOLD = 0.4; // blocks from jump pos
        static final double ROUGH_JUMP_THRESHOLD = 1.0; // blocks from target
        static final double ROUGH_JUMP_NOSPRINT_THRESHOLD = 2; // the jump gap to target without sprint
    }

    // Generic action state tracking
    private static class GenericActionState {
        boolean isActive = false;
        int phase = 0;
        Vec3d targetPosition = null;
        Vec3d nextStepPosition = null;
        boolean rotationComplete = false;
        boolean movementComplete = false;

        Vec3d lastValidPosition = null;

        static final double JUMP_THRESHOLD = 0.6;
    }

    /**
     * Executes a step with the given logistics data
     */
    public static void executeStep(Step step, JumpLogistics logistics) {
        currentStep = step;
        currentLogistics = logistics;

        // Add null checks to prevent NullPointerException
        if (step == null) {
            System.err.println("PlayerController: Cannot execute empty step!");
            return;
        }

        if (step == Step.ROUGH_START ||
                step == Step.ROUGH_MOMENTUM ||
                step == Step.ROUGH_JUMP) {

            initializeRoughAction(step);
        } else {
            // Initialize generic action state for other action types
            initializeGenericAction(step);
        }


        System.out.println("PlayerController: Executing step - " + step);
    }

    /**
     * Initializes state for rough action types
     */
    private static void initializeRoughAction(Step step) {
        if (roughState == null) {
            roughState = new RoughActionState();
        }

        roughState.isActive = true;
        roughState.phase = 0;
        roughState.rotationComplete = false;
        roughState.movementComplete = false;

        switch (step) {
            case ROUGH_START:
                // Target position is the momentum start position (precise)
                if (currentLogistics != null) {
                    roughState.targetPosition = currentLogistics.getMomentumStartPos().add(0, -0.45, 0);
                    roughState.nextStepPosition = currentLogistics.getJumpPos().add(0, -0.45, 0);
                }
                break;

            case ROUGH_MOMENTUM:
                // Target position is near the jump position (precise)
                if (currentLogistics != null) {
                    roughState.hasEnteredThreshold = false;
                    roughState.targetPosition = currentLogistics.getJumpPos().add(0, -0.45, 0);
                    roughState.nextStepPosition = currentLogistics.getTargetPos();
                }
                break;

            case ROUGH_JUMP:
                // Target position is the target position (precise)
                if (currentLogistics != null) {
                    roughState.targetPosition = currentLogistics.getTargetPos();
                }
                break;
        }
    }

    private static void initializeGenericAction(Step step) {
        if (genericState == null) {
            genericState = new GenericActionState();
        }

        genericState.isActive = true;
        genericState.phase = 0;
        genericState.rotationComplete = false;
        genericState.movementComplete = false;

        switch (step) {
            case UNIT_SAFE_CORNER:
                genericState.targetPosition = currentLogistics.getTargetPos();
                break;
            case NEO_A:
                genericState.targetPosition = currentLogistics.getJumpPos();
                genericState.nextStepPosition = currentLogistics.getTargetPos();
                break;
            default:
                genericState.targetPosition = currentLogistics.getTargetPos();
                break;
        }
    }

    /**
     * Checks if the current step is complete
     */
    public static boolean isStepComplete() {
        if (currentStep == null) return true;

        return switch (currentStep) {
            case ROUGH_START, ROUGH_MOMENTUM, ROUGH_JUMP -> roughState != null && !roughState.isActive;
            default -> genericState != null && !genericState.isActive;
        };
    }

    /**
     * Clears the current step
     */
    public static void clearCurrentStep(MinecraftClient client) {
        currentStep = null;
        currentLogistics = null;
        if (roughState != null) {
            roughState.isActive = false;
        }
        stopAllMovement(client);
    }

    // Call this in a client tick event
    public static void tick(MinecraftClient client) {
        // Handle movement timer
        if (moveTimer > 0) {
            moveTimer--;
            if (moveTimer == 0 && client.options != null) {
                client.options.forwardKey.setPressed(false);
            }
        }

        // Handle rotation timer and smooth rotation
        if (rotationTimer > 0 && client.player != null) {
            rotationTimer--;

            // Calculate interpolation progress (0.0 to 1.0)
            float progress = 1.0f - ((float) rotationTimer / ROTATION_DURATION);

            // Use smooth step for more natural rotation
            float smoothProgress = progress * progress * (3.0f - 2.0f * progress);

            // Handle angle wrapping for interpolation
            float angleDifference = MathHelper.wrapDegrees(targetYaw - startYaw);

            // Interpolate using the wrapped difference
            float currentYaw = startYaw + angleDifference * smoothProgress;

            // Wrap the final result
            currentYaw = MathHelper.wrapDegrees(currentYaw);

            client.player.setYaw(currentYaw);
            client.player.setPitch(client.player.getPitch()); // Maintain current pitch
        }

        // Handle sneak timer
        if (sneaktimer > 0) {
            sneaktimer--;
            if (sneaktimer == 0 && client.options != null) {
                client.options.sneakKey.setPressed(false);
            }
        }

        // Handle backtrace timer
        if (backtraceTimer > 0) {
            backtraceTimer--;
            if (backtraceTimer == 0 && client.options != null) {
                client.options.backKey.setPressed(false);
            }
        }

        // Handle rough action execution
        if (roughState != null && roughState.isActive && currentStep != null && client.player != null) {
            handleRoughAction(client);
        }

        // Handle generic action execution
        if (genericState != null && genericState.isActive && currentStep != null && client.player != null) {
            handleGenericAction(client);
        }
    }

    /**
     * Handles the execution of rough action types
     */
    private static void handleRoughAction(MinecraftClient client) {
        if (client.player == null) return;
        Vec3d playerPos = client.player.getPos();

        switch (currentStep) {
            case ROUGH_START:
                handleRoughStart(client, playerPos);
                break;
            case ROUGH_MOMENTUM:
                handleRoughMomentum(client, playerPos);
                break;
            case ROUGH_JUMP:
                handleRoughJump(client, playerPos);
                break;
        }
    }

    private static void handleGenericAction(MinecraftClient client) {
        if (client.player == null) return;

        switch (currentStep) {
            case UNIT_SAFE_CORNER:
                handleCorner(client, false);
                break;
            case UNIT_SAFE_CORNER_BACK:
                handleCorner(client, true);
                break;
            case NEO_A:
                handleNeoA(client);
                break;

            default:
                // Other actions can be handled here
                break;
        }
    }

    /**
     * ROUGH_START: Point camera toward momentum start, move forward until nearby, then shift move to precise position
     */
    private static void handleRoughStart(MinecraftClient client, Vec3d playerPos) {
        if (roughState.targetPosition == null) {
            roughState.isActive = false;
            return;
        }

        System.out.println("Phase: " + roughState.phase);

        switch (roughState.phase) {
            case 0: // Point camera toward target
                if (!roughState.rotationComplete) {
                    Vec3d direction = roughState.targetPosition.subtract(playerPos).normalize();
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

                    if (client.player == null) return;
                    // Normalize current yaw to the range [-180, 180]
                    float currentYaw = MathHelper.wrapDegrees(client.player.getYaw());

                    // Calculate the shortest difference, handling wrap-around
                    float yawDifference = MathHelper.wrapDegrees(targetYaw - currentYaw);

                    // Check if we're close enough to the target rotation
                    if (Math.abs(yawDifference) < 3.0f) {
                        roughState.rotationComplete = true;
                        roughState.phase = 1;
                        rotationTimer = 0; // Stop rotation
                    } else {
                        // Set target yaw for smooth rotation
                        if (rotationTimer == 0) {
                            startYaw = currentYaw;
                            PlayerController.targetYaw = targetYaw;
                            rotationTimer = ROTATION_DURATION;
                            roughState.rotationComplete = false; // Ensure we are not yet complete
                        }
                    }
                }
                break;

            case 1: // Move forward until nearby
                double distanceToTarget = playerPos.distanceTo(roughState.targetPosition);

                System.out.println("Distance to target: " + distanceToTarget);

                if (distanceToTarget > RoughActionState.ROUGH_START_NEARBY_THRESHOLD) {
                    client.options.forwardKey.setPressed(true);
                } else {
                    client.options.forwardKey.setPressed(false);
                    roughState.phase = 2;
                }
                break;

            case 2: // Shift move to precise position
                double preciseDistance = playerPos.distanceTo(roughState.targetPosition);

                client.options.sneakKey.setPressed(true);

                if (preciseDistance > RoughActionState.ROUGH_START_PRECISE_THRESHOLD) {
                    // Use slower, more precise movement
                    Vec3d direction = roughState.targetPosition.subtract(playerPos).normalize();

                    float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                    if (client.player != null) {
                        client.player.setYaw(targetYaw);
                    }
                    client.options.forwardKey.setPressed(true);
                } else {
                    // Stop all movement - we've reached the precise position
                    stopAllMovement(client);
                    roughState.isActive = false;
                    System.out.println("PlayerController: ROUGH_START completed");
                }
                break;
        }
    }

    /**
     * ROUGH_MOMENTUM: Sprint in direction of momentum line until either:
     * A) We reach the last point we can stand on the block before falling (precise edge)
     * B) We enter and then exit the threshold area around the jump position
     */
    private static void handleRoughMomentum(MinecraftClient client, Vec3d playerPos) {
        if (roughState.targetPosition == null) {
            roughState.isActive = false;
            return;
        }

        double distanceToJumpPos = playerPos.distanceTo(roughState.targetPosition);
        double distanceToNextStep = playerPos.distanceTo(roughState.nextStepPosition);

        double jumpGap = Dist.realDistanceWithHeight(currentLogistics.getJumpBlockPos(), currentLogistics.getTargetBlockPos());

        // Track if we've entered the threshold zone
        if (!roughState.hasEnteredThreshold && distanceToJumpPos <= RoughActionState.ROUGH_MOMENTUM_THRESHOLD) {
            roughState.hasEnteredThreshold = true;
            System.out.println("Entered momentum threshold zone");
        }

        // Check if we're at the edge of a block (about to fall)
        boolean atBlockEdge = Ledge.shouldAutoJump(0.001);
        if (client.player == null) return;
        boolean atBlockEdgeLegacy = AutoJumpHelper.INSTANCE.shouldAutoJump(client.player, client);

        System.out.println("At block edge (legacy): " + atBlockEdgeLegacy +
                ", At block edge (new): " + atBlockEdge);

        // Update last valid position if we're still on solid ground
        if (!atBlockEdge) {
            roughState.lastValidPosition = playerPos;
        }

        System.out.println("Distance to jump pos: " + distanceToJumpPos +
                ", Has entered threshold: " + roughState.hasEnteredThreshold +
                ", At block edge: " + atBlockEdge);

        // We're at the edge of a block and about to fall
        if (atBlockEdge && roughState.lastValidPosition != null && roughState.hasEnteredThreshold) {
            roughState.isActive = false;
            System.out.println("PlayerController: ROUGH_MOMENTUM completed - reached block edge");
        }

        if (!roughState.isActive) {
            Vec3d directionLanding = roughState.nextStepPosition.subtract(playerPos).normalize();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-directionLanding.x, directionLanding.z));
            client.player.setYaw(targetYaw);
            // Sprint forward if the total horizontal distance to the target is more than 2 blocks
            if (jumpGap > RoughActionState.ROUGH_JUMP_NOSPRINT_THRESHOLD) {
                System.out.println("RUNNING TO NEXT STEP: " + distanceToNextStep);
                client.options.sprintKey.setPressed(true);
            } else {
                client.options.sprintKey.setPressed(false);
            }
            client.options.jumpKey.setPressed(true);
            return;
        }

        // Continue moving toward jump position
        Vec3d direction = roughState.targetPosition.subtract(playerPos).normalize();

        // If we have entered the threshold, we need to rotate toward the next step position
        if (roughState.hasEnteredThreshold && roughState.nextStepPosition != null) {
            direction = roughState.nextStepPosition.subtract(playerPos).normalize();
        }

        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        if (client.player == null) return;
        // Normalize current yaw to the range [-180, 180]
        float currentYaw = MathHelper.wrapDegrees(client.player.getYaw());

        // Calculate the shortest difference, handling wrap-around
        float yawDifference = MathHelper.wrapDegrees(targetYaw - currentYaw);

        // Check if we're close enough to the target rotation
        if (Math.abs(yawDifference) < 5.0f) {
            roughState.rotationComplete = true;
            roughState.phase = 1;
            rotationTimer = 0; // Stop rotation
        } else {
            // Set target yaw for smooth rotation
            if (rotationTimer == 0) {
                startYaw = currentYaw;
                PlayerController.targetYaw = targetYaw;
                rotationTimer = ROTATION_DURATION;
                roughState.rotationComplete = false; // Ensure we are not yet complete
            }
        }

        if (Math.abs(yawDifference) > 10.0f && !roughState.hasEnteredThreshold) {
            client.options.forwardKey.setPressed(false);
            return;
        }
        if (jumpGap > RoughActionState.ROUGH_JUMP_NOSPRINT_THRESHOLD) {
            System.out.println("RUNNING TO NEXT STEP: " + distanceToNextStep);
            client.options.sprintKey.setPressed(true);
        } else {
            client.options.sprintKey.setPressed(false);
        }
        client.options.forwardKey.setPressed(true);
    }

    /**
     * ROUGH_JUMP: Jump while sprinting toward target block
     */
    private static void handleRoughJump(MinecraftClient client, Vec3d playerPos) {
        if (roughState.targetPosition == null) {
            roughState.isActive = false;
            return;
        }

        double distanceToTarget = playerPos.distanceTo(roughState.targetPosition);
        // Get jump gap from jump block and target block
        double jumpGap = Dist.realDistanceWithHeight(currentLogistics.getJumpBlockPos(), currentLogistics.getTargetBlockPos());

        if (distanceToTarget > RoughActionState.ROUGH_JUMP_THRESHOLD) {
            // Point toward target and sprint
            Vec3d direction = roughState.targetPosition.subtract(playerPos).normalize();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

            // Set rotation (immediate for jump phase)
            if (client.player != null) {
                client.player.setYaw(targetYaw);
            }

            // Move forward
            client.options.forwardKey.setPressed(true);

            // Sprint if the total horizontal distance is more than 2 blocks
            client.options.sprintKey.setPressed(jumpGap > RoughActionState.ROUGH_JUMP_NOSPRINT_THRESHOLD);

            // If the bottom half of our hitbox is within a ladder block, hold shift
            if (client.player != null && client.player.isClimbing()) {
                client.options.sneakKey.setPressed(true);
                client.options.jumpKey.setPressed(false);
            } else {
                client.options.sneakKey.setPressed(false);
            }
        } else {
            // We've reached the target area - stop everything
            stopAllMovement(client);
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(true);
            sneaktimer = 4; // 4 ticks of sneak
            if (client.player != null && client.player.jumping) {
                backtraceTimer = 4; // 8 ticks of backtrace
                client.options.backKey.setPressed(true);
            } else {
                backtraceTimer = 0; // No backtrace if not on ground
            }
            roughState.isActive = false;
            System.out.println("PlayerController: ROUGH_JUMP completed");
        }
    }

    private static void handleCorner(MinecraftClient client, boolean isBack) {
        ChatMessageUtil.sendInfo(client, String.valueOf(genericState.phase));
        switch (genericState.phase) {
            case 0: // Point camera toward target (using BLOCK level position, so rotation is an interval of 90 degrees)
                ChatMessageUtil.sendInfo(client, "asd");
                if (!genericState.rotationComplete) {
                    if (client.player == null) return;
                    Vec3d direction = genericState.targetPosition.subtract(client.player.getPos()).normalize();
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

                    // Round rotation to nearest 90 degrees
                    float roundedYaw = Math.round(targetYaw / 90.0f) * 90.0f;

                    // Normalize current yaw to the range [-180, 180]
                    float currentYaw = MathHelper.wrapDegrees(client.player.getYaw());

                    // Calculate the shortest difference, handling wrap-around
                    float yawDifference = MathHelper.wrapDegrees(roundedYaw - currentYaw);

                    // Check if we're close enough to the target rotation
                    if (Math.abs(yawDifference) < 0.1f) {
                        genericState.rotationComplete = true;
                        rotationTimer = 0; // Stop rotation
                         genericState.phase = 1; // Move to next phase if needed
                    } else {
                        // Set target yaw for smooth rotation
                        if (rotationTimer == 0) {
                            startYaw = currentYaw;
                            System.out.println("DEBUG: Starting rotation from " + startYaw + " to " + roundedYaw);
                            PlayerController.targetYaw = roundedYaw;
                            rotationTimer = ROTATION_DURATION;
                            genericState.rotationComplete = false; // Ensure we are not yet complete
                        }
                    }
                }
                break;
            case 1:
                if (client.player == null) return;
                Vec3d playerPos = client.player.getPos();
                if (playerPos != genericState.lastValidPosition) {
//                    // Point toward target
//                    Vec3d direction = genericState.targetPosition.subtract(playerPos).normalize();
//                    float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
//                    client.player.setYaw(targetYaw);

                    // Move forward
                    if (isBack) {
                        client.options.backKey.setPressed(true);
                        client.options.leftKey.setPressed(true);
                    } else {
                        client.options.forwardKey.setPressed(true);
                        client.options.rightKey.setPressed(true);
                    }
                    client.options.sneakKey.setPressed(true);
                } else {
                    // We've reached the corner - stop just forward and right movement
                    client.options.forwardKey.setPressed(false);
                    client.options.rightKey.setPressed(false);
                    client.options.backKey.setPressed(false);
                    client.options.leftKey.setPressed(false);
                    if (isBack) {
//                        genericState.isActive = false; // Complete the action
//                        genericState.phase = 0; // Reset phase for next use
                        return;
                    }
                    genericTimer = 0;
                    genericState.phase = 2;
                }
                genericState.lastValidPosition = playerPos; // Update last valid position
                break;
            case 2:
                // Move backward and right for some ticks, and do a linear approach to the target rotation
                if (genericTimer == 0) {
                    genericTimer = 6;
                } else {
                    genericTimer--;
                    client.options.backKey.setPressed(true);
                    client.options.rightKey.setPressed(true);
                    // Smoothly rotate toward the target yaw (NEO_ROTATION_OFFSET)
                    if (client.player == null) return;
                    float currentYaw = client.player.getYaw() % 360;
                    float targetYaw = currentYaw + (NEO_ROTATION_OFFSET / 6.0f);
                    client.player.setYaw(targetYaw);
                    if (genericTimer == 0) {
                        client.options.backKey.setPressed(false);
                        client.options.rightKey.setPressed(false);
                        genericState.phase = 3;
                    }
                }
                break;
            case 3:
                // Wait before unshifting and completing the action
                if (genericTimer == 0) {
                    genericTimer = 3;
                } else {
                    genericTimer--;
                    if (genericTimer == 0) {
                        client.options.sneakKey.setPressed(false); // Unshift
                        genericState.isActive = false; // Complete the action
                        genericState.phase = 0; // Reset phase for next use
                        genericState.targetPosition = null; // Clear target position
                        System.out.println("PlayerController: UNIT_SAFE_CORNER completed");
                    }
                }
        }
    }

    private static void handleNeoA(MinecraftClient client) {
        if (genericState.targetPosition == null) {
            genericState.isActive = false;
            return;
        }
        Vec3d playerPos = client.player.getPos();
        BlockPos startBlock = BlockPos.ofFloored(genericState.targetPosition);
        BlockPos endBlock = BlockPos.ofFloored(genericState.nextStepPosition);
        // Check whether jump is X facing or Z facing
        boolean isXFacing = Math.abs(startBlock.getX() - endBlock.getX()) > Math.abs(startBlock.getZ() - endBlock.getZ());
        switch (genericState.phase) {
            case 0:
                // Run and jump forward (no rotate)
                client.options.forwardKey.setPressed(true);
                client.options.sprintKey.setPressed(true);
                if (genericTimer == 0) {
                    genericTimer = 1; // Run for 1 tick
                } else {
                    genericTimer--;
                    client.options.jumpKey.setPressed(true);
                }
                // Run until our X or Z (whichever is the direction of the jump) is beyond the clamped position of the players hitbox (.7 for positive, .3 for negative) on the jump block
                if (isXFacing) {
                    // X facing jump
                    if (playerPos.getX() > startBlock.getX() + 0.7 || playerPos.getX() < startBlock.getX() + 0.3) {
                        genericState.phase = 1; // Move to next phase
                    }
                } else {
                    // Z facing jump
                    if (playerPos.getZ() > startBlock.getZ() + 0.7 || playerPos.getZ() < startBlock.getZ() + 0.3) {
                        genericState.phase = 1; // Move to next phase
                    }
                }
                break;
            case 1:
                // Stop the jump key
                client.options.jumpKey.setPressed(false);
                // Round current camera angle to nearest 90 degrees (easy way to get back to "forward" facing)
                float currentYaw = client.player.getYaw() % 360;
                float roundedYaw = Math.round(currentYaw / 90.0f) * 90.0f;
                client.player.setYaw(roundedYaw);
                // Continue straight until our X or Z (whichever is the direction of the jump) is beyond the clamped position of the players hitbox (.7 for positive, .3 for negative) on the BLOCK AFTER the last prop
                BlockPos lastProp = Obstructions.getBlockAfterLastProp(client, currentLogistics);
                System.out.println("Last prop position: " + lastProp);
                if (isXFacing) {
                    // X facing jump
                    if (lastProp != null && (playerPos.getX() < lastProp.getX() + 1.8 && playerPos.getX() > lastProp.getX() - 0.5)) {
                        System.out.println("Moving to next phase after X facing jump. Player X: " + playerPos.getX() + ", Last prop X: " + lastProp.getX());
                        genericState.phase = 2; // Move to next phase
                    }
                } else {
                    // Z facing jump
                    if (lastProp != null && (playerPos.getZ() < lastProp.getZ() + 1.8 && playerPos.getZ() > lastProp.getZ() - 0.5)) {
                        System.out.println("Moving to next phase after Z facing jump. Player Z: " + playerPos.getZ() + ", Last prop Z: " + lastProp.getZ());
                        genericState.phase = 2; // Move to next phase
                    }
                }
                break;
            case 2:
                double distanceToTarget = playerPos.distanceTo(genericState.nextStepPosition);
                if (distanceToTarget > GenericActionState.JUMP_THRESHOLD) {
                    // Point toward target and sprint
                    Vec3d direction = genericState.nextStepPosition.subtract(playerPos).normalize();
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

                    // Set rotation (immediate for jump phase)
                    client.player.setYaw(targetYaw);
                } else {
                    // We've reached the target area - stop everything
                    stopAllMovement(client);
                    client.options.sneakKey.setPressed(true);
                    sneaktimer = 4; // 4 ticks of sneak
                    if (client.player.jumping) {
                        backtraceTimer = 4; // 8 ticks of backtrace
                        client.options.backKey.setPressed(true);
                    } else {
                        backtraceTimer = 0; // No backtrace if not on ground
                    }
                    genericState.isActive = false;
                    System.out.println("PlayerController: ROUGH_JUMP completed");
                }
                break;
        }
    }

    /**
     * Stops all movement keys
     */
    public static void stopAllMovement(MinecraftClient client) {
        if (client.options != null) {
            client.options.forwardKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.sprintKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
        }
    }
}
