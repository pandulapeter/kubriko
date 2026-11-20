/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * The Actors selected in the Scene editor, and those an undo or redo brought back ([restore]), which are selected once the
 * Scene shows them, as a placed one is. Selecting cancels Actors restored and still waiting.
 */
internal class SceneSelection {
    /**
     * The selected Actors, in the order they were selected.
     */
    var selectedIds by mutableStateOf<List<String>>(emptyList())
        private set
    var restoredIds by mutableStateOf<List<String>>(emptyList())
        private set

    /**
     * The selected Actor while only one is, which the inspector shows and the handles change.
     */
    val selectedId: String? get() = selectedIds.singleOrNull()

    fun select(actorId: String?) = select(listOfNotNull(actorId))

    fun select(actorIds: List<String>) {
        selectedIds = actorIds.distinct()
        if (actorIds.isNotEmpty()) restoredIds = emptyList()
    }

    /**
     * Takes the Actors [isKept] refuses out of the selection.
     */
    fun keepOnly(isKept: (String) -> Boolean) {
        if (selectedIds.any { !isKept(it) }) selectedIds = selectedIds.filter(isKept)
    }

    /**
     * Adds [actorId] to the selection, or takes it out if it is in it.
     */
    fun toggle(actorId: String) = select(if (actorId in selectedIds) selectedIds - actorId else selectedIds + actorId)

    fun restore(actorIds: List<String>) {
        restoredIds = actorIds
    }

    fun dropRestored() {
        restoredIds = emptyList()
    }

    fun selectRestored(actorIds: List<String>) {
        selectedIds = actorIds
        restoredIds = emptyList()
    }
}

/**
 * The selection of [state]'s Scene, following what [broughtBack] reports from the moment the Scene opens, even while it
 * is still prepared, and selecting the first Actor a [highlight] that selects names once the Scene shows it.
 */
@Composable
internal fun rememberSceneSelection(state: SceneEditingState.Open, broughtBack: Flow<ActorsBroughtBack>, highlight: SceneHighlight? = null): SceneSelection {
    val selection = remember(state.scene.file) { SceneSelection() }
    LaunchedEffect(selection, broughtBack, state.sceneName) {
        broughtBack.collect { actors -> restoredSelectionOf(actors, state.sceneName).takeIf { it.isNotEmpty() }?.let(selection::restore) }
    }
    LaunchedEffect(selection, highlight?.key) {
        highlight?.takeIf { it.isSelecting }?.actorIds?.firstOrNull()?.let { selection.restore(listOf(it)) }
    }
    return selection
}

/**
 * Selects the Actors brought back into [view] once it shows them, apart from the Scene settings, which are never
 * selected. Those the Scene doesn't show by its next load (removed again meanwhile) are dropped, so they can't take the
 * selection later.
 */
@Composable
internal fun SelectingRestoredActor(selection: SceneSelection, view: SceneView) {
    val restoredIds = selection.restoredIds
    LaunchedEffect(view, restoredIds) {
        if (restoredIds.isEmpty()) return@LaunchedEffect
        if (restoredIds.any { view.entryOf(it) == null }) {
            val revision = view.loadRevision.value
            view.loadRevision.first { it != revision }
        }
        val shown = restoredIds.filter { view.entryOf(it) != null && !view.isBackground(it) }
        if (shown.isEmpty()) selection.dropRestored() else selection.selectRestored(shown)
    }
}

/**
 * The IDs of the Actors the Scene editor of the Scene called [sceneName] selects once an undo or redo brought
 * [broughtBack] back: every Actor brought back into it, none when the step brought them back into another Scene.
 */
internal fun restoredSelectionOf(broughtBack: ActorsBroughtBack, sceneName: String): List<String> =
    if (broughtBack.scene == sceneName) broughtBack.actors.map { it.id } else emptyList()
