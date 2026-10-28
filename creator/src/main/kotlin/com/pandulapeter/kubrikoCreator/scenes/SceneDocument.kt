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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.put
import kotlin.random.Random

/**
 * Reads and edits Scene files without loading the project's classes: a Scene is a list of type IDs with JSON states,
 * and every Actor with a body keeps its position in `body.position`. Every Actor has an ID ([SceneActorEntry.id]), and
 * files are written with one Actor per line, so that they diff and merge by Actor.
 */
internal object SceneDocument {

    private val json = Json { ignoreUnknownKeys = true }
    private const val INDEX_ID_PREFIX = "i"
    private const val NEW_ID_PREFIX = "a"
    private const val NEW_ID_LENGTH = 6
    private const val ID_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789"
    private val idPattern = Regex("[A-Za-z0-9_-]+")
    private val entries = ListSerializer(SceneActorEntry.serializer())

    /**
     * The Actors of the Scene file [content]. One without an ID, or with the ID of an earlier one, gets one from its
     * index (`i3`), which the same file always gives it, until a write keeps it.
     *
     * @throws SerializationException If [content] isn't a Scene.
     */
    fun parse(content: String): List<SceneActorEntry> {
        val parsed = json.decodeFromString(entries, content)
        val ids = HashSet<String>(parsed.size * 2)
        return parsed.mapIndexed { index, entry ->
            var id = entry.id.takeIf { it.isNotEmpty() && it !in ids } ?: "$INDEX_ID_PREFIX$index"
            while (id in ids) id += "_"
            ids += id
            if (id == entry.id) entry else entry.copy(id = id)
        }
    }

    /**
     * An ID that none of [existing] has: short, and unlike those [parse] gives from an index.
     */
    fun newId(existing: Collection<String>, random: Random = Random.Default): String {
        while (true) {
            val id = NEW_ID_PREFIX + (1..NEW_ID_LENGTH).map { ID_CHARACTERS[random.nextInt(ID_CHARACTERS.length)] }.joinToString("")
            if (id !in existing) return id
        }
    }

    /**
     * Whether [id] can be an Actor's ID: letters, numbers, dashes, and underscores.
     */
    fun isValidId(id: String) = idPattern.matches(id)

    fun isValid(content: String) = try {
        isValid(parse(content))
    } catch (_: SerializationException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    /**
     * Whether every one of [actors] has a state Kubriko can read.
     */
    fun isValid(actors: List<SceneActorEntry>) = actors.all { stateObject(it.state) != null }

    fun format(actors: List<SceneActorEntry>) = if (actors.isEmpty()) {
        "[]"
    } else {
        actors.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]\n") { json.encodeToString(SceneActorEntry.serializer(), it) }
    }

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

    /**
     * [state] changed by [change], or null if it isn't a JSON object or [change] left it as it was.
     */
    fun withStateChanged(state: String, change: (JsonObject) -> JsonObject): String? {
        val stateObject = stateObject(state) ?: return null
        return change(stateObject).takeIf { it != stateObject }?.toString()
    }

    private fun position(x: Float, y: Float) = buildJsonObject {
        put("x", x)
        put("y", y)
    }

    internal fun stateObject(state: String) = try {
        json.parseToJsonElement(state) as? JsonObject
    } catch (_: SerializationException) {
        null
    }

    private fun JsonObject.objectAt(key: String) = get(key) as? JsonObject

    private fun JsonObject.numberAt(key: String) = (get(key) as? JsonPrimitive)?.floatOrNull ?: 0f
}
