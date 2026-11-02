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

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingController
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * The only place that pairs selecting a type with closing the Scene shown, and opening a Scene with deselecting the type,
 * so that the workspace's center shows one of them at most.
 *
 * @param sceneOf The Scene file of the given name, or null if there is none.
 */
internal class SelectionController(
    private val blockTypes: BlockTypesController?,
    private val sceneEditing: SceneEditingController,
    private val sceneOf: (name: String) -> ProjectResource?,
    scope: CoroutineScope,
) {
    /**
     * One value for the center, the sidebar highlight, and the AI panel. A shown Scene wins over a type name left behind.
     */
    val current: StateFlow<WorkspaceSelection> = combine(blockTypes?.selectedName ?: flowOf(null), sceneEditing.state, ::selectionOf)
        .stateIn(scope, SharingStarted.Eagerly, selectionOf(blockTypes?.selectedName?.value, sceneEditing.state.value))

    /**
     * Shows the type [name], closing the Scene shown.
     *
     * @return False, changing nothing, when there is no such type.
     */
    fun selectType(name: String): Boolean {
        if (blockTypes?.get(name) == null) return false
        sceneEditing.close()
        blockTypes.select(name)
        return true
    }

    /**
     * Shows the Scene [name] unless it is shown already, deselecting the type.
     *
     * @return False, changing nothing, when there is no such Scene file.
     */
    fun openScene(name: String): Boolean {
        val scene = sceneOf(name) ?: return false
        if (sceneEditing.state.value.sceneName == name) return true
        blockTypes?.select(null)
        sceneEditing.open(scene)
        return true
    }

    /**
     * Shows nothing: closes the Scene and deselects the type.
     */
    fun clear() {
        sceneEditing.close()
        blockTypes?.select(null)
    }

    private fun selectionOf(selectedName: String?, state: SceneEditingState): WorkspaceSelection =
        state.sceneName?.let(WorkspaceSelection::Scene) ?: selectedName?.let(WorkspaceSelection::BlockType) ?: WorkspaceSelection.None
}
