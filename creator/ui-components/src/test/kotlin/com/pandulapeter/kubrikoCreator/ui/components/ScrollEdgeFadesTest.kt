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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScrollEdgeFadesTest {

    @Test
    fun onlyTheEdgeWithMoreBeyondItFades() = uiTest(width = 100, height = 200) {
        val scroll = androidx.compose.foundation.ScrollState(0)
        showing {
            Box(Modifier.testTag("list").size(100.dp, 200.dp).scrollEdgeFades(scroll).verticalScroll(scroll)) {
                Box(Modifier.fillMaxWidth().height(1000.dp).background(Color.White))
            }
        }
        waitForIdle()
        fun brightnessAt(isTop: Boolean) = onNodeWithTag("list").captureToImage().toPixelMap().let { it[it.width / 2, if (isTop) 1 else it.height - 2].red }
        val top = brightnessAt(isTop = true)
        val bottom = brightnessAt(isTop = false)
        saveSnapshot("scroll-edge-fades-top")
        assertTrue(top > 0.9f && bottom < 0.5f, "At the top: top $top, bottom $bottom.")
        runBlocking { scroll.scrollTo(scroll.maxValue) }
        waitForIdle()
        val topAtEnd = brightnessAt(isTop = true)
        val bottomAtEnd = brightnessAt(isTop = false)
        saveSnapshot("scroll-edge-fades-end")
        assertTrue(topAtEnd < 0.5f && bottomAtEnd > 0.9f, "At the end: top $topAtEnd, bottom $bottomAtEnd.")
    }
}
