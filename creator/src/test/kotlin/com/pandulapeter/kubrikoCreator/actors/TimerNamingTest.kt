/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerNamingTest {
    private val json = Json

    @Test
    fun namesAreIdentifiersStartingLowercase() {
        listOf("timer", "spawnEnemy", "timer12").forEach { assertTrue(TimerNaming.isValid(it), it) }
        listOf("", "Spawn", "spawn enemy", "2timer", "while", "spawn_enemy").forEach { assertFalse(TimerNaming.isValid(it), it) }
    }

    @Test
    fun aNameMakesItsFieldsAndItsHook() {
        assertEquals("spawnEnemyRemainingInMilliseconds", TimerNaming.remainingField("spawnEnemy"))
        assertEquals("spawnEnemyPeriodInMilliseconds", TimerNaming.periodField("spawnEnemy"))
        assertEquals("onSpawnEnemyFinished", TimerNaming.hookName("spawnEnemy"))
    }

    @Test
    fun numberedTimersOfOlderProjectsKeepTheFieldsTheyHad() {
        assertEquals("timer3", TimerNaming.ofNumber(3))
        assertEquals("timer3RemainingInMilliseconds", TimerNaming.remainingField(TimerNaming.ofNumber(3)))
        assertEquals("onTimer3Finished", TimerNaming.hookName(TimerNaming.ofNumber(3)))
    }

    @Test
    fun numbersAreReadAsTheirNamesAndNamesAreWrittenAsThemselves() {
        assertEquals(EventDefinition(EventType.TIMER_FINISHED, timer = "timer2"), json.decodeFromString<EventDefinition>("""{"type":"timerFinished","timer":2}"""))
        assertEquals(BlockDefinition.StopTimer("timer8"), json.decodeFromString<BlockDefinition>("""{"type":"stopTimer","timer":8}"""))
        val block = BlockDefinition.StartTimer("spawnEnemy", seconds = 2f)
        assertEquals(block, json.decodeFromString<BlockDefinition>(json.encodeToString<BlockDefinition>(block)))
        assertTrue("\"timer\":\"spawnEnemy\"" in json.encodeToString<BlockDefinition>(block))
    }

    @Test
    fun aTypeUsesTheTimersItsEventsAndBlocksName() {
        val definition = ActorTypeDefinition(
            name = "Bat",
            events = listOf(
                EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.StartTimer("flap"), BlockDefinition.StopTimer("dive"))),
                EventDefinition(EventType.TIMER_FINISHED, timer = "flap"),
            ),
        )
        assertEquals(listOf("flap", "dive"), definition.timerNames)
        assertEquals(listOf("wingbeat", "dive"), definition.withTimerRenamed("flap", "wingbeat").timerNames)
    }
}
