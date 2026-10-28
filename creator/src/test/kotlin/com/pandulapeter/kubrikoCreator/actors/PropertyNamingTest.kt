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

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_property_name_invalid
import kubriko.creator.generated.resources.error_property_name_reserved
import kubriko.creator.generated.resources.error_property_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PropertyNamingTest {

    @Test
    fun namesAreKotlinPropertyNames() {
        assertNull(PropertyNaming.validate("score", emptyList()))
        assertNull(PropertyNaming.validate("livesLeft2", emptyList()))
        listOf("", "Score", "2lives", "high score", "score!", "lives_left").forEach { assertEquals(Res.string.error_property_name_invalid, PropertyNaming.validate(it, emptyList()), it) }
    }

    @Test
    fun namesTheGeneratedCodeUsesAreReserved() = listOf("velocity", "body", "color", "other", "key", "when", "isTouchingLeftEdge", "timer3RemainingInMilliseconds", "stateManager", "generatedScenes", "keyboardInputManager").forEach {
        assertEquals(Res.string.error_property_name_reserved, PropertyNaming.validate(it, emptyList()), it)
    }

    @Test
    fun namesAreUniqueRegardlessOfCase() {
        assertEquals(Res.string.error_property_name_taken, PropertyNaming.validate("score", listOf("lives", "score")))
        assertEquals(Res.string.error_property_name_taken, PropertyNaming.validate("highScore", listOf("highscore")))
    }

    @Test
    fun newPropertiesGetTheFirstFreeName() {
        assertEquals("value", PropertyNaming.freeName("value", emptyList()))
        assertEquals("value3", PropertyNaming.freeName("value", listOf("value", "Value2")))
        assertEquals("velocity2", PropertyNaming.freeName("velocity", emptyList()))
    }
}
