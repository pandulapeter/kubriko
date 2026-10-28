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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.PI

/**
 * Changes the values an Actor's saved state keeps, as Kubriko's serialization saves them, without loading the project's
 * classes: the body's position, size, scale, and rotation, and for block-built Actor types, the color and Properties.
 */
internal object ActorStateEdits {

    /**
     * The values to change; null ones stay as they are.
     *
     * @param rotationInDegrees Saved in radians, as Kubriko's `AngleRadians`.
     * @param color As `0xAARRGGBB`, saved as Compose's packed value.
     * @param properties Values by Property name.
     */
    data class Changes(
        val x: Float? = null,
        val y: Float? = null,
        val width: Float? = null,
        val height: Float? = null,
        val rotationInDegrees: Float? = null,
        val scaleHorizontal: Float? = null,
        val scaleVertical: Float? = null,
        val color: Long? = null,
        val properties: Map<String, JsonPrimitive> = emptyMap(),
    )

    /**
     * [state] with [changes] made and everything else kept, or null if it isn't a JSON object. A new size moves the
     * pivot to the middle, as a newly placed Actor has it.
     */
    fun withChanges(state: String, changes: Changes): String? {
        val stateObject = SceneDocument.stateObject(state) ?: return null
        val changed = stateObject.toMutableMap()
        val body = (stateObject[BODY] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        if (changes.x != null || changes.y != null) {
            body[POSITION] = body.merged(POSITION, "x" to changes.x, "y" to changes.y)
        }
        if (changes.width != null || changes.height != null) {
            val size = body.merged(SIZE, WIDTH to changes.width, HEIGHT to changes.height)
            body[SIZE] = size
            val width = (size[WIDTH] as? JsonPrimitive)?.content?.toFloatOrNull()
            val height = (size[HEIGHT] as? JsonPrimitive)?.content?.toFloatOrNull()
            if (width != null && height != null) {
                body[PIVOT] = buildJsonObject {
                    put("x", width / 2)
                    put("y", height / 2)
                }
            }
        }
        if (changes.scaleHorizontal != null || changes.scaleVertical != null) {
            body[SCALE] = body.merged(SCALE, "horizontal" to changes.scaleHorizontal, "vertical" to changes.scaleVertical)
        }
        changes.rotationInDegrees?.let { body[ROTATION] = JsonPrimitive((it * PI / 180).toFloat()) }
        if (body.isNotEmpty() || BODY in stateObject) changed[BODY] = JsonObject(body)
        changes.color?.let { changed[COLOR] = JsonPrimitive(SceneSettingsState.packedColorOf(it)) }
        changed.putAll(changes.properties)
        return JsonObject(changed).toString()
    }

    /**
     * The color saved in [state] as `0xAARRGGBB`, or null if it has none.
     */
    fun colorOf(state: String) = (SceneDocument.stateObject(state)?.get(COLOR) as? JsonPrimitive)?.content?.toLongOrNull()?.let(SceneSettingsState::argbOf)

    /**
     * The value saved in [state] for the Property [name], or null if it has none.
     */
    fun propertyOf(state: String, name: String) = SceneDocument.stateObject(state)?.get(name) as? JsonPrimitive

    private fun Map<String, JsonElement>.merged(key: String, vararg values: Pair<String, Float?>) = JsonObject(
        ((get(key) as? JsonObject).orEmpty()) + values.mapNotNull { (name, value) -> value?.let { name to JsonPrimitive(it) } },
    )

    private fun JsonObject?.orEmpty(): Map<String, JsonElement> = this ?: emptyMap()

    private const val BODY = "body"
    private const val POSITION = "position"
    private const val SIZE = "size"
    private const val WIDTH = "width"
    private const val HEIGHT = "height"
    private const val PIVOT = "pivot"
    private const val SCALE = "scale"
    private const val ROTATION = "rotation"
    private const val COLOR = "color"
}
