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

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import com.pandulapeter.kubrikoCreator.template.ProposedFileChange
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FileDiffTest {

    @Test
    fun aChangedFileShowsItsLinesAndACopiedOneOnlyItsName() = uiTest(width = 640, height = 240) {
        showing {
            Column {
                FileDiff(ProposedFileChange(path = "Main.kt", before = "a\n", after = "b\n"))
                FileDiff(ProposedFileChange.copyOf(resource = "fonts/font.ttf", path = "font/font.ttf"))
            }
        }
        onNodeWithText("- a").assertExists()
        onNodeWithText("+ b").assertExists()
        onNodeWithText("font/font.ttf (a new file that isn't text, so it isn't shown)").assertExists()
        saveSnapshot("file-diff")
    }
}
