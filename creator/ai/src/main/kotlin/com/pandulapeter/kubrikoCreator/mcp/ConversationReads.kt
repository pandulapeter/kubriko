/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import com.pandulapeter.kubrikoCreator.context.ResultDelta
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap

/**
 * What each conversation of the assistant has read, so that reading the same thing again answers with what changed
 * since ([ResultDelta]) rather than all of it. Forgotten with the conversation.
 */
internal class ConversationReads {
    private val reads = ConcurrentHashMap<String, ConcurrentHashMap<String, JsonElement>>()

    /**
     * What the conversation [conversationId] gets for reading [result] with [toolName] and [arguments]: [result] itself
     * the first time or when [isFull], otherwise what changed since its last read of it.
     */
    fun answer(conversationId: String, toolName: String, arguments: JsonObject, result: JsonElement, isFull: Boolean): JsonElement {
        val seen = reads.getOrPut(conversationId) { ConcurrentHashMap() }
        val key = toolName + JsonObject(arguments.toSortedMap())
        val previous = seen.put(key, result)
        return if (previous == null || isFull) result else ResultDelta.of(previous, result)
    }

    fun forget(conversationId: String) {
        reads.remove(conversationId)
    }
}
