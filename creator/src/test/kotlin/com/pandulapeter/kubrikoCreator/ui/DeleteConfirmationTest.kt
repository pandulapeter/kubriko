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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class DeleteConfirmationTest {
    private val brick = RelationSubject.Type("Brick")
    private val adds = Usage(UsagePlace.InModel(RelationPlace.Type("Ball", 0, 2)), RelationKind.ADDS, brick, UsageEffect.BLOCK_REMOVED)
    private val places = Usage(UsagePlace.InModel(RelationPlace.Scene("level-1")), RelationKind.PLACES, brick, UsageEffect.ACTORS_REMOVED, count = 40)
    private val kotlin = Usage(UsagePlace.KotlinFile("src/commonMain/kotlin/Game.kt"), RelationKind.NAMES_IN_KOTLIN, brick, UsageEffect.KOTLIN_TO_CHANGE)

    @Test
    fun everyUsageIsListedWithWhatHappensToItAndLeadsToItsPlace() = uiTest(width = 720, height = 520) {
        val selected = mutableListOf<Usage>()
        var isConfirmed = false
        showing {
            DeleteConfirmation(
                subject = brick,
                usages = listOf(adds, places, kotlin),
                onConfirm = { isConfirmed = true },
                onUsageSelected = { selected += it },
                onDismiss = {},
            )
        }
        onNodeWithText("Delete Brick?").assertExists()
        onNodeWithText("Ball, Event 1, Block 3 · Adds Brick").assertExists()
        onNodeWithText("Scene level-1 · Places Brick (40)").assertExists()
        onNodeWithText("Left as it is: the build fails until you change it").assertExists()
        saveSnapshot("delete-confirmation")
        onNodeWithText("Ball, Event 1, Block 3 · Adds Brick").performClick()
        onNodeWithText("src/commonMain/kotlin/Game.kt · Names Brick in Kotlin").performClick()
        assertEquals(listOf(adds, kotlin), selected, "a hand-written Kotlin file opens too, in the system's app")
        onNodeWithText("Delete").assertIsEnabled().performClick()
        assertEquals(true, isConfirmed)
    }

    @Test
    fun deletingWaitsForTheUsagesAndSaysWhenThereAreNone() = uiTest(width = 720, height = 360) {
        var usages: List<Usage>? by mutableStateOf(null)
        showing { DeleteConfirmation(subject = RelationSubject.Property("Ball", "speed"), usages = usages, onConfirm = {}, onUsageSelected = {}, onDismiss = {}) }
        onNodeWithText("Remove the Property speed?").assertExists()
        onNodeWithText("Looking for what uses it…").assertExists()
        onNodeWithText("Delete").assertIsNotEnabled()
        usages = emptyList()
        waitForIdle()
        onNodeWithText("Nothing else uses it. You can undo this.").assertExists()
        onNodeWithText("Delete").assertIsEnabled()
        saveSnapshot("delete-confirmation-unused")
    }

    @Test
    fun whatKeepsItFromBeingDeletedIsShownAndDeletingIsRefused() = uiTest(width = 720, height = 360) {
        var isConfirmed = false
        val first = Usage(UsagePlace.GameSettings, RelationKind.STARTS_WITH, RelationSubject.Scene("menu"), UsageEffect.PREVENTS_DELETING)
        showing { DeleteConfirmation(subject = RelationSubject.Scene("menu"), usages = listOf(first), onConfirm = { isConfirmed = true }, onUsageSelected = {}, onDismiss = {}) }
        onNodeWithText("Delete the Scene menu?").assertExists()
        onNodeWithText("It can't be deleted while this is so.").assertExists()
        onNodeWithText("Project settings · Starts with Scene menu").assertExists()
        onNodeWithText("Delete").assertIsNotEnabled().performClick()
        assertFalse(isConfirmed)
        saveSnapshot("delete-confirmation-refused")
    }

    @Test
    fun usagesThatCouldntBeReadAreSaidAndDeletingStillGoesAhead() = uiTest(width = 720, height = 360) {
        showing { DeleteConfirmation(subject = brick, usages = emptyList(), readProblem = "Permission denied", onConfirm = {}, onUsageSelected = {}, onDismiss = {}) }
        onNodeWithText("Kubriko Creator couldn't find what uses it, so it can't say what deleting it changes: Permission denied").assertExists()
        onNodeWithText("Delete").assertIsEnabled()
    }
}
