/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.ui.scenes.ScenePicture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class EditorAttentionTest {
    private val attention = EditorAttention()

    @Test
    fun theEventShownIsKeptWithItsTypeUntilNoneIs() {
        attention.showEvent(ManagerTypeDefinition(name = "Score"), 1, EventType.KEY_PRESSED)
        assertEquals(AttentionChip.Event("Score", isManager = true, index = 1, type = EventType.KEY_PRESSED), attention.shownEvent)
        attention.showEvent(ActorTypeDefinition(name = "Ball"), 0, EventType.EVERY_FRAME)
        assertEquals(AttentionChip.Event("Ball", isManager = false, index = 0, type = EventType.EVERY_FRAME), attention.shownEvent)
        attention.showEvent(ActorTypeDefinition(name = "Ball"), null, null)
        assertNull(attention.shownEvent)
    }

    @Test
    fun aTypeEditorClosingAfterTheNextOneOpenedLeavesTheNextOnesEvent() {
        attention.showEvent(ActorTypeDefinition(name = "Paddle"), 0, EventType.KEY_PRESSED)
        attention.showEvent(ActorTypeDefinition(name = "Ball"), null, null)
        assertEquals("Paddle", attention.shownEvent?.typeName)
        attention.showEvent(ActorTypeDefinition(name = "Paddle"), null, null)
        assertNull(attention.shownEvent)
    }

    @Test
    fun aScenePictureWithdrawnAfterAnotherWasProvidedLeavesTheOther() {
        val first = ScenePicture { null }
        val second = ScenePicture { null }
        attention.provide(first)
        attention.provide(second)
        attention.withdraw(first)
        assertSame(second, attention.scenePicture)
        attention.withdraw(second)
        assertNull(attention.scenePicture)
    }
}
