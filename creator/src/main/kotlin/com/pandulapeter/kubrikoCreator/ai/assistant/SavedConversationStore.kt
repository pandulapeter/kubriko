/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.project.writeTextAtomically
import com.pandulapeter.kubrikoCreator.settingsDirectory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Keeps the assistant's conversations of every project in [file], the newest [conversationsPerProject] of each. Every
 * call blocks; a missing or unreadable file means none.
 */
internal class SavedConversationStore(
    private val file: File,
    private val conversationsPerProject: Int = 20,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val serializer = ListSerializer(SavedConversation.serializer())

    /**
     * The conversations about [project] held by the agent [agentId], newest first.
     */
    @Synchronized
    fun of(project: File, agentId: String) = read().filter { it.project == project.absolutePath && it.agentId == agentId }.sortedByDescending { it.updatedAt }

    /**
     * Keeps [conversation], replacing the one with its ID, and forgets the oldest of its project beyond the limit.
     */
    @Synchronized
    fun save(conversation: SavedConversation) {
        val others = read().filterNot { it.id == conversation.id && it.agentId == conversation.agentId }
        val project = (others.filter { it.project == conversation.project } + conversation).sortedByDescending { it.updatedAt }.take(conversationsPerProject)
        write(others.filterNot { it.project == conversation.project } + project)
    }

    private fun read(): List<SavedConversation> = try {
        if (file.isFile) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (_: IOException) {
        emptyList()
    } catch (_: SerializationException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun write(conversations: List<SavedConversation>) {
        try {
            file.parentFile?.mkdirs()
            file.writeTextAtomically(json.encodeToString(serializer, conversations))
        } catch (_: IOException) {
            // The conversations stay until Kubriko Creator quits.
        }
    }

    companion object {
        fun default() = SavedConversationStore(File(settingsDirectory(), "conversations.json"))
    }
}
