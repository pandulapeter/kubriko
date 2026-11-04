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
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesController
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingController
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * The only place that pairs selecting a type with closing the Scene shown, and opening a Scene or a page with deselecting
 * the rest, so that the workspace's center shows one of them at most.
 *
 * @param sceneOf The Scene file of the given name, or null if there is none.
 */
public class SelectionController internal constructor(
    private val blockTypes: BlockTypesController?,
    private val sceneEditing: SceneEditingController,
    private val sceneOf: (name: String) -> ProjectResource?,
    scope: CoroutineScope,
) {
    private val openPage = MutableStateFlow<String?>(null)

    /**
     * One value for the center, the sidebar highlight, and the AI panel. A shown Scene wins over a type name left behind.
     */
    public val current: StateFlow<WorkspaceSelection> = combine(blockTypes?.selectedName ?: flowOf(null), sceneEditing.state, openPage, ::selectionOf)
        .stateIn(scope, SharingStarted.Eagerly, selectionOf(blockTypes?.selectedName?.value, sceneEditing.state.value, openPage.value))

    /**
     * Shows the type [name], closing the Scene shown.
     *
     * @return False, changing nothing, when there is no such type.
     */
    public fun selectType(name: String): Boolean {
        if (blockTypes?.get(name) == null) return false
        sceneEditing.close()
        openPage.value = null
        blockTypes.select(name)
        return true
    }

    /**
     * Shows the Scene [name] unless it is shown already, deselecting the type.
     *
     * @return False, changing nothing, when there is no such Scene file.
     */
    public fun openScene(name: String): Boolean {
        val scene = sceneOf(name) ?: return false
        if (sceneEditing.state.value.sceneName == name) return true
        blockTypes?.select(null)
        openPage.value = null
        sceneEditing.open(scene)
        return true
    }

    /**
     * Shows the page [name] of the screenplay, closing the Scene and deselecting the type.
     */
    public fun openPage(name: String) {
        sceneEditing.close()
        blockTypes?.select(null)
        openPage.value = name
    }

    /**
     * Shows nothing: closes the Scene and deselects the type.
     */
    public fun clear() {
        sceneEditing.close()
        blockTypes?.select(null)
        openPage.value = null
    }

    private fun selectionOf(selectedName: String?, state: SceneEditingState, page: String?): WorkspaceSelection =
        state.sceneName?.let(WorkspaceSelection::Scene) ?: page?.let(WorkspaceSelection::Page) ?: selectedName?.let(WorkspaceSelection::BlockType) ?: WorkspaceSelection.None
}
