/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gamepadInput.implementation

/**
 * Keeps gamepad slots sticky by keying them on SDL joystick instance IDs, which stay stable for the life of a
 * connection, rather than on device indices, which SDL renumbers whenever a device is removed.
 */
internal class GamepadSlotAssignment(slotCount: Int) {

    /** The instance ID owning each slot, or [NO_INSTANCE] for a free one. */
    val instanceIds = IntArray(slotCount) { NO_INSTANCE }

    /**
     * Releases every slot whose owner is not among the first [connectedCount] entries of [connectedIds], then gives
     * each connected ID without a slot the lowest free one, in [connectedIds] order. IDs that find no free slot are
     * ignored.
     */
    fun reconcile(connectedIds: IntArray, connectedCount: Int) {
        for (slot in instanceIds.indices) {
            val instanceId = instanceIds[slot]
            if (instanceId != NO_INSTANCE && !connectedIds.containsWithin(connectedCount, instanceId)) {
                instanceIds[slot] = NO_INSTANCE
            }
        }
        for (index in 0 until connectedCount) {
            val instanceId = connectedIds[index]
            if (slotOf(instanceId) == NO_SLOT) {
                val freeSlot = slotOf(NO_INSTANCE)
                if (freeSlot == NO_SLOT) return
                instanceIds[freeSlot] = instanceId
            }
        }
    }

    /** The slot owned by [instanceId], or [NO_SLOT]. */
    fun slotOf(instanceId: Int): Int {
        for (slot in instanceIds.indices) {
            if (instanceIds[slot] == instanceId) return slot
        }
        return NO_SLOT
    }

    fun clear() = instanceIds.fill(NO_INSTANCE)

    private fun IntArray.containsWithin(count: Int, value: Int): Boolean {
        for (index in 0 until count) {
            if (this[index] == value) return true
        }
        return false
    }

    companion object {
        const val NO_INSTANCE = -1
        const val NO_SLOT = -1
    }
}
