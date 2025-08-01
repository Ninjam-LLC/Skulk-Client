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
package com.ariesninja.skulkpk.client.features.module

/**
 * Minimal module implementation focused on rotation/aiming system needs.
 * Removed all bloat like configs, scripts, notifications, etc.
 */
open class ClientModule(
    override val name: String,
    private var enabled: Boolean = false
) : ModuleProvider {

    /**
     * Whether this module is currently running and should be considered active
     */
    override val running: Boolean
        get() = enabled

    /**
     * Enable the module
     */
    open fun enable() {
        enabled = true
    }

    /**
     * Disable the module
     */
    open fun disable() {
        enabled = false
    }

    /**
     * Toggle the module state
     */
    fun toggle() {
        if (enabled) {
            disable()
        } else {
            enable()
        }
    }

    override fun toString(): String = "Module$name"
}
