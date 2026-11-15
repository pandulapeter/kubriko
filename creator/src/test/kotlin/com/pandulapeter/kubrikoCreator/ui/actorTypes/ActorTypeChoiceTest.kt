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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ActorTypeChoiceTest {

    @Test
    fun anActorTypeReferenceCanBeSetBackToNone() = uiTest {
        var chosen by mutableStateOf("Enemy")
        showing { ActorTypeChoice(chosen, listOf("Enemy", "Ball"), isNoneOffered = true) { chosen = it } }
        onNodeWithText("Enemy").performClick()
        onNodeWithText("None").performClick()
        waitForIdle()
        assertEquals("", chosen)
        onNodeWithText("None").assertExists()
    }

    @Test
    fun aBlocksChoiceOffersNoNone() = uiTest {
        showing { ActorTypeChoice("Enemy", listOf("Enemy", "Ball")) {} }
        onNodeWithText("Enemy").performClick()
        onAllNodesWithText("None").assertCountEquals(0)
    }
}
