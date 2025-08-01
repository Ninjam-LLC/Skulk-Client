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

import com.ariesninja.skulkpk.client.utils.aiming.data.Rotation
import com.ariesninja.skulkpk.client.utils.aiming.features.MovementCorrection
import com.ariesninja.skulkpk.client.utils.client.RestrictedSingleUseAction

/**
 * Simplified rotation target for the minimal rotation system
 */
data class RotationTarget(
    val rotation: Rotation,
    val movementCorrection: MovementCorrection = MovementCorrection.OFF,
    val ticksUntilReset: Int = 1,
    val resetThreshold: Float = 1.0f,
    val processors: List<Any> = emptyList(), // Simplified - no actual processors
    val whenReached: RestrictedSingleUseAction? = null
) {
    fun towards(fromRotation: Rotation, isFinished: Boolean): Rotation {
        // Simplified rotation interpolation - just return target rotation
        return rotation
    }

    companion object {
        fun simple(rotation: Rotation): RotationTarget {
            return RotationTarget(rotation)
        }
    }
}
