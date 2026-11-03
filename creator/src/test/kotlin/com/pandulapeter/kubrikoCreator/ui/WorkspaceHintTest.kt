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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WorkspaceHintTest {

    private fun ComposeUiTest.bounds(text: String): Rect = onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    @Test
    fun withRoomTheCardsStandSideBySideAndShrinkTogether() = uiTest(width = 760, height = 700) {
        showing { WorkspaceHint(Modifier.fillMaxSize()) }
        val cast = bounds("The cast")
        val scenes = bounds("The scenes")
        assertEquals(cast.top, scenes.top)
        assertTrue(scenes.left > cast.right)
        saveSnapshot("workspace-hint-side-by-side")
    }

    @Test
    fun withoutRoomTheCardsAreStackedRatherThanOneBeingSqueezed() = uiTest(width = 520, height = 700) {
        showing { WorkspaceHint(Modifier.fillMaxSize()) }
        val cast = bounds("The cast")
        val scenes = bounds("The scenes")
        assertEquals(cast.left, scenes.left)
        assertTrue(scenes.top > cast.bottom)
        saveSnapshot("workspace-hint-stacked")
    }
}
