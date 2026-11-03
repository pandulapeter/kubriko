/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneFileProblem
import com.pandulapeter.kubrikoCreator.scenes.SceneStore

/**
 * The Scene shown in Kubriko Creator's own window, if any.
 */
public sealed interface SceneEditingState {

    /**
     * No Scene is open.
     */
    public data object Closed : SceneEditingState

    /**
     * The project is being compiled and its classes loaded.
     */
    public data class Preparing(
        val scene: ProjectResource,
    ) : SceneEditingState

    /**
     * The Scene couldn't be opened: the project didn't build, its entry point couldn't be loaded, or its code threw while
     * the Scene's view was being created.
     *
     * @param reason What went wrong, for the user.
     * @param arguments The values of the [reason]'s placeholders.
     */
    public data class Failed(
        val scene: ProjectResource,
        val reason: SceneProblem,
        val arguments: List<String> = emptyList(),
    ) : SceneEditingState

    /**
     * The project's Kubriko version isn't Creator's, or its Compose or Kotlin is newer, so its classes can't be drawn in
     * Creator's window.
     */
    public data class Unsupported(
        val scene: ProjectResource,
        val reason: SceneProblem,
        val arguments: List<String>,
    ) : SceneEditingState

    /**
     * @param actors The Scene's Actors, read again whenever a command changes the Scene.
     * @param revision Changes every time the Scene gets other classes, opened again or swapped to newer ones, so that
     *   the editor creates its Actors again.
     * @param isOutdated Whether the classes couldn't be built again after a change, so they are older than the sources.
     * @param isUpdating Whether newer classes are being built, which the Scene swaps to when they are ready.
     * @param previews The looks of the block-built Actor types that changed since the classes were built (or are new),
     *   whose Actors the editor draws from them until newer classes are swapped in.
     * @param problem Why the Scene file can't be read, which no command can change until it is fixed by hand.
     */
    public data class Open(
        val scene: ProjectResource,
        val entryPoint: CreatorEntryPoint,
        val actors: List<SceneActorEntry>,
        val revision: Int,
        val isOutdated: Boolean = false,
        val isUpdating: Boolean = false,
        val previews: Map<String, ActorTypeLook> = emptyMap(),
        val problem: SceneFileProblem? = null,
    ) : SceneEditingState {
        /**
         * The Scene's name, from its file's.
         */
        public val sceneName: String get() = SceneStore.nameOf(scene.file)
    }
}
