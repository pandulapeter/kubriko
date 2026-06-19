/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UndoRedoHistory {

    private val undoSnapshots = ArrayDeque<SceneSnapshot>()
    private val redoSnapshots = ArrayDeque<SceneSnapshot>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    fun recordAction(snapshot: SceneSnapshot) {
        undoSnapshots.addLast(snapshot)
        if (undoSnapshots.size > MAX_STACK_SIZE) {
            undoSnapshots.removeFirst()
        }
        redoSnapshots.clear()
        _canUndo.value = true
        _canRedo.value = false
    }

    fun performUndo(currentSnapshot: SceneSnapshot): SceneSnapshot? {
        if (undoSnapshots.isEmpty()) {
            return null
        }
        redoSnapshots.addLast(currentSnapshot)
        val snapshot = undoSnapshots.removeLast()
        _canUndo.value = undoSnapshots.isNotEmpty()
        _canRedo.value = true
        return snapshot
    }

    fun performRedo(currentSnapshot: SceneSnapshot): SceneSnapshot? {
        if (redoSnapshots.isEmpty()) {
            return null
        }
        undoSnapshots.addLast(currentSnapshot)
        if (undoSnapshots.size > MAX_STACK_SIZE) {
            undoSnapshots.removeFirst()
        }
        val snapshot = redoSnapshots.removeLast()
        _canRedo.value = redoSnapshots.isNotEmpty()
        _canUndo.value = true
        return snapshot
    }

    fun reset() {
        undoSnapshots.clear()
        redoSnapshots.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    /**
     * A point-in-time copy of the scene. [isSceneModified] is part of the snapshot so that undoing or
     * redoing also restores the unsaved-changes state, keeping the Save button in sync with the history.
     */
    data class SceneSnapshot(
        val serializedScene: String,
        val isSceneModified: Boolean,
    )

    companion object {
        private const val MAX_STACK_SIZE = 25
    }
}
