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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Where [this] target is, as tool results write places: `Ball#2.1`, `Scene level`.
 */
internal fun CommandTarget.notation(): String = when (this) {
    is CommandTarget.BlockType -> RelationPlace.Type(name, eventIndex, blockIndex).toString()
    is CommandTarget.Scene -> RelationSubject.Scene(name).toString()
    is CommandTarget.Page -> "page $name"
    CommandTarget.VersionControl -> "version control"
    CommandTarget.Game -> "the game"
    CommandTarget.Project -> "the project's files"
}

/**
 * What a write tool returns: the revision the change made, what it changed, and the Event it changed as it is now, so
 * that the agent needn't read it again.
 */
internal fun CommandContext.writeSummary(revision: Int, commandName: String, target: CommandTarget?): JsonObject = buildJsonObject {
    put("revision", revision)
    put("changed", commandName + (target?.let { " at " + it.notation() } ?: ""))
    val type = target as? CommandTarget.BlockType ?: return@buildJsonObject
    val eventIndex = type.eventIndex ?: return@buildJsonObject
    blockTypes?.get(type.name)?.events?.getOrNull(eventIndex)?.let { event -> put("event", TypeNotation.of(eventIndex, event, ReadDetail.BLOCKS)) }
}
