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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginManagersTest {

    @Test
    fun theyFollowWhatEveryTypeUses() {
        val paddle = ActorTypeDefinition(name = "Paddle")
        val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Paddle")))
        val brick = ActorTypeDefinition(name = "Brick", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Deleted")))
        assertEquals(setOf("Ball", "Paddle"), PluginManagers.of(listOf(paddle, ball, brick)).collidableActorTypeNames)
        assertEquals(PluginManagers(), PluginManagers.of(listOf(paddle, brick)))
        val heldKey = EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(ConditionDefinition.KeyHeld())))
        assertTrue(PluginManagers.of(listOf(ManagerTypeDefinition(name = "Game", events = listOf(heldKey)))).usesKeyboard)
        assertTrue(PluginManagers.of(listOf(paddle.copy(events = listOf(EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.A))))).usesKeyboard)
        assertFalse(PluginManagers.of(listOf(paddle.copy(events = listOf(EventDefinition(EventType.KEY_PRESSED))))).usesKeyboard)
    }
}
