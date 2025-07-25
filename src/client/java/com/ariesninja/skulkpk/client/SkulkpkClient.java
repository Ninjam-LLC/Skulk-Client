package com.ariesninja.skulkpk.client;

import com.ariesninja.skulkpk.client.core.ActionQueue;
import com.ariesninja.skulkpk.client.core.BlockSelector;
import com.ariesninja.skulkpk.client.core.JumpAnalyzer;
import com.ariesninja.skulkpk.client.core.JumpPlanner;
import com.ariesninja.skulkpk.client.core.Keybinds;
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
            ActionQueue.addActions(JumpPlanner.planJump(BlockSelector.getSelectedBlock()));
        }
    }
}
