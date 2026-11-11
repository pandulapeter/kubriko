/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TimerChoiceTest {

    @Test
    fun theMenuOffersTheTimersInUseThenNamingAndRenaming() {
        assertEquals(
            listOf(TimerOption.Existing("blink"), TimerOption.Existing("spawn"), TimerOption.New, TimerOption.Rename),
            timerOptions("spawn", listOf("spawn", "blink"), canRename = true),
        )
        assertEquals(listOf(TimerOption.Existing("timer"), TimerOption.New), timerOptions("timer", emptyList(), canRename = false))
        assertEquals(
            listOf(TimerOption.Existing("spawn"), TimerOption.New, TimerOption.Rename, TimerOption.Delete),
            timerOptions("spawn", listOf("spawn"), canRename = true, canDelete = true),
        )
    }

    @Test
    fun deletingIsOfferedWhereTheTimerCanBeDeleted() = uiTest(width = 360, height = 300) {
        val deleted = mutableListOf<Unit>()
        showing { TimerChoice(timer = "spawn", timerNames = listOf("spawn"), onSelected = {}, onRename = {}, onDelete = { deleted += Unit }) }
        onNodeWithText("spawn").performClick()
        saveSnapshot("timer-menu-delete")
        onNodeWithText("Delete this Timer…").performClick()
        assertEquals(1, deleted.size)
    }

    @Test
    fun aNewNameMustBeValidAndARenameMustBeFree() {
        assertEquals(NameProblem.TIMER_FIRST_CHARACTER, timerNameError("Spawn", "spawn", listOf("spawn"), isRenaming = false))
        assertEquals(NameProblem.NAME_KEYWORD, timerNameError("class", "spawn", listOf("spawn"), isRenaming = false))
        assertEquals(null, timerNameError("blink", "spawn", listOf("spawn", "blink"), isRenaming = false))
        assertEquals(CommandProblem.TIMER_TAKEN, timerNameError("blink", "spawn", listOf("spawn", "blink"), isRenaming = true))
        assertEquals(null, timerNameError("spawn", "spawn", listOf("spawn", "blink"), isRenaming = true))
    }

    @Test
    fun aTimerNamedLikeAKotlinWordIsRefusedForThatReason() = uiTest(width = 360, height = 200) {
        val selected = mutableListOf<String>()
        showing { TimerChoice(timer = "spawn", timerNames = listOf("spawn"), onSelected = { selected += it }) }
        onNodeWithText("spawn").performClick()
        onNodeWithText("New Timer…").performClick()
        onNode(hasSetTextAction()).performTextInput("class")
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        onNodeWithText("That word means something in Kotlin", substring = true).assertExists()
        saveSnapshot("timer-name-keyword")
        assertEquals(emptyList(), selected)
    }
}
