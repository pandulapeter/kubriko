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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The lines of the workspace console, oldest first, and the recordings the AI's action tools take of them.
 */
internal class ConsoleLog {
    private val _entries = MutableStateFlow(emptyList<ConsoleEntry>())
    val entries = _entries.asStateFlow()
    private val recordings = CopyOnWriteArrayList<MutableList<ConsoleEntry>>()

    fun append(entry: ConsoleEntry) {
        _entries.update { (it + entry).takeLast(CONSOLE_SIZE) }
        recordings.forEach { it += entry }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    /**
     * Runs [action], and returns its result with the lines appended meanwhile.
     */
    suspend fun <T> record(action: suspend () -> T): Pair<T, List<ConsoleEntry>> {
        val recording = Collections.synchronizedList(mutableListOf<ConsoleEntry>())
        recordings += recording
        return try {
            val result = action()
            result to synchronized(recording) { recording.toList() }
        } finally {
            recordings.removeIf { it === recording }
        }
    }

    private companion object {
        const val CONSOLE_SIZE = 2000
    }
}
