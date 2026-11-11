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
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.usagesOf
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteTimerTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.Log("hi"), BlockDefinition.StartTimer("spawn"), BlockDefinition.StartTimer("blink"))),
            EventDefinition(EventType.TIMER_FINISHED, timer = "spawn", blocks = listOf(BlockDefinition.StartTimer("spawn"), BlockDefinition.RemoveSelf)),
        ),
    )
    private val boss = ActorTypeDefinition(
        name = "Boss",
        parent = "Enemy",
        events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.StopTimer("spawn")))),
    )
    private val bystander = ActorTypeDefinition(name = "Bystander", events = listOf(EventDefinition(EventType.TIMER_FINISHED, timer = "spawn")))
    private val hud = ManagerTypeDefinition(name = "Hud", events = listOf(EventDefinition(EventType.TIMER_FINISHED, timer = "spawn")))

    @Test
    fun theUsagesAreTheFamilysEventsAndTheBlocksOutsideThem() = runTest {
        val context = commandContext(enemy, boss, bystander, hud)
        val subject = RelationSubject.Timer("Enemy", "spawn")
        assertEquals(context.usagesOf(subject), context.usagesOf(RelationSubject.Timer("Boss", "spawn")), "One Timer, whichever type of the family it is asked through.")
        assertEquals(
            listOf(
                Usage(UsagePlace.InModel(RelationPlace.Type("Boss", 0, 0)), RelationKind.USES_TIMER, subject, UsageEffect.BLOCK_REMOVED),
                Usage(UsagePlace.InModel(RelationPlace.Type("Enemy", 0, 1)), RelationKind.USES_TIMER, subject, UsageEffect.BLOCK_REMOVED),
                Usage(UsagePlace.InModel(RelationPlace.Type("Enemy", 1)), RelationKind.USES_TIMER, subject, UsageEffect.EVENT_REMOVED),
            ),
            context.usagesOf(subject),
        )
    }

    @Test
    fun deletingRemovesItFromTheFamilyAloneAndIsUndoneExactly() = runTest {
        val context = commandContext(enemy, boss, bystander, hud)
        val after = context.assertRoundTrip(Invocation(DeleteTimer, DeleteTimer.Input("Enemy", "spawn"))) { context.blockTypes!!.types.value }
        val byName = after.associateBy { it.name }
        assertEquals(listOf("blink"), byName.getValue("Enemy").timerNames)
        assertEquals(listOf(BlockDefinition.Log("hi"), BlockDefinition.StartTimer("blink")), byName.getValue("Enemy").events.single().blocks)
        assertTrue(byName.getValue("Boss").timerNames.isEmpty())
        assertEquals(bystander, byName.getValue("Bystander"))
        assertEquals(hud, byName.getValue("Hud"))
    }

    @Test
    fun aManagerTypesTimerIsItsOwn() = runTest {
        val context = commandContext(enemy, hud)
        DeleteTimer.apply(context, DeleteTimer.Input("Hud", "spawn"))
        assertTrue(context.blockTypes!!["Hud"]!!.events.isEmpty())
        assertEquals(enemy, context.blockTypes!!["Enemy"])
    }

    @Test
    fun onlyATimerInUseCanBeDeleted() = runTest {
        val context = commandContext(enemy, boss)
        assertEquals(
            Rejection(LookupProblem.TIMER_UNKNOWN_SIMILAR, "Boss", "spawns", "spawn"),
            Invocation(DeleteTimer, DeleteTimer.Input("Boss", "spawns")).validate(context),
        )
    }

    @Test
    fun aTimerFieldOnAnotherKindOfEventIsNoTimer() = runTest {
        val stray = ActorTypeDefinition(
            name = "Stray",
            events = listOf(
                EventDefinition(EventType.EVERY_FRAME, timer = "spawn", blocks = listOf(BlockDefinition.Log("still here"))),
                EventDefinition(EventType.EVERY_FRAME, timer = "ghost"),
                EventDefinition(EventType.TIMER_FINISHED, timer = "spawn"),
            ),
        )
        val context = commandContext(stray)
        assertEquals(listOf("spawn"), stray.timerNames)
        assertEquals(Rejection(LookupProblem.TIMER_UNKNOWN, "Stray", "ghost"), Invocation(DeleteTimer, DeleteTimer.Input("Stray", "ghost")).validate(context))
        DeleteTimer.apply(context, DeleteTimer.Input("Stray", "spawn"))
        assertEquals(stray.events.take(2), context.blockTypes!!["Stray"]!!.events)
    }

    @Test
    fun kotlinUsingTheTimersMembersIsListedAndLeftAsItIs() = runTest {
        val kotlin = BlockDefinition.Kotlin("spawnRemainingInMilliseconds = 0L")
        val clock = ManagerTypeDefinition(
            name = "Clock",
            events = listOf(EventDefinition(EventType.TIMER_FINISHED, timer = "tick"), EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Kotlin("println(tickPeriodInMilliseconds)")))),
            kotlinUi = "Text(tickRemainingInMilliseconds.toString())",
        )
        val context = commandContext(enemy.copy(events = enemy.events + EventDefinition(EventType.EVERY_FRAME, blocks = listOf(kotlin))), clock)
        assertEquals(
            listOf(RelationPlace.Type("Clock", 1, 0), RelationPlace.KotlinUi("Clock")),
            context.usagesOf(RelationSubject.Timer("Clock", "tick")).filter { it.effect == UsageEffect.KOTLIN_TO_CHANGE }.map { (it.place as UsagePlace.InModel).place },
        )
        DeleteTimer.apply(context, DeleteTimer.Input("Enemy", "spawn"))
        assertEquals(listOf(kotlin), context.blockTypes!!["Enemy"]!!.events.last().blocks)
    }
}
