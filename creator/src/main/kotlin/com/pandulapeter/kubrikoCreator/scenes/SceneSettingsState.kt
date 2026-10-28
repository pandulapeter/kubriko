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

import com.pandulapeter.kubrikoCreator.actors.SceneSettingsCodeGenerator
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Reads and changes the saved state of a Scene's settings (the generated `SceneSettings` Actor) without loading the
 * project's classes.
 */
internal object SceneSettingsState {

    /**
     * The settings as they are saved, with the defaults of the generated class for anything missing.
     *
     * @param x The horizontal center of the Scene's bounds.
     * @param y The vertical center of the Scene's bounds.
     * @param backgroundColor As `0xAARRGGBB`.
     * @param music By its path from `composeResources`, or empty for none.
     */
    data class Values(
        val x: Float = 0f,
        val y: Float = 0f,
        val width: Float = SceneSettingsCodeGenerator.DEFAULT_WIDTH,
        val height: Float = SceneSettingsCodeGenerator.DEFAULT_HEIGHT,
        val backgroundColor: Long = SceneSettingsCodeGenerator.DEFAULT_BACKGROUND_COLOR,
        val cameraX: Float = 0f,
        val cameraY: Float = 0f,
        val music: String = "",
    )

    /**
     * The index of the first Scene settings among [actors], or -1 if the Scene has none.
     */
    fun indexIn(actors: List<SceneActorEntry>) = actors.indexOfFirst { it.typeId == SceneSettingsCodeGenerator.SETTINGS_NAME }

    /**
     * The music of the Scene file [content], or null if it has none (or isn't a Scene).
     */
    fun musicOf(content: String) = try {
        SceneDocument.parse(content).let { actors -> actors.getOrNull(indexIn(actors)) }?.let { read(it.state).music }?.ifEmpty { null }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    fun read(state: String): Values {
        val stateObject = SceneDocument.stateObject(state) ?: return Values()
        val body = stateObject["body"] as? JsonObject
        val position = body?.get("position") as? JsonObject
        val size = body?.get("size") as? JsonObject
        val camera = stateObject[SceneSettingsCodeGenerator.CAMERA_START_KEY] as? JsonObject
        val defaults = Values()
        return Values(
            x = position.numberAt("x") ?: defaults.x,
            y = position.numberAt("y") ?: defaults.y,
            width = size.numberAt("width") ?: defaults.width,
            height = size.numberAt("height") ?: defaults.height,
            backgroundColor = (stateObject[SceneSettingsCodeGenerator.BACKGROUND_COLOR_KEY] as? JsonPrimitive)?.longOrNull?.let(::argbOf) ?: defaults.backgroundColor,
            cameraX = camera.numberAt("x") ?: defaults.cameraX,
            cameraY = camera.numberAt("y") ?: defaults.cameraY,
            music = (stateObject[SceneSettingsCodeGenerator.MUSIC_KEY] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: defaults.music,
        )
    }

    /**
     * [state] (an empty one for new settings) with the given values replaced and everything else kept. A new size moves
     * the pivot to the middle, so that the position stays the center of the bounds.
     */
    fun withChanges(
        state: String = "{}",
        x: Float? = null,
        y: Float? = null,
        width: Float? = null,
        height: Float? = null,
        backgroundColor: Long? = null,
        cameraX: Float? = null,
        cameraY: Float? = null,
        music: String? = null,
    ): String {
        val values = read(state)
        val stateObject = SceneDocument.stateObject(state) ?: JsonObject(emptyMap())
        val body = (stateObject["body"] as? JsonObject).orEmpty().toMutableMap()
        if (x != null || y != null || "position" !in body) body["position"] = point(x ?: values.x, y ?: values.y)
        if (width != null || height != null || "size" !in body) {
            val newWidth = width ?: values.width
            val newHeight = height ?: values.height
            body["size"] = buildJsonObject {
                put("width", newWidth)
                put("height", newHeight)
            }
            body["pivot"] = point(newWidth / 2, newHeight / 2)
        }
        val changed = stateObject.toMutableMap()
        changed["body"] = JsonObject(body)
        backgroundColor?.let { changed[SceneSettingsCodeGenerator.BACKGROUND_COLOR_KEY] = JsonPrimitive(packedColorOf(it)) }
        if (cameraX != null || cameraY != null) changed[SceneSettingsCodeGenerator.CAMERA_START_KEY] = point(cameraX ?: values.cameraX, cameraY ?: values.cameraY)
        music?.let { changed[SceneSettingsCodeGenerator.MUSIC_KEY] = JsonPrimitive(it) }
        return JsonObject(changed).toString()
    }

    /**
     * How Kubriko's serialization saves an sRGB Compose `Color`: its packed value, which keeps the `0xAARRGGBB` in the
     * upper half.
     */
    fun packedColorOf(argb: Long) = (argb and 0xFFFFFFFFL) shl 32

    fun argbOf(packedColor: Long) = packedColor ushr 32

    private fun point(x: Float, y: Float) = buildJsonObject {
        put("x", x)
        put("y", y)
    }

    private fun JsonObject?.orEmpty() = this ?: JsonObject(emptyMap())

    private fun JsonObject?.numberAt(key: String) = (this?.get(key) as? JsonPrimitive)?.floatOrNull
}
