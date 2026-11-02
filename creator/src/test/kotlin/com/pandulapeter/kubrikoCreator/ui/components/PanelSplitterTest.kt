/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel
import com.pandulapeter.kubrikoCreator.ui.saveSnapshot
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PanelSplitterTest {
    private var widths by mutableStateOf(emptyMap<String, Float>())
    private var changes = 0
    private var resets = 0

    @Test
    fun draggingResizesThePanelSavingOnceAndDoubleClickingGivesItsDefaultWidthBack() = uiTest(width = 800, height = 400) {
        showing {
            val layout = remember(widths) {
                PanelLayout(
                    widths = widths,
                    onWidthChange = { panel, width ->
                        changes++
                        widths = widths + (panel.id to width.value)
                    },
                    onWidthReset = { panel ->
                        resets++
                        widths = widths - panel.id
                    },
                )
            }
            BoxWithConstraints {
                val row = PanelRow(maxWidth, panelsInRow = 1)
                val width = rememberPanelWidth(Panel.SIDEBAR, layout)
                Row {
                    Box(Modifier.testTag("panel").sidePanel(width.widthIn(row)))
                    PanelSplitter(width, row)
                }
            }
        }
        val defaultWidth = panelWidth()
        onNodeWithContentDescription("Drag to resize, double-click for the default width").performTouchInput {
            swipeRight(startX = centerX, endX = centerX + 80f, durationMillis = 200)
        }
        waitForIdle()
        assertTrue(panelWidth() > defaultWidth, "The panel is ${panelWidth()} wide, not wider than its default $defaultWidth")
        assertEquals(1, changes)
        assertEquals(setOf("sidebar"), widths.keys)
        saveSnapshot("panel-splitter-dragged")
        onNodeWithContentDescription("Drag to resize, double-click for the default width").performMouseInput { doubleClick() }
        waitForIdle()
        assertEquals(defaultWidth, panelWidth())
        assertEquals(1, resets)
        assertEquals(emptyMap(), widths)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.panelWidth(): Dp = onNodeWithTag("panel").getBoundsInRoot().let { it.right - it.left }
}
