/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * What a read returns to an agent that has read the same thing before in its conversation: that nothing changed, or only
 * what did (the values of an object that changed, the items of its lists at their index, or the lines of a text), or
 * everything again where that is no longer.
 */
internal object ResultDelta {

    private const val RECOVERY = "If that result isn't in front of you (a subagent read it, or it was summarized), read again with full: true."
    const val UNCHANGED = "Unchanged since you last read it in this conversation. $RECOVERY"
    private const val CHANGED = "Only what changed since you last read it in this conversation. $RECOVERY"
    private const val LINES_CHANGED = "The lines that changed since you last read it in this conversation, - as they were and + as they are. $RECOVERY"

    fun of(previous: JsonElement, current: JsonElement): JsonElement = when {
        previous == current -> JsonPrimitive(UNCHANGED)
        previous is JsonObject && current is JsonObject -> objectDelta(previous, current)
        previous is JsonArray && current is JsonArray -> listDelta("", previous, current).let { (changed, removed) -> deltaOf(changed, removed) }
        previous is JsonPrimitive && current is JsonPrimitive && previous.isString && current.isString -> textDelta(previous.content, current.content)
        else -> null
    }?.takeIf { it.toString().length < current.toString().length } ?: current

    private fun objectDelta(previous: JsonObject, current: JsonObject): JsonElement {
        val changed = LinkedHashMap<String, JsonElement>()
        val removed = mutableListOf<String>()
        current.forEach { (key, value) ->
            val before = previous[key]
            when {
                before == value -> Unit
                before is JsonArray && value is JsonArray -> listDelta(key, before, value).let { (changedItems, removedItems) ->
                    changed.putAll(changedItems)
                    removed.addAll(removedItems)
                }
                else -> changed[key] = value
            }
        }
        previous.keys.filter { it !in current }.forEach(removed::add)
        return deltaOf(changed, removed)
    }

    private fun listDelta(key: String, previous: JsonArray, current: JsonArray): Pair<Map<String, JsonElement>, List<String>> {
        val changed = current.withIndex().filter { (index, value) -> previous.getOrNull(index) != value }.associate { (index, value) -> "$key[$index]" to value }
        val removed = (current.size until previous.size).map { "$key[$it]" }
        return changed to removed
    }

    private fun deltaOf(changed: Map<String, JsonElement>, removed: List<String>) = buildJsonObject {
        put("sinceYourLastRead", CHANGED)
        if (changed.isNotEmpty()) putJsonObject("changed") { changed.forEach { (key, value) -> put(key, value) } }
        if (removed.isNotEmpty()) putJsonArray("removed") { removed.forEach { add(it) } }
    }

    private fun textDelta(previous: String, current: String): JsonElement {
        val before = previous.lines()
        val after = current.lines()
        val beforeSet = before.toSet()
        val afterSet = after.toSet()
        return JsonPrimitive(
            buildString {
                append(LINES_CHANGED)
                before.filter { it !in afterSet }.forEach { append("\n- ").append(it) }
                after.filter { it !in beforeSet }.forEach { append("\n+ ").append(it) }
            },
        )
    }
}
