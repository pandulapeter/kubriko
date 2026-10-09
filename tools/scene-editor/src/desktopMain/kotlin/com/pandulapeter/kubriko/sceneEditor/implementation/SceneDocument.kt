/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.UndoRedoHistory
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.indexOfReplacedUnique
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.restoredSelectionIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.IdentityHashMap

/**
 * The edited scene: its actors, the undo/redo history and the unsaved-changes flag. Main thread only.
 *
 * The scene's actors reach the game instance through [addActors] and [removeActors].
 */
internal class SceneDocument(
    private val serialize: (List<Editable<*>>) -> String,
    private val deserialize: (String) -> List<Editable<*>>,
    private val addActors: (List<Editable<*>>) -> Unit,
    private val removeActors: (List<Editable<*>>) -> Unit,
) {
    private val undoRedoHistory = UndoRedoHistory()
    val canUndo = undoRedoHistory.canUndo
    val canRedo = undoRedoHistory.canRedo
    private val _isSceneModified = MutableStateFlow(false)
    val isSceneModified = _isSceneModified.asStateFlow()
    private var pendingPropertyEditKey: Any? = null
    private val sceneActors = mutableListOf<Editable<*>>()
    private val sceneActorIds = IdentityHashMap<Editable<*>, Long>()
    private var nextSceneActorId = 0L

    fun serializeScene() = serialize(sceneActors)

    /**
     * Restores the state before the last change. [onSelectionRestored] receives the actor to select in the restored
     * scene: the one with the id of [selectedActor], if it is still there.
     */
    fun undo(selectedActor: Editable<*>?, onSelectionRestored: (Editable<*>?) -> Unit) {
        undoRedoHistory.performUndo(takeSnapshot())?.let { restoreSnapshot(it, selectedActor, onSelectionRestored) }
        pendingPropertyEditKey = null
    }

    /**
     * Reapplies the last undone change; [onSelectionRestored] works as for [undo].
     */
    fun redo(selectedActor: Editable<*>?, onSelectionRestored: (Editable<*>?) -> Unit) {
        undoRedoHistory.performRedo(takeSnapshot())?.let { restoreSnapshot(it, selectedActor, onSelectionRestored) }
        pendingPropertyEditKey = null
    }

    /**
     * Records the pre-change state of the scene before a property of the selected actor is edited.
     * Consecutive edits sharing the same [editKey] are coalesced into a single undo step, so dragging a
     * slider or typing into a field does not flood the history.
     */
    fun onBeforePropertyChange(editKey: Any) {
        if (editKey != pendingPropertyEditKey) {
            undoRedoHistory.recordAction(takeSnapshot())
            pendingPropertyEditKey = editKey
        }
    }

    fun clearPendingPropertyEdit() {
        pendingPropertyEditKey = null
    }

    fun recordSnapshot() {
        undoRedoHistory.recordAction(takeSnapshot())
        pendingPropertyEditKey = null
    }

    private fun takeSnapshot() = UndoRedoHistory.SceneSnapshot(
        serializedScene = serialize(sceneActors),
        isSceneModified = _isSceneModified.value,
        actorIds = LongArray(sceneActors.size) { sceneActorIds.getValue(sceneActors[it]) },
    )

    private fun restoreSnapshot(
        snapshot: UndoRedoHistory.SceneSnapshot,
        selectedActor: Editable<*>?,
        onSelectionRestored: (Editable<*>?) -> Unit,
    ) {
        val selectedId = selectedActor?.let(sceneActorIds::get)
        val restoredActors = deserialize(snapshot.serializedScene)
        val ids = snapshot.actorIds.takeIf { it.size == restoredActors.size }
        clearSceneActors()
        restoredActors.forEachIndexed { index, actor -> trackSceneActor(actor, ids?.get(index) ?: nextSceneActorId++) }
        addActors(restoredActors)
        onSelectionRestored(restoredSelectionIndex(selectedId, snapshot.actorIds, restoredActors.size)?.let(restoredActors::get))
        _isSceneModified.update { snapshot.isSceneModified }
    }

    fun markSceneAsModified() = _isSceneModified.update { true }

    fun onSceneReplaced() {
        undoRedoHistory.reset()
        _isSceneModified.update { false }
        pendingPropertyEditKey = null
    }

    fun onSceneSaved() {
        _isSceneModified.update { false }
        pendingPropertyEditKey = null
    }

    fun replaceSceneActors(actors: List<Editable<*>>) {
        clearSceneActors()
        actors.forEach { trackSceneActor(it) }
        addActors(actors)
    }

    fun addSceneActor(actor: Editable<*>) {
        trackSceneActor(actor)
        addActors(listOf(actor))
    }

    fun removeSceneActor(actor: Editable<*>) {
        sceneActors.remove(actor)
        sceneActorIds.remove(actor)
        removeActors(listOf(actor))
    }

    /**
     * Records [actor] as part of the scene. The scene is tracked here rather than read back from [ActorManager],
     * whose batched updates lag behind the editor's own actions. A [Unique] actor replaces the tracked one of the
     * same class, mirroring what [ActorManager] does with the actors themselves.
     */
    private fun trackSceneActor(actor: Editable<*>, id: Long = nextSceneActorId++) {
        val replacedIndex = indexOfReplacedUnique(sceneActors.map { it::class }, actor::class, actor is Unique)
        if (replacedIndex >= 0) {
            sceneActorIds.remove(sceneActors.removeAt(replacedIndex))
        }
        sceneActors.add(actor)
        sceneActorIds[actor] = id
    }

    fun clearSceneActors() {
        removeActors(sceneActors.toList())
        sceneActors.clear()
        sceneActorIds.clear()
    }
}
