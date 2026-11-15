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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The values a Scene gives the Properties of a Manager type when it starts: an entry of the Scene whose type ID is the
 * Manager type's name, its state holding a value by each Property's name, and only for the Properties it sets, so the
 * others keep what the Manager has. The game reads it through the class generated for it
 * (`ManagerSceneValuesCode`). A Scene has at most one per Manager type, and none while it sets nothing.
 */
public object ManagerSceneValues {

    /**
     * The index of the entry of [actors] holding the values for [managerType], or -1 if the Scene sets none.
     */
    public fun indexIn(actors: List<SceneActorEntry>, managerType: String): Int = actors.indexOfFirst { it.typeId == managerType }

    /**
     * What [actors] give each of [managerTypes], by Manager type and then by Property name, from the entry [indexIn]
     * finds for it (the first, should a Scene changed by hand have several).
     */
    public fun valuesByManagerType(actors: List<SceneActorEntry>, managerTypes: Set<String>): Map<String, Map<String, JsonPrimitive>> = buildMap {
        actors.forEach { actor -> if (actor.typeId in managerTypes && actor.typeId !in this) put(actor.typeId, valuesOf(actor.state)) }
    }

    /**
     * The values [state] sets, by Property name; empty if it can't be read.
     */
    public fun valuesOf(state: String): Map<String, JsonPrimitive> = SceneDocument.stateObject(state)?.mapNotNull { (name, value) -> (value as? JsonPrimitive)?.let { name to it } }?.toMap().orEmpty()

    /**
     * [state] (null for a Scene that sets nothing yet) setting [values] and no longer setting [cleared], with the rest
     * kept, or null if it would set nothing. Values are kept in [order] (the Manager type's Properties), and any it
     * doesn't name after them as they were, so the same values are always written alike, whatever changed them.
     */
    public fun withValues(state: String?, values: Map<String, JsonPrimitive>, cleared: Collection<String>, order: List<String>): String? {
        val kept = state?.let { SceneDocument.stateObject(it) }.orEmpty()
        val changed = (kept + values) - cleared.toSet()
        if (changed.isEmpty()) return null
        val positions = order.withIndex().associate { (index, name) -> name to index }
        return JsonObject(changed.entries.sortedBy { positions[it.key] ?: Int.MAX_VALUE }.associate { it.key to it.value }).toString()
    }
}
