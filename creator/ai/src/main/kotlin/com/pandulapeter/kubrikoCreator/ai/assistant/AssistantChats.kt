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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The assistant's conversations about the open project: the ones open, the one shown, and the earlier ones [store] keeps,
 * which can be continued.
 *
 * @param scope Where the lists change: the main thread, which the panel reads them on.
 */
internal class AssistantChats(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val store: SavedConversationStore?,
) {
    private val _open = MutableStateFlow(emptyList<AssistantChat>())

    /**
     * The conversations open, oldest first.
     */
    val open = _open.asStateFlow()
    private val _current = MutableStateFlow<AssistantChat?>(null)

    /**
     * The conversation the panel shows.
     */
    val current = _current.asStateFlow()
    private val _saved = MutableStateFlow(emptyList<SavedConversation>())

    /**
     * The earlier conversations about the project with the agent chosen, newest first, which can be continued.
     */
    val saved = _saved.asStateFlow()

    /**
     * Opens [chat] and shows it.
     */
    fun add(chat: AssistantChat) {
        _open.update { it + chat }
        _current.value = chat
    }

    fun select(chat: AssistantChat) {
        if (chat in _open.value) _current.value = chat
    }

    /**
     * Ends [chat] and forgets it for good, [store] included, showing the open conversation that was talked in last. It is
     * forgotten only once it ended, since a turn ending meanwhile would keep it again.
     */
    fun delete(chat: AssistantChat, project: File?, agentId: String) {
        if (chat !in _open.value) return
        _open.update { it - chat }
        if (_current.value === chat) _current.value = _open.value.maxByOrNull { it.lastMessageAt.value }
        scope.launch {
            chat.close()
            val id = chat.conversationId.value ?: return@launch
            if (store != null && project != null) withContext(ioDispatcher) { store.remove(project, agentId, id) }
        }
    }

    /**
     * Leaves the earlier conversation [id] out of [saved], once it is being continued.
     */
    fun onResumed(id: String) {
        _saved.update { saved -> saved.filterNot { it.id == id } }
    }

    /**
     * Forgets every open conversation and returns them, for ending them.
     */
    fun takeAll(): List<AssistantChat> {
        val all = _open.value
        _open.value = emptyList()
        _current.value = null
        return all
    }

    /**
     * Reads the earlier conversations about [project] with the agent [agentId] again, leaving out the open ones.
     */
    fun refreshSaved(project: File?, agentId: String) {
        if (store == null || project == null) {
            _saved.value = emptyList()
            return
        }
        scope.launch {
            val open = _open.value.mapNotNull { it.conversationId.value }.toSet()
            _saved.value = withContext(ioDispatcher) { store.of(project, agentId) }.filter { it.id !in open }
        }
    }
}
