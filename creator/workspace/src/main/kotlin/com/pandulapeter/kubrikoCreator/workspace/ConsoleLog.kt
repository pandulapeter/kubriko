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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The lines of the workspace console, oldest first, at most [capacity] of them, and the recordings the AI's action tools
 * take of them. A game may log every frame, from any thread, so adding a line copies nothing: the lines live in an
 * array that later lines only ever go after, and each [lines] shows a part of it that never changes.
 */
internal class ConsoleLog(private val capacity: Int = CONSOLE_SIZE) {
    private val lock = Any()
    private var storage = newStorage()
    private var start = 0
    private var end = 0
    private var revision = 0L
    private var addedCount = 0L
    private val _lines = MutableStateFlow(ConsoleLines())
    val lines: StateFlow<ConsoleLines> = _lines.asStateFlow()
    private val recordings = CopyOnWriteArrayList<MutableList<ConsoleEntry>>()

    fun append(entry: ConsoleEntry) {
        synchronized(lock) {
            if (end == storage.size) {
                // The lines shown so far keep the old array, so the ones kept move to a new one rather than within it.
                storage = newStorage().also { System.arraycopy(storage, start, it, 0, end - start) }
                end -= start
                start = 0
            }
            storage[end++] = entry
            addedCount++
            if (end - start > capacity) start++
            _lines.value = ConsoleLines(++revision, ConsoleWindow(storage, start, end), firstNumber = addedCount - (end - start))
        }
        recordings.forEach { it += entry }
    }

    fun clear() {
        synchronized(lock) {
            storage = newStorage()
            start = 0
            end = 0
            _lines.value = ConsoleLines(++revision, firstNumber = addedCount)
        }
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

    private fun newStorage() = arrayOfNulls<ConsoleEntry>(capacity * 2)

    /**
     * The lines from [start] to [end] of [storage], which nothing writes to again.
     */
    private class ConsoleWindow(
        private val storage: Array<ConsoleEntry?>,
        private val start: Int,
        private val end: Int,
    ) : AbstractList<ConsoleEntry>(), RandomAccess {
        override val size get() = end - start

        override fun get(index: Int): ConsoleEntry {
            if (index !in 0..<size) throw IndexOutOfBoundsException("Index $index of $size lines.")
            return checkNotNull(storage[start + index])
        }
    }

    private companion object {
        const val CONSOLE_SIZE = 2000
    }
}
