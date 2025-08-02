package com.ariesninja.skulkpk.client;

import com.ariesninja.skulkpk.client.core.*;
import com.ariesninja.skulkpk.client.core.physics.utils.Ledge;
import com.ariesninja.skulkpk.client.core.rendering.SelectionRenderer;
import com.ariesninja.skulkpk.client.license.LicenseInputScreen;
import com.ariesninja.skulkpk.client.license.LicenseManager;
import com.ariesninja.skulkpk.client.license.LicenseVerificationService;
import com.ariesninja.skulkpk.client.pk.AutoJumpHelper;
import com.ariesninja.skulkpk.client.util.ChatMessageUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class SkulkpkClient implements ClientModInitializer {
    private boolean isVerifyingLicense = false;

    @Override
    public void onInitializeClient() {
        Keybinds.register();
        SelectionRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Register server join event to verify license
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            verifyLicenseOnServerJoin(client);
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (Keybinds.SELECT_KEY.wasPressed()) {
            var cameraEntity = client.getCameraEntity();
            if (cameraEntity == null) return;

            var from = cameraEntity.getEyePos();
            var rotation = cameraEntity.getRotationVec(1.0f);
            var to = from.add(rotation.multiply(1000));

            var context = new RaycastContext(
                    from,
                    to,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    cameraEntity
            );
            var hit = client.world.raycast(context);

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockSelector.selectBlock(hit, client);
                // Analysis is already handled inside BlockSelector.selectBlock()
            }
        }

        if (Keybinds.EXECUTE_KEY.wasPressed()) {
            StepExecutor.getInstance().executeSequence(client);
        }

        if (Keybinds.CLEAR_KEY.wasPressed()) {
            // If neither selection nor execution was active, show a general clear message
            if (BlockSelector.getSelectedBlock() == null && !StepExecutor.getInstance().isExecuting()) {
                if (client.player != null) {
                    ChatMessageUtil.sendInfo(client, "Nothing to clear");
                }
            }

            // Clear selection if present
            if (BlockSelector.getSelectedBlock() != null) {
                BlockSelector.clearSelection();
            }

            // Stop execution if active
            if (StepExecutor.getInstance().isExecuting()) {
                StepExecutor.getInstance().stopExecution();
            }
        }

        if (Keybinds.TEST_KEY_1.wasPressed()) {
            // This key can be used for testing purposes, e.g., to trigger a specific action or log information
            PlayerController.cvmRotatePlayer(client);
        }

        if (Keybinds.TEST_KEY_2.wasPressed()) {
            PlayerController.cvmMovePlayer(client);
        }

        // Debug key to log AutoJumpHelper value when held
        if (Keybinds.DEBUG_AUTOJUMP_KEY.isPressed() && client.player != null) {
            boolean state0 = AutoJumpHelper.INSTANCE.shouldAutoJump(client.player, client);
            boolean state1 = Ledge.shouldAutoJump(0.001);
            System.out.println("state0: " + state0 + ", state1: " + state1);
        }

        // Call tick methods for ongoing execution
        StepExecutor.getInstance().tick(client);
        PlayerController.tick(client);
    }

    private void verifyLicenseOnServerJoin(MinecraftClient client) {
        if (isVerifyingLicense) return; // Prevent multiple verification attempts

        String storedLicense = LicenseManager.getStoredLicense();
        if (storedLicense == null) {
            // No license stored, show input screen
            client.execute(() -> client.setScreen(new LicenseInputScreen()));
            return;
        }

        // Verify the stored license with the API
        String username = client.getSession().getUsername();
        isVerifyingLicense = true;

        // Show verification message to player
        if (client.player != null) {
            ChatMessageUtil.sendWarn(client, "Verifying license...");
        }

        LicenseVerificationService.verifyLicense(username, storedLicense)
                .thenAccept(result -> {
                    client.execute(() -> {
                        isVerifyingLicense = false;

                        if (!result.isValid()) {
                            // License is invalid, show input screen with error and pre-filled license
                            if (client.player != null) {
                                ChatMessageUtil.sendError(client, "Stored license is invalid: " + result.getMessage());
                            }

                            // Show license input screen with error and pre-filled data
                            client.setScreen(new LicenseInputScreen("Automatic License Validation Failed", storedLicense));
                        } else {
                            // License is valid, allow continued play
                            if (client.player != null) {
                                ChatMessageUtil.sendSuccess(client, "License verified successfully!");
                            }
                        }
                    });
                })
                .exceptionally(throwable -> {
                    client.execute(() -> {
                        isVerifyingLicense = false;

                        if (client.player != null) {
                            ChatMessageUtil.sendError(client, "License verification error: " + throwable.getMessage());
                        }

                        // Show license input screen with error and pre-filled data for network errors too
                        client.setScreen(new LicenseInputScreen("License Verification Error", storedLicense));
                    });
                    return null;
                });
    }
}
