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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CompactTextFieldTest {

    @Test
    fun aFieldOfAGivenTextWidthGrowsWithItsLabelSoTheTextKeepsItsRoom() = uiTest {
        showing {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTextField(modifier = Modifier.testTag("short"), value = "1000", onValueChange = {}, label = "X", textWidth = 56.dp)
                CompactTextField(modifier = Modifier.testTag("long"), value = "1000", onValueChange = {}, label = "A much longer label", textWidth = 56.dp)
            }
        }
        val shortRoom = roomLeftBy("short", "X")
        val longRoom = roomLeftBy("long", "A much longer label")
        assertTrue(widthOf("long") > widthOf("short"), "The field with the longer label isn't wider")
        assertAbout(shortRoom, longRoom)
        saveSnapshot("compact-text-field-text-width")
    }

    @Test
    fun aLongLabelInANarrowFieldEndsInAnEllipsisRatherThanHidingTheText() = uiTest {
        val label = "Duration of the shake, in milliseconds"
        showing {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTextField(modifier = Modifier.testTag("narrow").width(100.dp), value = "300", onValueChange = {}, label = label)
                CompactTextField(modifier = Modifier.testTag("sized"), value = "300", onValueChange = {}, label = "X", textWidth = 40.dp)
            }
        }
        val narrowLabelWidth = onNodeWithText(label, useUnmergedTree = true).getBoundsInRoot().width
        val narrowRoom = widthOf("narrow") - narrowLabelWidth
        val minimumRoom = roomLeftBy("sized", "X")
        assertAbout(minimumRoom, narrowRoom)
        saveSnapshot("compact-text-field-narrow")
    }

    private fun ComposeUiTest.widthOf(tag: String) = onNodeWithTag(tag).getBoundsInRoot().width

    private fun ComposeUiTest.roomLeftBy(tag: String, label: String) = widthOf(tag) - onNodeWithText(label, useUnmergedTree = true).getBoundsInRoot().width

    private fun assertAbout(expected: Dp, actual: Dp) = assertTrue(abs((expected - actual).value) <= 1f, "$actual isn't about $expected")
}
