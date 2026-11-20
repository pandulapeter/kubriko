/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SceneAddActorButtonTest {

    @Test
    fun blockBuiltTypesComeFirstThenThoseWrittenByHandWithoutManagerTypesOrTheSceneSettings() {
        val registered = setOf("Wall", "Ball", "Spawner", "Clouds", "Game", SceneSettingsCodeGenerator.SETTINGS_NAME)
        assertEquals(listOf("Ball", "Wall", "Clouds", "Spawner"), placeableActorTypesOf(registered, listOf("Wall", "Ball"), setOf("Game")))
        assertEquals(listOf("Ball"), placeableActorTypesOf(emptySet(), listOf("Ball"), emptySet()))
    }

    @Test
    fun choosingATypePlacesAnActorOfIt() = uiTest(width = 480, height = 360) {
        val placed = mutableListOf<String>()
        showing { SceneAddActorButton(listOf("Ball", "Spawner"), onPlace = { placed += it }) }
        onNodeWithContentDescription("Add an Actor").performClick()
        waitForIdle()
        saveSnapshot("scene-add-actor")
        onNodeWithText("Spawner").performClick()
        waitForIdle()
        assertEquals(listOf("Spawner"), placed)
    }
}
