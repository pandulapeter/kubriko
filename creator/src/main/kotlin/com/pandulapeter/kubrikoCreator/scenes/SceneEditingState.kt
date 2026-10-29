/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import org.jetbrains.compose.resources.StringResource

/**
 * The Scene shown in Kubriko Creator's own window, if any.
 */
internal sealed interface SceneEditingState {

    data object Closed : SceneEditingState

    /**
     * The project is being compiled and its classes loaded.
     */
    data class Preparing(
        val scene: ProjectResource,
    ) : SceneEditingState

    /**
     * The Scene couldn't be opened: the project didn't build, or its entry point couldn't be loaded.
     *
     * @param reason What went wrong, for the user.
     */
    data class Failed(
        val scene: ProjectResource,
        val reason: StringResource,
    ) : SceneEditingState

    /**
     * The project's Kubriko version isn't Creator's, so its classes can't be drawn in Creator's window.
     */
    data class Unsupported(
        val scene: ProjectResource,
        val projectKubrikoVersion: String,
    ) : SceneEditingState

    /**
     * @param actors The Scene's Actors, read again whenever a command changes the Scene.
     * @param revision Changes every time the Scene gets other classes, opened again or swapped to newer ones, so that
     *   the editor creates its Actors again.
     * @param isOutdated Whether the classes couldn't be built again after a change, so they are older than the sources.
     * @param isUpdating Whether newer classes are being built, which the Scene swaps to when they are ready.
     */
    data class Open(
        val scene: ProjectResource,
        val entryPoint: CreatorEntryPoint,
        val actors: List<SceneActorEntry>,
        val revision: Int,
        val isOutdated: Boolean = false,
        val isUpdating: Boolean = false,
    ) : SceneEditingState {
        val sceneName get() = SceneStore.nameOf(scene.file)
    }
}
