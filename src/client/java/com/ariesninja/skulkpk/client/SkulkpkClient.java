package com.ariesninja.skulkpk.client;

import com.ariesninja.skulkpk.client.core.*;
import com.ariesninja.skulkpk.client.core.rendering.SelectionRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class SkulkpkClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Keybinds.register();
        SelectionRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
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
                    String prefix = "Skulk";
                    String arrow = " > ";
                    String message = "Nothing to clear";

                    net.minecraft.text.Text prefixText = net.minecraft.text.Text.literal(prefix).formatted(net.minecraft.util.Formatting.AQUA, net.minecraft.util.Formatting.BOLD);
                    net.minecraft.text.Text arrowText = net.minecraft.text.Text.literal(arrow).formatted(net.minecraft.util.Formatting.GRAY);
                    net.minecraft.text.Text messageText = net.minecraft.text.Text.literal(message).formatted(net.minecraft.util.Formatting.GRAY);

                    net.minecraft.text.Text fullMessage = net.minecraft.text.Text.empty().append(prefixText).append(arrowText).append(messageText);
                    client.player.sendMessage(fullMessage, false);
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

        // Call tick methods for ongoing execution
        StepExecutor.getInstance().tick(client);
        PlayerController.tick(client);
    }
}
