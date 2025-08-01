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
 *
 */

package com.ariesninja.skulkpk.client.event.events

import com.ariesninja.skulkpk.client.event.CancellableEvent
import com.ariesninja.skulkpk.client.event.Event
//import com.ariesninja.skulkpk.client.render.engine.type.Color4b
import com.ariesninja.skulkpk.client.utils.client.Nameable
//import com.ariesninja.skulkpk.client.utils.combat.EntityTargetClassification
//import com.ariesninja.skulkpk.client.utils.combat.EntityTargetingInfo
import com.ariesninja.skulkpk.client.utils.kotlin.Priority
import com.ariesninja.skulkpk.client.utils.kotlin.PriorityField
import net.minecraft.entity.Entity

@Nameable("attack")
class AttackEntityEvent(
    val entity: Entity,
    val caller: () -> Unit
) : CancellableEvent()

@Nameable("entityMargin")
class EntityMarginEvent(val entity: Entity, var margin: Float) : Event()

//@Nameable("tagEntityEvent")
//class TagEntityEvent(val entity: Entity, var targetingInfo: EntityTargetingInfo) : Event() {
//    val color: PriorityField<Color4b?> = PriorityField(null, Priority.NOT_IMPORTANT)
//
//    /**
//     * Don't start combat this target
//     */
//    fun dontTarget() {
//        if (this.targetingInfo.classification == EntityTargetClassification.TARGET) {
//            this.targetingInfo = this.targetingInfo.copy(classification = EntityTargetClassification.INTERESTING)
//        }
//    }
//
//    /**
//     * Fully ignore that target
//     */
//    fun ignore() {
//        this.targetingInfo = targetingInfo.copy(classification = EntityTargetClassification.IGNORED)
//    }
//
//    fun assumeFriend() {
//        this.targetingInfo = targetingInfo.copy(isFriend = true)
//    }
//
//    fun color(col: Color4b, priority: Priority) {
//        this.color.trySet(col, priority)
//    }
//}
