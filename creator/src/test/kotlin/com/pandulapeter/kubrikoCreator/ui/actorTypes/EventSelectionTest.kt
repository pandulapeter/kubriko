/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventSelectionTest {

    @Test
    fun aTypeWithoutEventsShowsNone() {
        assertNull(selectedEventIndex(keys = emptyList(), selectedKey = 4L, fallbackIndex = 1))
        assertNull(selectedEventIndex(keys = emptyList(), selectedKey = null, fallbackIndex = null))
    }

    @Test
    fun theSelectedEventIsShownWhereverItIs() {
        assertEquals(2, selectedEventIndex(keys = listOf(7L, 3L, 5L), selectedKey = 5L, fallbackIndex = 0))
    }

    @Test
    fun aRemovedSelectedEventFallsBackToItsPlaceClamped() {
        assertEquals(2, selectedEventIndex(keys = listOf(7L, 3L, 5L), selectedKey = 9L, fallbackIndex = 5))
        assertEquals(1, selectedEventIndex(keys = listOf(7L, 3L, 5L), selectedKey = 9L, fallbackIndex = 1))
    }

    @Test
    fun theFirstEventIsShownUntilAnotherIsSelected() {
        assertEquals(0, selectedEventIndex(keys = listOf(7L, 3L, 5L), selectedKey = null, fallbackIndex = null))
    }
}
