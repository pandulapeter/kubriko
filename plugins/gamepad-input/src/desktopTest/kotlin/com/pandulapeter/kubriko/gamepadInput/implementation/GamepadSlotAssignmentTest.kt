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

import com.pandulapeter.kubriko.gamepadInput.implementation.GamepadSlotAssignment.Companion.NO_INSTANCE
import kotlin.test.Test
import kotlin.test.assertContentEquals

class GamepadSlotAssignmentTest {

    private val assignment = GamepadSlotAssignment(slotCount = 2)

    private fun reconcile(vararg ids: Int) = assignment.reconcile(ids, ids.size)

    @Test
    fun secondPadKeepsItsSlotWhenTheFirstIsUnplugged() {
        reconcile(10, 11)
        assertContentEquals(intArrayOf(10, 11), assignment.instanceIds)
        reconcile(11)
        assertContentEquals(intArrayOf(NO_INSTANCE, 11), assignment.instanceIds)
    }

    @Test
    fun newPadTakesTheLowestFreeSlot() {
        reconcile(10, 11)
        reconcile(11)
        reconcile(11, 12)
        assertContentEquals(intArrayOf(12, 11), assignment.instanceIds)
    }

    @Test
    fun padReplacedInOnePollTakesTheFreedSlot() {
        reconcile(10)
        reconcile(12)
        assertContentEquals(intArrayOf(12, NO_INSTANCE), assignment.instanceIds)
    }

    @Test
    fun idsBeyondTheSlotCountAreIgnored() {
        reconcile(10, 11, 12)
        assertContentEquals(intArrayOf(10, 11), assignment.instanceIds)
    }

    @Test
    fun reconcilingTheSameIdsTwiceChangesNothing() {
        reconcile(11, 10)
        reconcile(11, 10)
        assertContentEquals(intArrayOf(11, 10), assignment.instanceIds)
    }

    @Test
    fun onlyTheFirstConnectedCountEntriesAreRead() {
        assignment.reconcile(intArrayOf(10, 11, 12), connectedCount = 1)
        assertContentEquals(intArrayOf(10, NO_INSTANCE), assignment.instanceIds)
    }
}
