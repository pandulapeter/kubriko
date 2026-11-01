/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import org.jetbrains.compose.resources.StringResource

/**
 * The project's revision, raised by every change a command makes (undoing and redoing included), and the newest of
 * those changes, so that the assistant can tell an agent what changed between two of its turns.
 */
internal class ChangeLog(
    private val capacity: Int = CAPACITY,
) {
    private val records = ArrayDeque<ChangeRecord>()

    /**
     * The revision after the newest change: 0 before any.
     */
    @Volatile
    var revision = 0
        private set

    @Synchronized
    fun record(title: StringResource, target: CommandTarget?, origin: AiStepOrigin? = null, isByAi: Boolean = false, step: ChangeRecord.Step? = null): Int {
        revision += 1
        records.addLast(ChangeRecord(revision, title, target, origin, isByAi, step))
        if (records.size > capacity) records.removeFirst()
        return revision
    }

    /**
     * The changes made after [revision] that are still kept, oldest first.
     */
    @Synchronized
    fun since(revision: Int) = records.filter { it.revision > revision }

    private companion object {
        const val CAPACITY = 200
    }
}
