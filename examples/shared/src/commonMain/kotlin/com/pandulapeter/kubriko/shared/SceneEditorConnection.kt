/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Ties an example's in-game "Editor" button to its desktop scene editor window. The Showcase creates one per example and
 * keeps it for the whole process, so an open editor window survives the example's [StateHolder] being disposed.
 */
class SceneEditorConnection {
    private val _isVisible = MutableStateFlow(false)

    /** Whether the scene editor window is open. */
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    /**
     * The scene the editor and the running example exchange in the editor's connected mode: the example publishes the
     * scene it loaded, the editor publishes every edit, and the example reloads it. Blank until the example publishes.
     */
    val sceneJson = MutableStateFlow("")

    /** Opens the scene editor window if it is closed, and closes it otherwise. */
    fun toggle() = _isVisible.update { !it }

    /** Closes the scene editor window. */
    fun close() = _isVisible.update { false }
}
