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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class QuickOpenDialogTest {
    private val sprite = "src/commonMain/composeResources/drawable/brick.png"
    private val items = quickOpenItemsOf(
        blockTypes = listOf(ActorTypeDefinition(name = "Ball"), ActorTypeDefinition(name = "Brick", properties = listOf(PropertyDefinition.Number("hits")))),
        resources = mapOf(ResourceGroup.SPRITES to listOf(ProjectResource(ResourceGroup.SPRITES, File(sprite), sprite))),
        pageNames = listOf("story"),
    )
    private val opened = mutableListOf<QuickOpenTarget>()
    private var dismissals = 0

    @Test
    fun typingNarrowsEverythingAndEnterOpensTheClosestMatch() = uiTest(width = 700, height = 500) {
        showing { QuickOpenDialog(items = items, onOpen = { opened += it }, onDismiss = { dismissals++ }) }
        onNode(hasSetTextAction() and isFocused()).assertExists()
        onNode(hasSetTextAction()).performTextInput("brk")
        onNodeWithText("Brick").assertExists()
        onNodeWithText("brick.png").assertExists()
        onNodeWithText("Ball").assertDoesNotExist()
        onNodeWithText("story").assertDoesNotExist()
        saveSnapshot("quick-open-filtered")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf<QuickOpenTarget>(QuickOpenTarget.File(Mention.ActorType("Brick", ".kubriko/actors/Brick.json"))), opened)
    }

    @Test
    fun theArrowsMoveTheLitRowAroundTheList() = uiTest(width = 700, height = 500) {
        showing { QuickOpenDialog(items = items, onOpen = { opened += it }, onDismiss = { dismissals++ }) }
        onNode(hasSetTextAction()).performTextInput("brk")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.DirectionDown) }
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.DirectionUp) }
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.DirectionUp) }
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("brick.png", "hits"), opened.map { target -> items.single { it.target == target }.title })
    }

    @Test
    fun aClickOpensAPropertysTypeAndEscapeCloses() = uiTest(width = 700, height = 500) {
        showing { QuickOpenDialog(items = items, onOpen = { opened += it }, onDismiss = { dismissals++ }) }
        onNode(hasSetTextAction()).performTextInput("hits")
        onNodeWithText("Property of Brick").assertExists()
        saveSnapshot("quick-open-property")
        onNodeWithText("Property of Brick").performClick()
        assertEquals(listOf<QuickOpenTarget>(QuickOpenTarget.Property("Brick", isOfManagerType = false, name = "hits")), opened)
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, dismissals)
    }

    @Test
    fun nothingMatchingSaysSo() = uiTest(width = 700, height = 500) {
        showing { QuickOpenDialog(items = items, onOpen = { opened += it }, onDismiss = { dismissals++ }) }
        onNode(hasSetTextAction()).performTextInput("zzz")
        onNodeWithText("Nothing matches.").assertExists()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        assertTrue(opened.isEmpty())
    }
}
