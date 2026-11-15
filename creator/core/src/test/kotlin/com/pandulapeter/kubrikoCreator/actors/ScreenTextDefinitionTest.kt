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

class ScreenTextDefinitionTest {

    @Test
    fun aNewTextShowsTheFirstPropertyOrScore() {
        assertEquals("{score}", ScreenTextDefinition.defaultFor(emptyList()).text)
        assertEquals("{lives}", ScreenTextDefinition.defaultFor(listOf(PropertyDefinition.Number("lives"), PropertyDefinition.Number("score"))).text)
    }

    @Test
    fun aTextCanBeShownOnlyWhileAConditionOnAPropertyHoldsWhoseNumberIsFixedOrAProperty() {
        assertTrue(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Toggle("isOver")))
        assertTrue(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Text("title", "Hi")))
        assertTrue(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Number("lives", value = NumberValue.Property("maximum", manager = "Game"))))
        assertFalse(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Number("lives", value = NumberValue.Random(0f, 3f))))
        assertFalse(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Number("lives", value = NumberValue.Edge(SceneEdge.LEFT))))
        assertFalse(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Chance()))
        assertFalse(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.Paused()))
        assertFalse(ScreenTextDefinition.isShownWhenAllowed(ConditionDefinition.ActorCount("Brick")))
        assertEquals(listOf(ConditionKind.NUMBER, ConditionKind.TEXT, ConditionKind.TOGGLE), ConditionKind.entries.filter { it.isAvailableForTexts })
    }
}
