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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LabeledSwitchTest {

    @Test
    fun aLongLabelWrapsInsteadOfSqueezingTheSwitch() = uiTest(width = 240) {
        showing {
            LabeledSwitch(
                modifier = Modifier.fillMaxWidth(),
                label = "Whether the Actor keeps moving after it leaves the screen and comes back later",
                checked = true,
                onCheckedChange = {},
            )
        }
        val label = onNodeWithText("Whether the Actor keeps moving", substring = true, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(root.right - label.right >= SWITCH_WIDTH_IN_PIXELS, "The label leaves ${root.right - label.right} pixels for the switch")
        saveSnapshot("labeled-switch-long-label")
    }
}

private const val SWITCH_WIDTH_IN_PIXELS = 40f
