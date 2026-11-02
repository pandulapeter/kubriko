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

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Keeps the assistant's conversations of every project in [file], the newest [conversationsPerProject] of each. Every
 * call blocks; a missing or unreadable file means none (one that can't be decoded is kept as `conversations.json.bak` by
 * the next save), and conversations that can't be written stay until Kubriko Creator quits.
 */
internal class SavedConversationStore(
    file: File,
    private val conversationsPerProject: Int = 20,
) {
    private val store = JsonFileStore(file, ListSerializer(SavedConversation.serializer()))

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
        store.write(others.filterNot { it.project == conversation.project } + project)
    }

    private fun read() = store.read().valueOrNull().orEmpty()

    companion object {
        fun default() = SavedConversationStore(File(settingsDirectory(), "conversations.json"))
    }
}
