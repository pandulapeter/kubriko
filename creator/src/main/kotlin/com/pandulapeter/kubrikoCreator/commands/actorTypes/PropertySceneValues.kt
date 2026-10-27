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
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull

/**
 * Changes the state of every Actor of [actorType] in every Scene, where each one keeps its own Property values, and
 * tells the editor which Scenes changed.
 */
@EditorMutation
internal suspend fun CommandContext.changeSceneStates(actorType: String, change: (JsonObject) -> JsonObject) {
    val changedScenes = withContext(ioDispatcher) { scenes.changeStates(actorType, change) }
    if (changedScenes.isNotEmpty()) onScenesChanged(changedScenes)
}

/**
 * [this] state without a value stored for the Property called [name] that doesn't fit [kind], which would keep the
 * Actor from loading (one left over from a removed Property of another kind).
 */
internal fun JsonObject.withoutMismatchedValue(name: String, kind: PropertyKind) = get(name)?.let { value ->
    if (value.fits(kind)) this else JsonObject(this - name)
} ?: this

/**
 * [this] state with the value of the Property called [oldName] stored as [newName], in the same place.
 */
internal fun JsonObject.withValueRenamed(oldName: String, newName: String) = if (oldName in this) {
    JsonObject(entries.filter { it.key != newName }.associate { (key, value) -> (if (key == oldName) newName else key) to value })
} else {
    this
}

private fun JsonElement.fits(kind: PropertyKind) = this is JsonPrimitive && when (kind) {
    PropertyKind.NUMBER -> !isString && floatOrNull != null
    PropertyKind.TEXT -> isString
    PropertyKind.TOGGLE -> !isString && booleanOrNull != null
}
