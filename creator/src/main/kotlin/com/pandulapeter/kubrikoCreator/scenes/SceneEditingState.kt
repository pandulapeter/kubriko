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
     * @param revision Changes every time the Scene is (re)opened, so that the editor starts over.
     * @param isOutdated Whether an Actor type changed since the project's classes were loaded, which a reload picks up.
     */
    data class Open(
        val scene: ProjectResource,
        val entryPoint: CreatorEntryPoint,
        val revision: Int,
        val isOutdated: Boolean = false,
    ) : SceneEditingState
}
