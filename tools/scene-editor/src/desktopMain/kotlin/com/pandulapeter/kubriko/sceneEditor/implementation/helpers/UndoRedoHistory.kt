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
import kotlin.reflect.KClass

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
     * [actorIds] holds the editor's id of every serialized actor, in order, so the selection can be restored.
     */
    class SceneSnapshot(
        val serializedScene: String,
        val isSceneModified: Boolean,
        val actorIds: LongArray,
    )

    companion object {
        private const val MAX_STACK_SIZE = 25
    }
}

/**
 * Returns the index of the restored actor that had [selectedId] when the snapshot was taken, or null when nothing
 * was selected, the actor did not exist yet, or the restored list no longer lines up with [snapshotIds].
 */
internal fun restoredSelectionIndex(
    selectedId: Long?,
    snapshotIds: LongArray,
    restoredCount: Int,
): Int? {
    if (selectedId == null || restoredCount != snapshotIds.size) {
        return null
    }
    return snapshotIds.indexOf(selectedId).takeIf { it >= 0 }
}

/**
 * Returns the index of the actor in [actorClasses] that adding an actor of [newClass] replaces (the engine keeps
 * only the latest [com.pandulapeter.kubriko.actor.traits.Unique] actor of a class), or -1 when nothing is replaced.
 */
internal fun indexOfReplacedUnique(
    actorClasses: List<KClass<*>>,
    newClass: KClass<*>,
    isUnique: Boolean,
) = if (isUnique) actorClasses.indexOf(newClass) else -1
