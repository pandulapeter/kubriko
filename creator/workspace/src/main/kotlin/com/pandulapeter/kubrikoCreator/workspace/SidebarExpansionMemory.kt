/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Which of the sidebar's modules, groups, and folders the user opened or closed, by the sidebar's own keys, kept for the
 * project on this computer in `.kubriko/local/sidebar.json`, so the sidebar opens as it was left. What is kept is read as
 * soon as the project opens, and what is saved meanwhile is written only after that, so it never replaces what wasn't read yet.
 */
public class SidebarExpansionMemory internal constructor(
    rootDirectory: File,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
) {
    private val store = LocalProjectStore(rootDirectory, FILE_NAME, LocalSidebarExpansion.serializer())
    private val saves = Channel<Map<String, Boolean>>(Channel.CONFLATED)
    private val _saved = MutableStateFlow<Map<String, Boolean>?>(null)

    /**
     * The choices kept from earlier sessions (empty when there are none, or when the file can't be read), or null until
     * they are read.
     */
    public val saved: StateFlow<Map<String, Boolean>?> = _saved.asStateFlow()

    init {
        scope.launch(ioDispatcher) {
            var written = store.read()?.choices.orEmpty()
            _saved.value = written
            for (choices in saves) {
                if (choices != written && store.write(LocalSidebarExpansion(choices))) written = choices
            }
        }
    }

    /**
     * Keeps [choices] in place of what was kept before.
     */
    public fun save(choices: Map<String, Boolean>) {
        saves.trySend(choices)
    }

    internal companion object {
        const val FILE_NAME = "sidebar.json"
    }
}

/**
 * The sidebar's open and closed modules, groups, and folders, by the sidebar's keys, kept in `.kubriko/local/`.
 */
@Serializable
private data class LocalSidebarExpansion(
    @SerialName("choices") val choices: Map<String, Boolean> = emptyMap(),
)
