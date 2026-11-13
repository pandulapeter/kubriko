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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetMovement
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MovementColumnTest {

    @Test
    fun stayingInsideTheSceneIsSwitchedAndSaysWhatItMeans() = uiTest {
        var definition by mutableStateOf(ActorTypeDefinition(name = "Enemy"))
        val changes = mutableListOf<SetMovement.Input>()
        showing {
            MovementColumn(
                definition = definition,
                onChange = { changes += it },
            )
        }
        onNodeWithText("Moves on past the edges. When it leaves the Scene can remove it.").assertExists()
        onNode(isToggleable() and hasText("Stays inside the Scene")).performClick()
        assertEquals(listOf(SetMovement.Input("Enemy", staysInsideScene = true)), changes)
        definition = definition.copy(staysInsideScene = true)
        waitForIdle()
        onNodeWithText("Pushed back whenever it reaches past an edge.").assertExists()
        saveSnapshot("movement-stays-inside")
    }
}
