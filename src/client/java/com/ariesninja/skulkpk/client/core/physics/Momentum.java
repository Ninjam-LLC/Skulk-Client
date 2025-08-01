package com.ariesninja.skulkpk.client.core.physics;

import com.ariesninja.skulkpk.client.core.JumpPlanner.JumpLogistics;
import com.ariesninja.skulkpk.client.core.physics.utils.SimWrapper;
import com.ariesninja.skulkpk.client.utils.entity.SimulatedPlayer;
import com.ariesninja.skulkpk.client.utils.entity.SimulatedPlayer.SimulatedPlayerInput;
import com.ariesninja.skulkpk.client.utils.entity.SimulatedPlayerCache;
import com.ariesninja.skulkpk.client.utils.movement.DirectionalInput;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;

public class Momentum {

    /**
     * Checks if advanced momentum calculation is required for the jump
     * Currently returns false for all cases as a placeholder
     */
    public static boolean requiresAdvancedMomentum(JumpLogistics logistics) {
        // TODO: Implement actual physics-based momentum checking
        return false;
    }

    public static boolean requiresAnyMomentum(PlayerEntity player, JumpLogistics logistics) {
        // Create a simulated player at the jump position
        SimulatedPlayer DAC = SimWrapper.createSimulatedPlayer(player, logistics.getJumpPos());
        // Set the simulated player's yaw and pitch to face the target position
        Vec3d direction = logistics.getTargetPos().subtract(logistics.getJumpPos()).normalize();
        DAC.setYaw((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        DAC.setPitch(0.0f); // Set pitch to 0 for horizontal jumps
        System.out.println("Simulated player yaw: " + DAC.getYaw() + ", pitch: " + DAC.getPitch());
        // Create a simulated input for the player to move forward
        SimulatedPlayerInput input = SimWrapper.SPI_FWD_S();
        // Set the simulated player's input
        DAC.setInput(input);
        // Rotate the simulated player to face toward the target position
        // Get the sim cache
        SimulatedPlayerCache DACCache = new SimulatedPlayerCache(DAC);
        // Tick the simulated player until they are expected to fall off the block on the next tick
        int ticks = 0;
        DACCache.simulate();
        while (DACCache.getSnapshotAt(4).getOnGround() && ticks < 500) {
            ticks++;
            DAC.tick();
        }
        if (ticks >= 500) {
            System.out.println("Momentum jump failed (stuck): " + DAC.getPos());
        }
        System.out.println("Simulated player position before jump: " + DAC.getPos());
        // Jump the simulated player
        DAC.jump();
        DAC.tick();
        // Tick the player until they land, then check if they are on the target block
        ticks = 0;
        while (!DAC.getOnGround() && ticks < 500) {
            ticks++;
            DAC.tick();
            if (DAC.getPos().y < logistics.getJumpPos().y - 0.5) {
                // If the player falls below the jump position, they cannot make the jump
                System.out.println("Momentum jump failed (low): " + DAC.getPos());
                System.out.println("Target position: " + logistics.getTargetPos());
                return true;
            }
            if (DAC.getPos().distanceTo(logistics.getTargetPos()) < 0.5) {
                // If the player lands on the target position, they can make the jump
                System.out.println("Momentum jump successful: " + DAC.getPos());
                System.out.println("Target position: " + logistics.getTargetPos());
                return false;
            }
        }
        // If the player lands but is not on the target position, they cannot make the jump
        System.out.println("Momentum jump failed: " + DAC.getPos());
        System.out.println("Target position: " + logistics.getTargetPos());
        return true;
    }
}
