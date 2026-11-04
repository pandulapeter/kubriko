/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FixedViewportLayoutTest {

    private class Overlay : Manager() {
        @Composable
        override fun Composable(windowInsets: WindowInsets) = Box(Modifier.fillMaxSize().testTag(TAG))
    }

    @Test
    fun aFixedViewportLetterboxesAndShowsManagerUiWhateverItsModifierSizes() = listOf(Modifier, Modifier.fillMaxSize()).forEach { modifier ->
        runDesktopComposeUiTest(width = 800, height = 400) {
            val kubriko = Kubriko.newInstance(
                ViewportManager.newInstance(aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(ratio = 0.5f, width = 100f.sceneUnit)),
                Overlay(),
            )
            try {
                setContent { KubrikoViewport(modifier = modifier, kubriko = kubriko) }
                waitForIdle()
                val bounds = onNodeWithTag(TAG).fetchSemanticsNode().boundsInRoot
                assertEquals(listOf(300f, 0f, 500f, 400f), listOf(bounds.left, bounds.top, bounds.right, bounds.bottom), "Under $modifier")
            } finally {
                kubriko.dispose()
            }
        }
    }

    private companion object {
        const val TAG = "overlay"
    }
}
