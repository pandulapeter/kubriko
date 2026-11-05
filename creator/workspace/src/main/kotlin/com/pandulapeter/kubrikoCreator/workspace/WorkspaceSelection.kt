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

import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore

/**
 * What the workspace's center shows: one type, one Scene (being prepared, open, or failed), one page of the screenplay, one
 * other Markdown document, or nothing.
 */
public sealed interface WorkspaceSelection {

    /**
     * Nothing is shown.
     */
    public data object None : WorkspaceSelection

    /**
     * The block type [name] is shown.
     */
    public data class BlockType(
        public val name: String,
    ) : WorkspaceSelection

    /**
     * The Scene [name] is shown, whether it is being prepared, open, or failed.
     */
    public data class Scene(
        public val name: String,
    ) : WorkspaceSelection

    /**
     * The page [name] of the screenplay is shown.
     */
    public data class Page(
        public val name: String,
    ) : WorkspaceSelection

    /**
     * The document at [path] (from the project's root) is shown in the page editor.
     */
    public data class Document(
        public val path: String,
    ) : WorkspaceSelection
}

/**
 * The name of the Scene this state shows, whether it is being prepared, open, or failed, or null when it is closed.
 */
internal val SceneEditingState.sceneName: String?
    get() = when (this) {
        SceneEditingState.Closed -> null
        is SceneEditingState.Preparing -> SceneStore.nameOf(scene.file)
        is SceneEditingState.Failed -> SceneStore.nameOf(scene.file)
        is SceneEditingState.Unsupported -> SceneStore.nameOf(scene.file)
        is SceneEditingState.Open -> sceneName
    }
