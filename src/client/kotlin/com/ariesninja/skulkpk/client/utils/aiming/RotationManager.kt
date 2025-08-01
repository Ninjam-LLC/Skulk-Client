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

import com.ariesninja.skulkpk.client.event.EventListener
import com.ariesninja.skulkpk.client.event.EventManager
import com.ariesninja.skulkpk.client.event.events.GameTickEvent
import com.ariesninja.skulkpk.client.event.events.PlayerVelocityStrafe
import com.ariesninja.skulkpk.client.event.events.RotationUpdateEvent
import com.ariesninja.skulkpk.client.event.handler
import com.ariesninja.skulkpk.client.features.module.ModuleProvider
import com.ariesninja.skulkpk.client.utils.aiming.data.Rotation
import com.ariesninja.skulkpk.client.utils.aiming.features.MovementCorrection
import com.ariesninja.skulkpk.client.utils.aiming.utils.setRotation
import com.ariesninja.skulkpk.client.utils.aiming.utils.withFixedYaw
import com.ariesninja.skulkpk.client.utils.client.*
import com.ariesninja.skulkpk.client.utils.entity.lastRotation
import com.ariesninja.skulkpk.client.utils.entity.rotation
import com.ariesninja.skulkpk.client.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import com.ariesninja.skulkpk.client.utils.kotlin.EventPriorityConvention.MODEL_STATE
import com.ariesninja.skulkpk.client.utils.kotlin.Priority
import com.ariesninja.skulkpk.client.utils.kotlin.RequestHandler
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * A rotation manager
 */
object RotationManager : EventListener {

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     */
    private val rotationTarget
        get() = rotationTargetHandler.getActiveRequestValue()
    private var rotationTargetHandler = RequestHandler<RotationTarget>()

    val activeRotationTarget: RotationTarget?
        get() = rotationTarget ?: previousRotationTarget
    internal var previousRotationTarget: RotationTarget? = null

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentRotation: Rotation? = null
        set(value) {
            previousRotation = if (value == null) {
                null
            } else {
                field ?: mc.player?.rotation ?: Rotation.ZERO
            }

            field = value
        }

    // Used for rotation interpolation
    var previousRotation: Rotation? = null

    private val fakeLagging
        get() = false

    val serverRotation: Rotation
        get() = if (fakeLagging) theoreticalServerRotation else actualServerRotation

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerRotation = Rotation.ZERO
        private set

    private var theoreticalServerRotation = Rotation.ZERO

    fun setRotationTarget(plan: RotationTarget, priority: Priority, provider: ModuleProvider) {
        if (!allowedToUpdate()) {
            return
        }

        rotationTargetHandler.request(
            RequestHandler.Request(
                if (plan.movementCorrection == MovementCorrection.CHANGE_LOOK) 1 else plan.ticksUntilReset,
                priority.priority,
                provider,
                plan
            )
        )
    }

    /**
     * Checks if the rotation is allowed to be updated
     */
    fun isRotatingAllowed(rotationTarget: RotationTarget): Boolean {
        return allowedToUpdate()
    }

    /**
     * Update current rotation to a new rotation step
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    fun update() {
        val activeRotationTarget = this.activeRotationTarget ?: return
        val playerRotation = player.rotation

        val rotationTarget = this.rotationTarget

        // Prevents any rotation changes when inventory is opened
        if (isRotatingAllowed(activeRotationTarget)) {
            val fromRotation = currentRotation ?: playerRotation
            val rotation = activeRotationTarget.towards(fromRotation, rotationTarget == null)
                // After generating the next rotation, we need to normalize it
                .normalize()

            val diff = rotation.angleTo(playerRotation)

            if (rotationTarget == null && (activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK
                    || activeRotationTarget.processors.isEmpty()
                    || diff <= activeRotationTarget.resetThreshold)) {
                currentRotation?.let { currentRotation ->
                    player.yaw = player.withFixedYaw(currentRotation)
                    player.renderYaw = player.yaw
                    player.lastRenderYaw = player.yaw
                }

                currentRotation = null
                previousRotationTarget = null
            } else {
                if (activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK) {
                    player.setRotation(rotation)
                }

                currentRotation = rotation
                previousRotationTarget = activeRotationTarget

                rotationTarget?.whenReached?.invoke()
            }
        }

        // Update reset ticks
        rotationTargetHandler.tick()
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun allowedToUpdate() = true

    fun rotationMatchesPreviousRotation(): Boolean {
        val player = mc.player ?: return false

        currentRotation?.let {
            return it == previousRotation
        }

        return player.rotation == player.lastRotation
    }

    @Suppress("unused")
    private val velocityHandler = handler<PlayerVelocityStrafe>(priority = MODEL_STATE) { event ->
        if (activeRotationTarget?.movementCorrection != MovementCorrection.OFF) {
            val rotation = currentRotation ?: return@handler

            event.velocity = Entity.movementInputToVelocity(
                event.movementInput,
                event.speed,
                rotation.yaw
            )
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        EventManager.callEvent(RotationUpdateEvent)
        update()
    }

    override val running: Boolean
        get() = inGame

}
