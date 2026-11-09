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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceStatus
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ConsoleTest {

    @Test
    fun problemsErrorsAndWhatTheGameLogsOpenTheConsoleButBuildChatterDoesnt() {
        assertEquals(
            listOf(true, true, true, true, false, false),
            listOf(
                ConsoleEntry.Problem("Unresolved reference", location = null),
                ConsoleEntry.Log("Score: 3", source = "Paddle", isImportant = true),
                ConsoleEntry.SessionFailure("The game stopped"),
                ConsoleEntry.Status(WorkspaceStatus.BUILT, isError = true),
                ConsoleEntry.Status(WorkspaceStatus.BUILT),
                ConsoleEntry.BuildOutput("> Task :compileKotlinDesktop"),
            ).map { it.opensConsole },
        )
    }

    @Test
    fun theListFollowsNewLinesOnceFullUntilTheUserScrollsAway() = uiTest {
        var lines by mutableStateOf(ConsoleLines())
        var count = 0L
        fun add(number: Int) {
            count++
            val first = maxOf(0, number - CAPACITY + 1)
            lines = ConsoleLines(count, (first..number).map { ConsoleEntry.BuildOutput("Line $it") }, firstNumber = first.toLong())
        }
        showing {
            Console(lines = lines, isExpanded = true, onExpandedChange = {}, onClear = {}, listHeight = 200.dp)
        }
        (0..CAPACITY + 10).forEach { add(it) }
        waitForIdle()
        onNodeWithText("Line ${CAPACITY + 10}").assertIsDisplayed()
        add(CAPACITY + 11)
        waitForIdle()
        onNodeWithText("Line ${CAPACITY + 11}").assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToIndex(0)
        waitForIdle()
        add(CAPACITY + 12)
        waitForIdle()
        onNodeWithText("Line ${CAPACITY + 12}").assertDoesNotExist()
        onNodeWithText("Line 13").assertIsDisplayed()
    }

    @Test
    fun rejectionsNameEventsAndBlocksAsTheEditorDoes() = uiTest {
        val rejections = listOf(
            ConsoleEntry.Status(CommandProblem.EVENT_GAME_STARTED_IN_ACTOR_TYPE, isError = true),
            ConsoleEntry.Status(CommandProblem.KEY_NOT_ALLOWED, isError = true),
            ConsoleEntry.Status(CommandProblem.BLOCK_REMOVE_OTHER_OUTSIDE_COLLISION, isError = true),
            ConsoleEntry.Status(CommandProblem.TOGGLE_PROPERTY_MISSING, isError = true, arguments = listOf("isOver", "Game")),
        )
        showing {
            Console(lines = ConsoleLines(1, rejections), isExpanded = true, onExpandedChange = {}, onClear = {}, listHeight = 200.dp)
        }
        onNodeWithText("Use When added to the Scene instead", substring = true).assertIsDisplayed()
        onNodeWithText("Only a When a key is pressed Event has a key.").assertIsDisplayed()
        onNodeWithText("Remove the other Actor works only in a When it collides with Event", substring = true).assertIsDisplayed()
        onNodeWithText("Game has no toggle Property called isOver.").assertIsDisplayed()
        listOf("gameStarted", "keyPressed", "collision)", "removeOther").forEach { onNodeWithText(it, substring = true).assertDoesNotExist() }
        saveSnapshot("console-rejections")
    }

    @Test
    fun theListFollowsWhenTheLastLineIsShownNothingWasYetOrItIsGone() {
        assertEquals(true, isFollowing(previousLastNumber = null, firstNumber = 0) { false })
        assertEquals(true, isFollowing(previousLastNumber = 9, firstNumber = 0) { it == 9L })
        assertEquals(false, isFollowing(previousLastNumber = 9, firstNumber = 0) { it < 2 })
        assertEquals(true, isFollowing(previousLastNumber = 9, firstNumber = 20) { false })
    }

    private companion object {
        const val CAPACITY = 40
    }
}
