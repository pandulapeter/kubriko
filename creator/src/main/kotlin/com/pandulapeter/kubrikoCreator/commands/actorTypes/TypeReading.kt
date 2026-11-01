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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.context.ReadDetail
import com.pandulapeter.kubrikoCreator.context.TypeNotation
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private val json = Json { encodeDefaults = true; explicitNulls = false }

/**
 * What read_actor_type and read_manager_type return for [definition]: as much of it as [detail] asks, or only the Event at
 * [eventIndex]. isChangedByHand is only there when it is true.
 */
internal fun <T : BlockTypeDefinition> CommandContext.typeReadResult(
    definition: T,
    serializer: KSerializer<T>,
    detail: ReadDetail,
    eventIndex: Int?,
): CommandOutcome {
    eventIndex?.let { index -> validateEvent(definition.name, index)?.let { return CommandOutcome.Rejected(it) } }
    val isChangedByHand = definition.name in requireBlockTypes.changedByHand.value
    val result = if (detail == ReadDetail.JSON) {
        val whole = json.encodeToJsonElement(serializer, definition).jsonObject
        val selected = eventIndex?.let { index ->
            JsonObject(mapOf("name" to whole.getValue("name"), "eventIndex" to Json.parseToJsonElement(index.toString()), "event" to json.encodeToJsonElement(EventDefinition.serializer(), definition.events[index])))
        } ?: whole
        if (isChangedByHand) JsonObject(selected + ("isChangedByHand" to Json.parseToJsonElement("true"))) else selected
    } else {
        notationOf(definition, detail, eventIndex, isChangedByHand)
    }
    return CommandOutcome.Success(result)
}

private fun notationOf(definition: BlockTypeDefinition, detail: ReadDetail, eventIndex: Int?, isChangedByHand: Boolean) = buildJsonObject {
    put("name", definition.name)
    if (eventIndex == null) {
        if (definition is ActorTypeDefinition) {
            definition.parent?.let { put("parent", it) }
            put("appearance", TypeNotation.appearanceOf(definition))
        }
        if (definition.properties.isNotEmpty()) putJsonArray("properties") { definition.properties.forEach { add(TypeNotation.of(it)) } }
    }
    putJsonArray("events") {
        definition.events.forEachIndexed { index, event -> if (eventIndex == null || index == eventIndex) add(TypeNotation.of(index, event, detail)) }
    }
    if (eventIndex == null && definition is ManagerTypeDefinition) {
        if (definition.texts.isNotEmpty()) putJsonArray("texts") { definition.texts.forEachIndexed { index, text -> add(TypeNotation.of(index, text)) } }
        definition.kotlinUi?.let { put("kotlinUi", it) }
    }
    if (isChangedByHand) put("isChangedByHand", true)
}
