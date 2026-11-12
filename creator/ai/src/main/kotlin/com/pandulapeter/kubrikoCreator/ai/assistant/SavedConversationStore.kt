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
 * Keeps the assistant's conversations of every project in [file], the newest [conversationsPerProject] of each, and
 * their messages in [texts], forgotten with them. Every call blocks; a missing or unreadable file means none (one that
 * can't be decoded is kept as `conversations.json.bak` by the next save), and conversations that can't be written stay
 * until Kubriko Creator quits.
 */
public class SavedConversationStore(
    file: File,
    private val conversationsPerProject: Int = 20,
    readText: (File) -> String = File::readText,
    internal val texts: ConversationTextStore? = null,
) {
    private val store = JsonFileStore(file, ListSerializer(SavedConversation.serializer()), readText = readText)

    /**
     * The conversations about [project] held by the agent [agentId], newest first.
     */
    @Synchronized
    internal fun of(project: File, agentId: String) = read().filter { it.project == project.absolutePath && it.agentId == agentId }.sortedByDescending { it.updatedAt }

    /**
     * The conversations about [project] held by any agent, newest first.
     */
    @Synchronized
    internal fun of(project: File) = read().filter { it.project == project.absolutePath }.sortedByDescending { it.updatedAt }

    /**
     * Whether the agent [agentId] holds a conversation about any project, which means it has worked here before.
     */
    @Synchronized
    internal fun hasAny(agentId: String) = read().any { it.agentId == agentId }

    /**
     * Keeps [conversation], replacing the one with its ID (and keeping that one's summary, as an earlier one, when it has
     * none), and [messages] with it, if given, and forgets the oldest of its project beyond the limit, messages and all.
     */
    @Synchronized
    internal fun save(conversation: SavedConversation, messages: List<KeptMessage>? = null) {
        val isSame = { saved: SavedConversation -> saved.id == conversation.id && saved.agentId == conversation.agentId }
        var dropped = emptyList<SavedConversation>()
        val isSaved = store.update { current ->
            val all = current.orEmpty()
            val earlierSummary = all.firstOrNull(isSame)?.summary
            val saved = if (conversation.summary == null && earlierSummary != null) conversation.copy(summary = earlierSummary, isSummaryEarlier = true) else conversation
            val others = all.filterNot(isSame)
            val ofProject = (others.filter { it.project == conversation.project } + saved).sortedByDescending { it.updatedAt }
            dropped = ofProject.drop(conversationsPerProject)
            others.filterNot { it.project == conversation.project } + ofProject.take(conversationsPerProject)
        }
        if (!isSaved) return
        if (messages != null && dropped.none(isSame)) texts?.write(conversation.agentId, conversation.id, messages)
        dropped.forEach { texts?.delete(it.agentId, it.id) }
    }

    /**
     * Forgets the conversation [id] about [project] held by the agent [agentId].
     */
    @Synchronized
    internal fun remove(project: File, agentId: String, id: String) {
        store.update { current -> current.orEmpty().filterNot { it.project == project.absolutePath && it.agentId == agentId && it.id == id } }
        texts?.delete(agentId, id)
    }

    private fun read() = store.read().valueOrNull().orEmpty()

    public companion object {
        /**
         * The store keeping the conversations in Creator's settings folder.
         */
        public fun default(): SavedConversationStore = SavedConversationStore(
            file = File(settingsDirectory(), "conversations.json"),
            texts = ConversationTextStore(File(settingsDirectory(), "conversations")),
        )
    }
}
