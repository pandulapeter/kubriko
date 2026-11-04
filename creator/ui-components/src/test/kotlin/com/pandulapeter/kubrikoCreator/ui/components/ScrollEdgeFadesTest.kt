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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollEdgeFadesTest {

    @Test
    fun atTheTopOnlyTheBottomFades() = showingAList(ScrollState(0)) { top, bottom ->
        assertTrue(top > 0.9f && bottom < 0.5f, "At the top: top $top, bottom $bottom.")
    }

    @Test
    fun atTheEndOnlyTheTopFades() = showingAList(ScrollState(Int.MAX_VALUE)) { top, bottom ->
        assertTrue(top < 0.5f && bottom > 0.9f, "At the end: top $top, bottom $bottom.")
    }

    /**
     * Shows a white list taller than its box, scrolled as [scroll] starts, and hands [check] how bright its top and bottom
     * rows of pixels are.
     */
    private fun showingAList(scroll: ScrollState, check: (top: Float, bottom: Float) -> Unit) = uiTest(width = 100, height = 200) {
        showing {
            Box(Modifier.testTag("list").size(100.dp, 200.dp).scrollEdgeFades(scroll).verticalScroll(scroll)) {
                Box(Modifier.fillMaxWidth().height(1000.dp).background(Color.White))
            }
        }
        waitForIdle()
        val pixels = onNodeWithTag("list").captureToImage().toPixelMap()
        check(pixels[pixels.width / 2, 1].red, pixels[pixels.width / 2, pixels.height - 2].red)
    }
}
