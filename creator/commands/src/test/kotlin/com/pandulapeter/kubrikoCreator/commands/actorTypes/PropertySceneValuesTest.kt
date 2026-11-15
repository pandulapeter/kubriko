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

import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertySceneValuesTest {

    @Test
    fun aSavedColorIsNoNumberAndASavedNumberNoColor() {
        val color = JsonPrimitive(SceneSettingsState.packedColorOf(0xFF3366CC))
        assertEquals(listOf(PropertyKind.COLOR), PropertyKind.entries.filter { color.fits(it) })
        assertEquals(listOf(PropertyKind.NUMBER), PropertyKind.entries.filter { JsonPrimitive(2.5f).fits(it) })
        assertEquals(listOf(PropertyKind.NUMBER), PropertyKind.entries.filter { JsonPrimitive(-3).fits(it) })
        assertEquals(listOf(PropertyKind.TOGGLE), PropertyKind.entries.filter { JsonPrimitive(true).fits(it) })
        assertEquals(listOf(PropertyKind.TEXT), PropertyKind.entries.filter { JsonPrimitive("hello there").fits(it) })
        assertEquals(listOf(PropertyKind.TEXT, PropertyKind.ACTOR_TYPE), PropertyKind.entries.filter { JsonPrimitive("Enemy").fits(it) })
        assertEquals(listOf(PropertyKind.TEXT, PropertyKind.ACTOR_TYPE), PropertyKind.entries.filter { JsonPrimitive("").fits(it) })
    }

    @Test
    fun aColorLeftOverFromARemovedPropertyIsDroppedWhenANumberOfItsNameIsAdded() {
        val state = JsonObject(mapOf("tint" to JsonPrimitive(SceneSettingsState.packedColorOf(0xFF3366CC)), "hp" to JsonPrimitive(3f)))
        assertEquals(JsonObject(mapOf("hp" to JsonPrimitive(3f))), state.withoutMismatchedValue("tint", PropertyKind.NUMBER))
        assertEquals(state, state.withoutMismatchedValue("tint", PropertyKind.COLOR))
    }
}
