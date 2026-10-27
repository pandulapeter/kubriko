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

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.put

/**
 * Reads and edits Scene files without loading the project's classes: a Scene is a list of type IDs with JSON states,
 * and every Actor with a body keeps its position in `body.position`.
 */
internal object SceneDocument {

    private val json = Json { ignoreUnknownKeys = true }
    private val entries = ListSerializer(SceneActorEntry.serializer())

    /**
     * @throws SerializationException If [content] isn't a Scene.
     */
    fun parse(content: String): List<SceneActorEntry> = json.decodeFromString(entries, content)

    fun isValid(content: String) = try {
        parse(content).all { stateObject(it.state) != null }
    } catch (_: SerializationException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    fun format(actors: List<SceneActorEntry>) = json.encodeToString(entries, actors)

    /**
     * The position stored in [state], or null if the Actor has none.
     */
    fun positionOf(state: String): Pair<Float, Float>? {
        val position = stateObject(state)?.objectAt("body")?.objectAt("position") ?: return null
        return position.numberAt("x") to position.numberAt("y")
    }

    /**
     * [state] with its position replaced, or null if the Actor has no body.
     */
    fun withPosition(state: String, x: Float, y: Float): String? {
        val stateObject = stateObject(state) ?: return null
        val body = stateObject.objectAt("body") ?: return null
        return JsonObject(stateObject + ("body" to JsonObject(body + ("position" to position(x, y))))).toString()
    }

    /**
     * The state of a newly placed Actor of a block-built Actor type, which leaves everything but its body to the
     * Actor type's defaults.
     */
    fun blockBuiltState(x: Float, y: Float, width: Float, height: Float) = buildJsonObject {
        put("body", buildJsonObject {
            put("position", position(x, y))
            put("size", buildJsonObject {
                put("width", width)
                put("height", height)
            })
        })
    }.toString()

    private fun position(x: Float, y: Float) = buildJsonObject {
        put("x", x)
        put("y", y)
    }

    private fun stateObject(state: String) = try {
        json.parseToJsonElement(state) as? JsonObject
    } catch (_: SerializationException) {
        null
    }

    private fun JsonObject.objectAt(key: String) = get(key) as? JsonObject

    private fun JsonObject.numberAt(key: String) = (get(key) as? JsonPrimitive)?.floatOrNull ?: 0f
}
