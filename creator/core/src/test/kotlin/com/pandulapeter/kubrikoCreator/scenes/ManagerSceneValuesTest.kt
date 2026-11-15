/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ManagerSceneValuesTest {

    @Test
    fun theFirstEntryOfAManagerTypeIsTheOneItsValuesAreReadFrom() {
        val actors = listOf(
            SceneActorEntry("Wall", "{}", "wall"),
            SceneActorEntry("Game", """{"lives":5.0}""", "first"),
            SceneActorEntry("Game", """{"lives":9.0}""", "second"),
            SceneActorEntry("Hud", """{"title":"Hi"}""", "hud"),
        )
        assertEquals(
            mapOf("Game" to mapOf("lives" to JsonPrimitive(5.0)), "Hud" to mapOf("title" to JsonPrimitive("Hi"))),
            ManagerSceneValues.valuesByManagerType(actors, setOf("Game", "Hud", "Other")),
        )
        assertEquals(1, ManagerSceneValues.indexIn(actors, "Game"))
    }

    @Test
    fun valuesAreKeptInTheManagerTypesOrderWithUnknownOnesAfterAndNoneLeftMeansNoEntry() {
        val state = ManagerSceneValues.withValues("""{"gone":1,"title":"Hi"}""", mapOf("lives" to JsonPrimitive(3)), emptyList(), order = listOf("lives", "title"))
        assertEquals("""{"lives":3,"title":"Hi","gone":1}""", state)
        assertNull(ManagerSceneValues.withValues(state, emptyMap(), listOf("lives", "title", "gone"), order = listOf("lives", "title")))
    }
}
