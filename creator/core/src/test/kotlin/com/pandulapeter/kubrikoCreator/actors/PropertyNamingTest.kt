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
import kotlin.test.assertNull

class PropertyNamingTest {

    @Test
    fun namesAreKotlinPropertyNames() {
        assertNull(PropertyNaming.validate("score", emptyList()))
        assertNull(PropertyNaming.validate("livesLeft2", emptyList()))
        assertEquals(NameProblem.NAME_EMPTY, PropertyNaming.validate("", emptyList()))
        listOf("Score", "2lives", "_score").forEach { assertEquals(NameProblem.PROPERTY_FIRST_CHARACTER, PropertyNaming.validate(it, emptyList()), it) }
        listOf("high score", "score!", "lives_left", "lives٣").forEach { assertEquals(NameProblem.PROPERTY_CHARACTERS, PropertyNaming.validate(it, emptyList()), it) }
        listOf("életerő", "pontszám").forEach { assertEquals(NameProblem.NAME_LETTER_BEYOND_A_TO_Z, PropertyNaming.validate(it, emptyList()), it) }
    }

    @Test
    fun kotlinsWordsAreToldApartFromTheGeneratedCodesNames() {
        listOf("class", "when", "fun", "val").forEach { assertEquals(NameProblem.NAME_KEYWORD, PropertyNaming.validate(it, emptyList()), it) }
    }

    @Test
    fun namesTheGeneratedCodeUsesAreReserved() = listOf("velocity", "body", "color", "other", "key", "isTouchingLeftEdge", "timer3RemainingInMilliseconds", "spawnPeriodInMilliseconds", "stateManager", "generatedScenes", "keyboardInputManager", "bodyCenter", "isRemoved", "box", "sprite", "isInSceneEditor", "scope", "textStyle1", "scoreManager", "levelManager").forEach {
        assertEquals(NameProblem.PROPERTY_RESERVED, PropertyNaming.validate(it, emptyList()), it)
    }

    @Test
    fun namesAreUniqueRegardlessOfCase() {
        assertEquals(NameProblem.PROPERTY_TAKEN, PropertyNaming.validate("score", listOf("lives", "score")))
        assertEquals(NameProblem.PROPERTY_TAKEN, PropertyNaming.validate("highScore", listOf("highscore")))
    }

    @Test
    fun newPropertiesGetTheFirstFreeName() {
        assertEquals("value", PropertyNaming.freeName("value", emptyList()))
        assertEquals("value3", PropertyNaming.freeName("value", listOf("value", "Value2")))
        assertEquals("velocity2", PropertyNaming.freeName("velocity", emptyList()))
    }
}
