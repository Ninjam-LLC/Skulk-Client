package com.ariesninja.skulkpk.client.core.physics.utils;

import com.ariesninja.skulkpk.client.utils.entity.SimulatedPlayer;
import com.ariesninja.skulkpk.client.utils.movement.DirectionalInput;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;

public class SimWrapper {

    public static SimulatedPlayer createSimulatedPlayer(PlayerEntity player, Vec3d startPos) {
        SimulatedPlayer.SimulatedPlayerInput customInput = new SimulatedPlayer.SimulatedPlayerInput(
                new DirectionalInput(
                        false,
                        false,
                        false,
                        false
                ),
                false,
                false,
                false
        );
        SimulatedPlayer sim = new SimulatedPlayer(
                player,
                customInput,
                startPos,
                Vec3d.ZERO,
                player.dimensions.getBoxAt(startPos),
                player.getYaw(),
                player.getPitch(),
                false,
                0.0f,
                0,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                new Object2DoubleArrayMap<TagKey<Fluid>>(),
                new HashSet<TagKey<Fluid>>()
        );
        return sim;
    }

    public static SimulatedPlayer.SimulatedPlayerInput SPI_FWD() {
        return new SimulatedPlayer.SimulatedPlayerInput(
                new DirectionalInput(
                        true, // forward
                        false, // back
                        false, // left
                        false  // right
                ),
                false, // jump
                false, // sprint
                false  // sneak
        );
    }

    public static SimulatedPlayer.SimulatedPlayerInput SPI_FWD_S() {
        return new SimulatedPlayer.SimulatedPlayerInput(
                new DirectionalInput(
                        true, // forward
                        false, // back
                        false, // left
                        false  // right
                ),
                false, // jump
                true, // sprint
                false  // sneak
        );
    }

}
