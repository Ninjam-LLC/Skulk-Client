package com.ariesninja.skulkpk.client.core;

import net.minecraft.client.MinecraftClient;

public class PlayerController {

    // Rotation state variables
    private static int rotationTimer = 0;
    private static float startYaw = 0.0f;
    private static float targetYaw = 0.0f;
    private static final int ROTATION_DURATION = 20; // 20 ticks (1 second at 20 TPS)

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
    }
}