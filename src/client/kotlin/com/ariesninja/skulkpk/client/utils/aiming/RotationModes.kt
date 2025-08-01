/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package com.ariesninja.skulkpk.client.utils.aiming

import com.ariesninja.skulkpk.client.features.module.ModuleProvider
import com.ariesninja.skulkpk.client.utils.aiming.data.Rotation
import com.ariesninja.skulkpk.client.utils.kotlin.Priority
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * Base class for rotation modes - simplified without config dependencies
 */
abstract class RotationMode(
    val name: String,
    val module: ModuleProvider,
) {
    var postMove: Boolean = false
    var instant: Boolean = false

    abstract fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit)
}

class NormalRotationMode(
    module: ModuleProvider,
    val priority: Priority = Priority.IMPORTANT_FOR_USAGE_2,
    private val aimAfterInstantAction: Boolean = false
) : RotationMode("Normal", module) {

    var ignoreOpenInventory: Boolean = true

    override fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit) {
        if (instant && isFinished()) {
            onFinished()
            if (aimAfterInstantAction) {
                MinecraftClient.getInstance().execute {
                    // Simplified rotation target setting - would need actual RotationsConfigurable
                    RotationManager.setRotationTarget(
                        RotationTarget.simple(rotation),
                        priority,
                        module
                    )
                }
            }
            return
        }

        MinecraftClient.getInstance().execute {
            RotationManager.setRotationTarget(
                RotationTarget.simple(rotation),
                priority,
                module
            )
        }
    }
}

class NoRotationMode(module: ModuleProvider) : RotationMode("None", module) {

    var send: Boolean = false

    override fun rotate(rotation: Rotation, isFinished: () -> Boolean, onFinished: () -> Unit) {
        val task = {
            if (send) {
                val fixedRotation = rotation.normalize()
                val mc = MinecraftClient.getInstance()
                val player = mc.player
                if (player != null) {
                    mc.networkHandler?.sendPacket(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            fixedRotation.yaw, fixedRotation.pitch, player.isOnGround,
                            player.horizontalCollision
                        )
                    )
                }
            }
            onFinished()
        }

        if (instant) {
            task()
            return
        }

        PostRotationExecutor.addTask(module, postMove, task)
    }
}
