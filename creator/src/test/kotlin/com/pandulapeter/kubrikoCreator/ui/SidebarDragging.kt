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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput

/**
 * Drags the sidebar row showing [item] onto the one showing [target] with the mouse, as the user would.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.dragOnto(item: String, target: String) {
    val from = onNodeWithText(item).fetchSemanticsNode().boundsInRoot
    val to = onNodeWithText(target).fetchSemanticsNode().boundsInRoot.center
    onNodeWithText(item).performMouseInput {
        moveTo(center)
        press()
        moveBy(Offset(0f, 30f))
        moveTo(to - from.topLeft)
        release()
    }
    waitForIdle()
}
