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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_taken
import kotlin.test.Test
import kotlin.test.assertEquals

class TimerChoiceTest {

    @Test
    fun theMenuOffersTheTimersInUseThenNamingAndRenaming() {
        assertEquals(
            listOf(TimerOption.Existing("blink"), TimerOption.Existing("spawn"), TimerOption.New, TimerOption.Rename),
            timerOptions("spawn", listOf("spawn", "blink"), canRename = true),
        )
        assertEquals(listOf(TimerOption.Existing("timer"), TimerOption.New), timerOptions("timer", emptyList(), canRename = false))
    }

    @Test
    fun aNewNameMustBeValidAndARenameMustBeFree() {
        assertEquals(Res.string.error_command_timer_invalid, timerNameError("Spawn", "spawn", listOf("spawn"), isRenaming = false))
        assertEquals(null, timerNameError("blink", "spawn", listOf("spawn", "blink"), isRenaming = false))
        assertEquals(Res.string.error_command_timer_taken, timerNameError("blink", "spawn", listOf("spawn", "blink"), isRenaming = true))
        assertEquals(null, timerNameError("spawn", "spawn", listOf("spawn", "blink"), isRenaming = true))
    }
}
