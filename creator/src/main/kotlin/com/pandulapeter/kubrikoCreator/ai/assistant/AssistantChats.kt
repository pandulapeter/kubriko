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
 * The assistant's conversations about the open project: the ones open, the one shown, the ones closed during this run,
 * and the earlier ones [store] keeps, which can be continued.
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
     * Conversations closed during this run, kept running so that continuing one needs no `session/load`, which the
     * agent's client refuses for an ID it still holds.
     */
    private val parked = mutableListOf<AssistantChat>()

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
     * Closes [chat], showing the one before it. A conversation with something in it is kept for continuing it later;
     * an empty one ends.
     */
    fun close(chat: AssistantChat, project: File?, agentId: String) {
        val index = _open.value.indexOf(chat)
        if (index < 0) return
        _open.update { it - chat }
        if (_current.value === chat) _current.value = _open.value.getOrNull((index - 1).coerceAtLeast(0))
        if (chat.conversationId.value != null && chat.transcript.value.title != null) {
            parked += chat
            refreshSaved(project, agentId)
        } else {
            scope.launch { chat.close() }
        }
    }

    /**
     * Opens again the conversation [id] closed during this run, if it was, and tells whether it was.
     */
    fun reopen(id: String): Boolean {
        _saved.update { saved -> saved.filterNot { it.id == id } }
        val chat = parked.firstOrNull { it.conversationId.value == id } ?: return false
        parked -= chat
        add(chat)
        return true
    }

    /**
     * Forgets every conversation, open or closed during this run, and returns them, for ending them.
     */
    fun takeAll(): List<AssistantChat> {
        val all = _open.value + parked
        _open.value = emptyList()
        parked.clear()
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
