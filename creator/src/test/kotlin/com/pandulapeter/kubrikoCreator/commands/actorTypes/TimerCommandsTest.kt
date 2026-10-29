/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.timerNames
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_taken
import kubriko.creator.generated.resources.error_command_timer_unknown
import kotlin.test.Test
import kotlin.test.assertEquals

class TimerCommandsTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.StartTimer("spawn"), BlockDefinition.StartTimer("blink")))),
    )
    private val bat = ActorTypeDefinition(
        name = "Bat",
        parent = "Enemy",
        events = listOf(EventDefinition(EventType.TIMER_FINISHED, timer = "spawn", blocks = listOf(BlockDefinition.StopTimer("spawn")))),
    )
    private val wall = ActorTypeDefinition(
        name = "Wall",
        events = listOf(EventDefinition(EventType.TIMER_FINISHED, timer = "spawn")),
    )
    private val game = ManagerTypeDefinition(
        name = "Game",
        events = listOf(EventDefinition(EventType.TIMER_FINISHED, timer = "spawn")),
    )

    @Test
    fun renamingATimerFollowsItThroughTheWholeFamilyAndNothingElse() = runTest {
        val context = commandContext(enemy, bat, wall, game)
        context.assertRoundTrip(Invocation(RenameTimer, RenameTimer.Input("Bat", "spawn", "spawnBat"))) {
            listOf("Enemy", "Bat", "Wall", "Game").map { context.blockTypes!![it]!!.timerNames }
        }
        RenameTimer.apply(context, RenameTimer.Input("Bat", "spawn", "spawnBat"))
        assertEquals(listOf("spawnBat", "blink"), context.blockTypes!!["Enemy"]!!.timerNames)
        assertEquals(EventDefinition(EventType.TIMER_FINISHED, timer = "spawnBat", blocks = listOf(BlockDefinition.StopTimer("spawnBat"))), context.blockTypes!!["Bat"]!!.events.single())
        assertEquals(listOf("spawn"), context.blockTypes!!["Wall"]!!.timerNames)
        assertEquals(listOf("spawn"), context.blockTypes!!["Game"]!!.timerNames)
    }

    @Test
    fun aTimerIsRenamedOnlyIfItIsUsedAndToAFreeValidName() = runTest {
        val context = commandContext(enemy, bat, game)
        assertEquals(Rejection(Res.string.error_command_timer_unknown, "Bat", "fly"), RenameTimer.validate(context, RenameTimer.Input("Bat", "fly", "flap")))
        assertEquals(Rejection(Res.string.error_command_timer_taken), RenameTimer.validate(context, RenameTimer.Input("Bat", "spawn", "blink")))
        assertEquals(Rejection(Res.string.error_command_timer_invalid), RenameTimer.validate(context, RenameTimer.Input("Bat", "spawn", "Spawn")))
        assertEquals(null, RenameTimer.validate(context, RenameTimer.Input("Game", "spawn", "blink")))
    }

    @Test
    fun theScopeOffersTheTimersOfTheWholeFamily() = runTest {
        val context = commandContext(enemy, bat, wall, game)
        assertEquals(listOf("blink", "spawn"), context.scopeOf(context.blockTypes!!["Bat"]!!).timerNames)
        assertEquals(listOf("spawn"), context.scopeOf(context.blockTypes!!["Game"]!!).timerNames)
    }
}
