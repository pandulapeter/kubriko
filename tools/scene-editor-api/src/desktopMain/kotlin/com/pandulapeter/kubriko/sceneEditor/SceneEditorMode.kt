/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor

/**
 * Defines the operational mode of the Scene Editor.
 */
sealed class SceneEditorMode {

    /**
     * Standalone mode where the editor manages its own scene state.
     */
    data object Normal : SceneEditorMode()

    /**
     * Connected mode where the editor is linked to an external scene state provider.
     * This is designed for real-time updates.
     *
     * @param sceneJson The current scene state in JSON format.
     * @param onSceneJsonChanged Callback when the editor modifies the scene state.
     */
    data class Connected(
        val sceneJson: String,
        val onSceneJsonChanged: (String) -> Unit,
    ) : SceneEditorMode()
}