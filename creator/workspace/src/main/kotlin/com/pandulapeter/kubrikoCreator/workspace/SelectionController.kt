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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The only place that pairs selecting a type with closing the Scene shown, and opening a Scene, a page, or a document with deselecting
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
    private val openText = MutableStateFlow<WorkspaceSelection?>(null)
    private val _revealed = MutableStateFlow<Reveal?>(null)

    /**
     * The place the center lights up in what it shows, if one was asked for, until something else is shown.
     */
    public val revealed: StateFlow<Reveal?> = _revealed.asStateFlow()

    /**
     * What is asked before the center shows something else, the project closes, or Creator quits, while what it shows
     * holds changes not saved yet (the sprite editor's): it calls the given continuation once they are saved or
     * discarded, or never when the user stays. Null while there is nothing to ask about.
     */
    public var leaveGuard: ((proceed: () -> Unit) -> Unit)? = null

    /**
     * Whether leaving what the center shows would ask first ([leaveGuard]), so that following the AI's steps doesn't.
     */
    public val isLeaveGuarded: Boolean get() = leaveGuard != null

    /**
     * Runs [proceed] once leaving what the center shows is fine: at once, or after [leaveGuard] asked.
     */
    public fun leaving(proceed: () -> Unit) {
        leaveGuard?.invoke(proceed) ?: proceed()
    }

    /**
     * One value for the center, the sidebar highlight, and the AI panel. A shown Scene wins over a type name left behind.
     */
    public val current: StateFlow<WorkspaceSelection> = combine(blockTypes?.selectedName ?: flowOf(null), sceneEditing.state, openText, ::selectionOf)
        .stateIn(scope, SharingStarted.Eagerly, selectionOf(blockTypes?.selectedName?.value, sceneEditing.state.value, openText.value))

    init {
        // The center can change without this controller (a deleted type deselects itself), and a place left behind
        // would light up again when its type or Scene is shown next.
        scope.launch { current.collect { selection -> if (_revealed.value?.place?.isIn(selection) == false) _revealed.value = null } }
    }

    /**
     * Shows the type [name], closing the Scene shown, and lights up [place] in it, if given.
     *
     * @return False, changing nothing, when there is no such type.
     */
    public fun selectType(name: String, place: RevealedPlace.InType? = null): Boolean {
        if (blockTypes?.get(name) == null) return false
        leaving {
            sceneEditing.close()
            openText.value = null
            blockTypes.select(name)
            _revealed.value = place?.takeIf { it.type == name }?.let(::Reveal)
        }
        return true
    }

    /**
     * Shows the Scene [name] unless it is shown already, deselecting the type, and lights up the Actors of [actorIds] in
     * it, if any.
     *
     * @return False, changing nothing, when there is no such Scene file.
     */
    public fun openScene(name: String, actorIds: List<String> = emptyList()): Boolean {
        val scene = sceneOf(name) ?: return false
        val reveal = actorIds.takeIf { it.isNotEmpty() }?.let { Reveal(RevealedPlace.InScene(name, it)) }
        if (sceneEditing.state.value.sceneName == name) {
            _revealed.value = reveal
            return true
        }
        leaving {
            blockTypes?.select(null)
            openText.value = null
            sceneEditing.open(scene)
            _revealed.value = reveal
        }
        return true
    }

    /**
     * Shows the page [name] of the screenplay, closing the Scene and deselecting the type.
     */
    public fun openPage(name: String): Unit = openText(WorkspaceSelection.Page(name))

    /**
     * Shows the document at [path] in the page editor, closing the Scene and deselecting the type.
     */
    public fun openDocument(path: String): Unit = openText(WorkspaceSelection.Document(path))

    /**
     * Shows the sprite at [path] (from `composeResources`) in the sprite editor, closing the Scene and deselecting the type.
     */
    public fun openSprite(path: String): Unit = openText(WorkspaceSelection.Sprite(path))

    /**
     * Shows nothing: closes the Scene and deselects the type.
     */
    public fun clear(): Unit = leaving {
        sceneEditing.close()
        blockTypes?.select(null)
        openText.value = null
        _revealed.value = null
    }

    private fun openText(selection: WorkspaceSelection) {
        if (current.value == selection) return
        leaving {
            sceneEditing.close()
            blockTypes?.select(null)
            openText.value = selection
            _revealed.value = null
        }
    }

    private fun selectionOf(selectedName: String?, state: SceneEditingState, text: WorkspaceSelection?): WorkspaceSelection =
        state.sceneName?.let(WorkspaceSelection::Scene) ?: text ?: selectedName?.let(WorkspaceSelection::BlockType) ?: WorkspaceSelection.None

    private fun RevealedPlace.isIn(selection: WorkspaceSelection) = when (this) {
        is RevealedPlace.InType -> selection == WorkspaceSelection.BlockType(type)
        is RevealedPlace.InScene -> selection == WorkspaceSelection.Scene(scene)
    }
}
