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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun aClearableFieldShowsItsClearButtonOnlyWhileItHoldsTextAndEmptiesIt() = uiTest {
        var value by mutableStateOf("goblin")
        showing {
            CompactTextField(value = value, onValueChange = { value = it }, modifier = Modifier.width(200.dp), isClearable = true)
        }
        saveSnapshot("compact-text-field-clearable")
        onNodeWithContentDescription("Clear").performClick()
        waitForIdle()
        assertEquals("", value)
        onNodeWithContentDescription("Clear").assertDoesNotExist()
    }

    @Test
    fun aClearableFieldKeepsTheRoomOfItsHiddenClearButtonSoTheTextNeverRunsUnderIt() = uiTest {
        val placeholder = "A placeholder far too long to fit in the field"
        showing {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactTextField(value = "", onValueChange = {}, modifier = Modifier.width(200.dp), placeholder = placeholder, isClearable = true)
                CompactTextField(value = "goblin", onValueChange = {}, modifier = Modifier.width(200.dp), isClearable = true)
                CompactTextField(value = "", onValueChange = {}, modifier = Modifier.width(200.dp), placeholder = placeholder)
            }
        }
        val placeholders = onAllNodesWithText(placeholder, useUnmergedTree = true)
        val clearableRoom = placeholders[0].getBoundsInRoot()
        val plainRoom = placeholders[1].getBoundsInRoot()
        assertTrue(plainRoom.width > clearableRoom.width, "The hidden clear button keeps no room")
        assertTrue(onNodeWithContentDescription("Clear").getBoundsInRoot().left >= clearableRoom.right, "The clear button overlaps the text")
    }

    @Test
    fun aFieldThatIsNotClearableHasNoClearButton() = uiTest {
        showing {
            CompactTextField(value = "goblin", onValueChange = {})
        }
        onNodeWithContentDescription("Clear").assertDoesNotExist()
    }

    private fun ComposeUiTest.widthOf(tag: String) = onNodeWithTag(tag).getBoundsInRoot().width

    private fun ComposeUiTest.roomLeftBy(tag: String, label: String) = widthOf(tag) - onNodeWithText(label, useUnmergedTree = true).getBoundsInRoot().width

    private fun assertAbout(expected: Dp, actual: Dp) = assertTrue(abs((expected - actual).value) <= 1f, "$actual isn't about $expected")
}
