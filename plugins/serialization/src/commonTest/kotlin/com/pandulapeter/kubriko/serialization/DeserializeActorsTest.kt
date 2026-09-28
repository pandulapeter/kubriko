/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeserializeActorsTest {

    private val serializationManager = SerializationManager.newInstance<SerializableMetadata<out Serializable<*>>, Serializable<*>>(
        SerializableMetadata<ValueActor>(typeId = VALUE_TYPE_ID, deserializeState = { ValueActor.State(Json.decodeFromString<Float>(it)) }),
        SerializableMetadata<PositiveActor>(typeId = POSITIVE_TYPE_ID, deserializeState = { PositiveActor.State(Json.decodeFromString<Float>(it)) }),
    )

    @Test
    fun validSceneRoundTrips() {
        val actors = listOf(ValueActor(1f), ValueActor(2.5f), PositiveActor(3f))

        val restored = serializationManager.deserializeActors(serializationManager.serializeActors(actors))

        assertEquals(listOf(1f, 2.5f, 3f), restored.map { (it as HasValue).value })
    }

    @Test
    fun invalidInnerStateFailsTheWholeScene() {
        val scene = scene(VALUE_TYPE_ID to "1.0", VALUE_TYPE_ID to "not a float", VALUE_TYPE_ID to "2.0")

        assertTrue(serializationManager.deserializeActors(scene).isEmpty())
    }

    @Test
    fun throwingRestoreFailsTheWholeScene() {
        val scene = scene(VALUE_TYPE_ID to "1.0", POSITIVE_TYPE_ID to "-1.0", VALUE_TYPE_ID to "2.0")

        assertTrue(serializationManager.deserializeActors(scene).isEmpty())
    }

    @Test
    fun unknownTypeIdIsSkipped() {
        val scene = scene(VALUE_TYPE_ID to "1.0", "unknown" to "whatever", VALUE_TYPE_ID to "2.0")

        assertEquals(listOf(1f, 2f), serializationManager.deserializeActors(scene).map { (it as HasValue).value })
    }

    @Test
    fun invalidOrEmptySceneReturnsEmptyList() {
        assertTrue(serializationManager.deserializeActors("not json").isEmpty())
        assertTrue(serializationManager.deserializeActors("[]").isEmpty())
    }

    private fun scene(vararg actors: Pair<String, String>) = actors.joinToString(prefix = "[", postfix = "]") { (typeId, state) ->
        """{"typeId":"$typeId","state":"$state"}"""
    }

    private interface HasValue {
        val value: Float
    }

    private class ValueActor(override val value: Float) : Serializable<ValueActor>, HasValue {
        override fun save() = State(value)

        class State(private val value: Float) : Serializable.State<ValueActor> {
            override fun restore() = ValueActor(value)

            override fun serialize() = Json.encodeToString(value)
        }
    }

    private class PositiveActor(override val value: Float) : Serializable<PositiveActor>, HasValue {
        init {
            require(value >= 0f) { "The value must not be negative." }
        }

        override fun save() = State(value)

        class State(private val value: Float) : Serializable.State<PositiveActor> {
            override fun restore() = PositiveActor(value)

            override fun serialize() = Json.encodeToString(value)
        }
    }

    private companion object {
        const val VALUE_TYPE_ID = "value"
        const val POSITIVE_TYPE_ID = "positive"
    }
}
