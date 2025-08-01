/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package com.ariesninja.skulkpk.client.utils.aiming.preference

import com.ariesninja.skulkpk.client.utils.aiming.RotationManager
import com.ariesninja.skulkpk.client.utils.aiming.data.Rotation
import com.ariesninja.skulkpk.client.utils.client.player
import com.ariesninja.skulkpk.client.utils.entity.rotation
import com.ariesninja.skulkpk.client.utils.math.geometry.Line
import com.ariesninja.skulkpk.client.utils.math.minus
import com.ariesninja.skulkpk.client.utils.math.plus
import com.ariesninja.skulkpk.client.utils.math.sq
import com.ariesninja.skulkpk.client.utils.math.times
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

class LeastDifferencePreference(
    private val baseRotation: Rotation,
    private val basePoint: Vec3d? = null
) : RotationPreference {

    override fun getPreferredSpot(eyesPos: Vec3d, range: Double): Vec3d {
        if (basePoint != null) {
            return basePoint
        }

        return eyesPos + baseRotation.directionVector * range
    }

    override fun getPreferredSpotOnBox(box: Box, eyesPos: Vec3d, range: Double): Vec3d {
        if (basePoint != null) {
            return basePoint
        }

        val preferredSpot = getPreferredSpot(eyesPos, range)
        if (box.contains(preferredSpot)) {
            return preferredSpot
        }

        val look = Line(eyesPos, preferredSpot - eyesPos)
        return look.getPointOnBoxInDirection(box)
            ?.takeIf { it.squaredDistanceTo(eyesPos) <= range.sq() }
            ?: preferredSpot
    }

    override fun compare(o1: Rotation, o2: Rotation): Int {
        val rotationDifferenceO1 = baseRotation.angleTo(o1)
        val rotationDifferenceO2 = baseRotation.angleTo(o2)

        return rotationDifferenceO1.compareTo(rotationDifferenceO2)
    }

    companion object {

        val LEAST_DISTANCE_TO_CURRENT_ROTATION: LeastDifferencePreference
            get() = LeastDifferencePreference(RotationManager.currentRotation ?: player.rotation)

        fun leastDifferenceToLastPoint(eyes: Vec3d, point: Vec3d): LeastDifferencePreference {
            return LeastDifferencePreference(Rotation.lookingAt(point, from = eyes), point)
        }

    }

}
