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
 * The Actor selected in the Scene editor, and the one an undo or redo brought back ([restore]), which is selected once the
 * Scene shows it, as a placed one is. Selecting an Actor cancels a restored one still waiting.
 */
internal class SceneSelection {
    var selectedId by mutableStateOf<String?>(null)
        private set
    var restoredId by mutableStateOf<String?>(null)
        private set

    fun select(actorId: String?) {
        selectedId = actorId
        if (actorId != null) restoredId = null
    }

    fun restore(actorId: String) {
        restoredId = actorId
    }

    fun dropRestored() {
        restoredId = null
    }

    fun selectRestored(actorId: String) {
        selectedId = actorId
        restoredId = null
    }
}

/**
 * The selection of [state]'s Scene, following what [broughtBack] reports from the moment the Scene opens, even while it
 * is still prepared.
 */
@Composable
internal fun rememberSceneSelection(state: SceneEditingState.Open, broughtBack: Flow<ActorsBroughtBack>): SceneSelection {
    val selection = remember(state.scene.file) { SceneSelection() }
    LaunchedEffect(selection, broughtBack, state.sceneName) {
        broughtBack.collect { actors -> restoredSelectionOf(actors, state.sceneName)?.let(selection::restore) }
    }
    return selection
}

/**
 * Selects the Actor brought back into [view] once it shows it, unless it is the Scene settings, which are never selected.
 * One the Scene doesn't show by its next load (removed again meanwhile) is dropped, so it can't take the selection later.
 */
@Composable
internal fun SelectingRestoredActor(selection: SceneSelection, view: SceneView) {
    val restoredId = selection.restoredId
    LaunchedEffect(view, restoredId) {
        if (restoredId == null) return@LaunchedEffect
        if (view.entryOf(restoredId) == null) {
            val revision = view.loadRevision.value
            view.loadRevision.first { it != revision }
        }
        when {
            view.entryOf(restoredId) == null || view.isBackground(restoredId) -> selection.dropRestored()
            else -> selection.selectRestored(restoredId)
        }
    }
}

/**
 * The ID of the Actor the Scene editor of the Scene called [sceneName] selects once an undo or redo brought
 * [broughtBack] back: the one Actor brought back into it, or null when the step brought back several, or none into this
 * Scene.
 */
internal fun restoredSelectionOf(broughtBack: ActorsBroughtBack, sceneName: String): String? = broughtBack.actors.singleOrNull()?.id?.takeIf { broughtBack.scene == sceneName }
