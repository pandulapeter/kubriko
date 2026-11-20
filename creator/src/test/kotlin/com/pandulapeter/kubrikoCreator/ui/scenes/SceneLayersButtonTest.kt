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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneLayerFilter
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SceneLayersButtonTest {

    @Test
    fun eachLayerIsHiddenAndLockedFromItsOwnMenuAndEveryOneComesBackAtOnce() = uiTest(width = 480, height = 360) {
        var filter by mutableStateOf(SceneLayerFilter())
        showing { SceneLayersButton(layerIndices = listOf(null, 0, 2), filter = filter, onFilterChange = { filter = it }) }
        onNodeWithContentDescription("Layers").performClick()
        waitForIdle()
        onNodeWithText("Without a layer").assertExists()
        onNodeWithText("Show and unlock every layer").assertDoesNotExist()
        onNodeWithText("Layer 2").performClick()
        waitForIdle()
        onNodeWithText("Shown").performClick()
        waitForIdle()
        assertEquals(SceneLayerFilter(hidden = setOf(2)), filter)
        onNodeWithContentDescription("Layers").performClick()
        waitForIdle()
        onNodeWithText("Without a layer").performClick()
        waitForIdle()
        onNodeWithText("Shown").assertDoesNotExist()
        onNodeWithText("Locked").performClick()
        waitForIdle()
        assertEquals(SceneLayerFilter(hidden = setOf(2), locked = setOf(null)), filter)
        onNodeWithContentDescription("Layers").performClick()
        waitForIdle()
        onNodeWithText("Hidden").assertExists()
        saveSnapshot("scene-layers-menu")
        onNodeWithText("Show and unlock every layer").performClick()
        waitForIdle()
        assertEquals(SceneLayerFilter(), filter)
    }
}
