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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NewSpriteDialogTest {

    @Test
    fun aSpriteIsMadeOfItsNameAndSizeOrSaysWhyItCantBe() = uiTest(width = 600, height = 400) {
        val created = mutableListOf<Triple<String, Int, Int>>()
        var isDismissed = false
        showing {
            NewSpriteDialog(
                onCreate = { name, width, height ->
                    created += Triple(name, width, height)
                    if (name == "ball") Rejection(CommandProblem.RESOURCE_TAKEN, "drawable/ball.png") else null
                },
                onDismiss = { isDismissed = true },
            )
        }
        onNode(hasSetTextAction() and hasText("Name")).performTextReplacement("ball")
        onNodeWithText("Create").performClick()
        onNodeWithText("drawable/ball.png", substring = true).assertExists()
        saveSnapshot("new-sprite-dialog")
        onNode(hasSetTextAction() and hasText("ball")).performTextReplacement("ship ")
        onNodeWithText("Create").performClick()
        assertEquals(listOf(Triple("ball", 32, 32), Triple("ship", 32, 32)), created)
        assertTrue(isDismissed)
    }

    @Test
    fun frameFilesSortByTheirNumbersAndNameTheSpriteWithout() {
        val files = listOf("walk-10.png", "walk-2.png", "walk-1.png").map(::File)
        assertEquals(listOf("walk-1.png", "walk-2.png", "walk-10.png"), files.sortedWith(compareBy(::framesOrderOf)).map { it.name })
        assertEquals("walk", frameSpriteNameOf(listOf(File("walk_01.png"))))
        assertEquals("42", frameSpriteNameOf(listOf(File("42.png"))))
    }
}
