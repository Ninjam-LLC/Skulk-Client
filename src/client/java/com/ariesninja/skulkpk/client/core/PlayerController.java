package com.ariesninja.skulkpk.client.core;

import com.ariesninja.skulkpk.client.core.data.Step;
import com.ariesninja.skulkpk.client.core.JumpPlanner.JumpLogistics;
import com.ariesninja.skulkpk.client.core.physics.utils.Ledge;
import com.ariesninja.skulkpk.client.pk.AutoJumpHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PlayerController {

    // Rotation state variables
    private static int rotationTimer = 0;
    private static float startYaw = 0.0f;
    private static float targetYaw = 0.0f;
    private static final int ROTATION_DURATION = 20; // 20 ticks (1 second at 20 TPS)

    // Step execution state
    private static Step currentStep = null;
    private static JumpLogistics currentLogistics = null;
    private static RoughActionState roughState = null;

    // Rough action state tracking
    private static class RoughActionState {
        boolean isActive = false;
        int phase = 0; // Different phases for multi-phase actions
        Vec3d targetPosition = null;
        float targetYaw = 0.0f;
        boolean rotationComplete = false;
        boolean movementComplete = false;

        // ROUGH_MOMENTUM specific state
        boolean hasEnteredThreshold = false;
        Vec3d lastValidPosition = null;

        // Thresholds for stop conditions
        static final double ROUGH_START_NEARBY_THRESHOLD = 0.5; // blocks
        static final double ROUGH_START_PRECISE_THRESHOLD = 0.1; // blocks
        static final double ROUGH_MOMENTUM_THRESHOLD = 0.3; // blocks from jump pos
        static final double ROUGH_JUMP_THRESHOLD = 1.0; // blocks from target
        static final double EDGE_DETECTION_THRESHOLD = 0.1; // blocks from block edge
    }

    // Test method to rotate user (with smooth lerp over time)
    public static void cvmRotatePlayer(MinecraftClient client) {
        if (client.player != null && rotationTimer == 0) {
            startYaw = client.player.getYaw();
            targetYaw = startYaw + 90.0f;
            rotationTimer = ROTATION_DURATION;
        }
    }

    private static int moveTimer = 0;

    // Test method to move user (lerp required for movement) using keybinds and relevant delays
    public static void cvmMovePlayer(MinecraftClient client) {
        if (client.options != null) {
            client.options.forwardKey.setPressed(true);
            moveTimer = 8; // Move for 8 ticks (400ms at 20 TPS)
        }
    }

    /**
     * Executes a step with the given logistics data
     */
    public static void executeStep(Step step, JumpLogistics logistics) {
        currentStep = step;
        currentLogistics = logistics;

        if (step.getAction() == Step.ActionType.ROUGH_START ||
            step.getAction() == Step.ActionType.ROUGH_MOMENTUM ||
            step.getAction() == Step.ActionType.ROUGH_JUMP) {

            initializeRoughAction(step.getAction());
        }

        System.out.println("PlayerController: Executing step - " + step.getAction());
    }

    /**
     * Initializes state for rough action types
     */
    private static void initializeRoughAction(Step.ActionType actionType) {
        if (roughState == null) {
            roughState = new RoughActionState();
        }

        roughState.isActive = true;
        roughState.phase = 0;
        roughState.rotationComplete = false;
        roughState.movementComplete = false;

        switch (actionType) {
            case ROUGH_START:
                // Target position is the momentum start position (precise)
                if (currentLogistics != null) {
                    roughState.targetPosition = currentLogistics.getMomentumStartPos().add(0, -0.45, 0);
                }
                break;

            case ROUGH_MOMENTUM:
                // Target position is near the jump position (precise)
                if (currentLogistics != null) {
                    roughState.hasEnteredThreshold = false;
                    roughState.targetPosition = currentLogistics.getJumpPos().add(0, -0.45, 0);
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

    /**
     * Checks if the current step is complete
     */
    public static boolean isStepComplete() {
        if (currentStep == null) return true;

        switch (currentStep.getAction()) {
            case ROUGH_START:
            case ROUGH_MOMENTUM:
            case ROUGH_JUMP:
                return roughState != null && !roughState.isActive;
            default:
                return true; // Other action types complete immediately for now
        }
    }

    /**
     * Clears the current step
     */
    public static void clearCurrentStep() {
        currentStep = null;
        currentLogistics = null;
        if (roughState != null) {
            roughState.isActive = false;
        }
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

            // Interpolate between start and target yaw
            float currentYaw = startYaw + (targetYaw - startYaw) * smoothProgress;

            client.player.setYaw(currentYaw);
            client.player.setPitch(client.player.getPitch()); // Maintain current pitch
        }

        // Handle rough action execution
        if (roughState != null && roughState.isActive && currentStep != null && client.player != null) {
            handleRoughAction(client);
        }
    }

    /**
     * Handles the execution of rough action types
     */
    private static void handleRoughAction(MinecraftClient client) {
        Vec3d playerPos = client.player.getPos();

        switch (currentStep.getAction()) {
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

                    if (rotationTimer == 0) {
                        // Start the rotation
                        startYaw = client.player.getYaw();
                        PlayerController.targetYaw = targetYaw;
                        rotationTimer = ROTATION_DURATION;
                    }

                    // Check if we're close enough to the target rotation
                    float currentYaw = client.player.getYaw() % 360;
                    float yawDifference = Math.abs(currentYaw - targetYaw);
                    // Handle yaw wrap-around (360 degrees)
                    if (yawDifference > 180) {
                        yawDifference = 360 - yawDifference;
                    }

                    System.out.println("Current Yaw: " + currentYaw + ", Target Yaw: " + targetYaw + ", Difference: " + yawDifference);

                    if (Math.abs(yawDifference) < 3.0f) {
                        roughState.rotationComplete = true;
                        roughState.phase = 1;
                        rotationTimer = 0; // Stop rotation
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
                    client.player.setYaw(targetYaw);
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

        // Track if we've entered the threshold zone
        if (!roughState.hasEnteredThreshold && distanceToJumpPos <= RoughActionState.ROUGH_MOMENTUM_THRESHOLD) {
            roughState.hasEnteredThreshold = true;
            System.out.println("Entered momentum threshold zone");
        }

        // Check if we're at the edge of a block (about to fall)
        boolean atBlockEdgeLegacy = Ledge.shouldAutoJump(0.001);
        boolean atBlockEdge = AutoJumpHelper.INSTANCE.shouldAutoJump(client.player, client);

        System.out.println("At block edge (legacy): " + atBlockEdgeLegacy +
                           ", At block edge (new): " + atBlockEdge);

        // Update last valid position if we're still on solid ground
        if (!atBlockEdge) {
            roughState.lastValidPosition = playerPos;
        }

        System.out.println("Distance to jump pos: " + distanceToJumpPos +
                         ", Has entered threshold: " + roughState.hasEnteredThreshold +
                         ", At block edge: " + atBlockEdge);

        // Stop conditions:
        // A) We're at the edge of a block and about to fall
        if (atBlockEdge && roughState.lastValidPosition != null) {
            roughState.isActive = false;
            System.out.println("PlayerController: ROUGH_MOMENTUM completed - reached block edge");
            return;
        }

        // B) We've entered the threshold and are now exiting it (moving away)
        if (roughState.hasEnteredThreshold && distanceToJumpPos > RoughActionState.ROUGH_MOMENTUM_THRESHOLD && false) {
            roughState.isActive = false;
            System.out.println("PlayerController: ROUGH_MOMENTUM completed - exited threshold zone");
            return;
        }

        // Continue moving toward jump position
        Vec3d direction = roughState.targetPosition.subtract(playerPos).normalize();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // Set rotation if we aren't too close
        if (distanceToJumpPos > RoughActionState.ROUGH_MOMENTUM_THRESHOLD * 2) {
            if (!roughState.rotationComplete) {
                startYaw = client.player.getYaw();
                PlayerController.targetYaw = targetYaw;
                rotationTimer = ROTATION_DURATION;
                roughState.rotationComplete = true;
            }
        }

        // Don't move until we are almost at the right rotation
        float currentYaw = client.player.getYaw() % 360;
        float yawDifference = Math.abs(currentYaw - targetYaw);
        // Handle yaw wrap-around (360 degrees)
        if (yawDifference > 180) {
            yawDifference = 360 - yawDifference;
        }

        if (yawDifference > 20.0f) {
            return;
        }

        // Sprint forward
        client.options.sprintKey.setPressed(true);
        client.options.forwardKey.setPressed(true);
    }

    /**
     * Checks if the player is at the edge of a block and about to fall
     */


    /**
     * ROUGH_JUMP: Jump while sprinting toward target block
     */
    private static void handleRoughJump(MinecraftClient client, Vec3d playerPos) {
        if (roughState.targetPosition == null) {
            roughState.isActive = false;
            return;
        }

        double distanceToTarget = playerPos.distanceTo(roughState.targetPosition);

        if (distanceToTarget > RoughActionState.ROUGH_JUMP_THRESHOLD) {
            // Point toward target and sprint
            Vec3d direction = roughState.targetPosition.subtract(playerPos).normalize();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

            // Set rotation (immediate for jump phase)
            client.player.setYaw(targetYaw);

            // Sprint and jump
            client.options.sprintKey.setPressed(true);
            client.options.forwardKey.setPressed(true);
            client.options.jumpKey.setPressed(true);
        } else {
            // We've reached the target area - stop everything
            stopAllMovement(client);
            client.options.jumpKey.setPressed(false);
            roughState.isActive = false;
            System.out.println("PlayerController: ROUGH_JUMP completed");
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
