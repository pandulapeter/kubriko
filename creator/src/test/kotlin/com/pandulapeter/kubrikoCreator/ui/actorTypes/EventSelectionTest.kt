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
    fun theFirstEventIsShownUntilAnotherIsSelected() {
        assertEquals(0, selectedEventIndex(requestedIndex = null, eventCount = 3))
        assertEquals(2, selectedEventIndex(requestedIndex = 2, eventCount = 3))
    }

    @Test
    fun removingTheLastEventSelectsTheNewLastOne() {
        assertEquals(1, selectedEventIndex(requestedIndex = 2, eventCount = 2))
    }

    @Test
    fun anActorTypeWithoutEventsShowsNone() {
        assertNull(selectedEventIndex(requestedIndex = 0, eventCount = 0))
        assertNull(selectedEventIndex(requestedIndex = null, eventCount = 0))
    }
}
