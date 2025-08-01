package com.ariesninja.skulkpk.client.pk

import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import com.ariesninja.skulkpk.client.utils.entity.PlayerSimulationCache.getSimulationForLocalPlayer

object AutoJumpHelper {

    /**
     * Returns true if the player should auto-jump at the edge of a block.
     */
    fun shouldAutoJump(player: PlayerEntity, mc: MinecraftClient): Boolean {
        val simulatedPlayer = getSimulationForLocalPlayer()

        return !simulatedPlayer.getSnapshotAt(2).onGround
    }
}
