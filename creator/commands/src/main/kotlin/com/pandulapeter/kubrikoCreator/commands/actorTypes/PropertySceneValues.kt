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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Changes the state of every Actor of the [actorTypes] in every Scene, where each one keeps its own Property values.
 *
 * @return The states it replaced, by Actor ID, by the name of the Scene ([com.pandulapeter.kubrikoCreator.scenes.SceneStore.changeStates]).
 */
@EditorMutation
internal suspend fun CommandContext.changeSceneStates(actorTypes: Set<String>, change: (JsonObject) -> JsonObject): Map<String, Map<String, String>> =
    withContext(ioDispatcher) { scenes.changeStates(actorTypes, change) }

/**
 * [this] state without a value stored for the Property called [name] that doesn't fit [kind], which would keep the
 * Actor from loading (one left over from a removed Property of another kind).
 */
internal fun JsonObject.withoutMismatchedValue(name: String, kind: PropertyKind) = get(name)?.let { value ->
    if (value.fits(kind)) this else JsonObject(this - name)
} ?: this

/**
 * Whether [this] saved value is one of [kind]. A color is saved as its packed `Long`, which is a number too, but one beyond
 * an `Int`'s range (but transparent black's 0), as no number a Property is given is, so the two are told apart by it.
 * An Actor type reference is a text that is empty or shaped like an Actor type's name, and a sound or a music one that is
 * empty or the path of a file of its kind, which most texts aren't.
 */
internal fun JsonElement.fits(kind: PropertyKind) = this is JsonPrimitive && when (kind) {
    PropertyKind.NUMBER -> !isString && floatOrNull != null && longOrNull.let { it == null || it in Int.MIN_VALUE..Int.MAX_VALUE }
    PropertyKind.TEXT -> isString
    PropertyKind.SOUND -> isString && (content.isEmpty() || ResourceKind.of(content) == ResourceKind.SOUND)
    PropertyKind.MUSIC -> isString && (content.isEmpty() || ResourceKind.of(content) == ResourceKind.MUSIC)
    PropertyKind.TOGGLE -> !isString && booleanOrNull != null
    PropertyKind.ACTOR_TYPE -> isString && (content.isEmpty() || BlockTypeNaming.isWellFormed(content))
    PropertyKind.COLOR -> !isString && longOrNull.let { it != null && (it == 0L || it !in Int.MIN_VALUE..Int.MAX_VALUE) }
}
