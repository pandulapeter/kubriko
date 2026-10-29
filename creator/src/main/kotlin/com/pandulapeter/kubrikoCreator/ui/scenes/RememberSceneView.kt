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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingState
import com.pandulapeter.kubrikoCreator.scenes.SceneView

/**
 * The view that draws [state]'s Scene. When the Scene gets other classes ([SceneEditingState.Open.revision]), the view
 * of the new ones creates its Actors while the old one is still shown, and takes over its camera once it is ready, so
 * that swapping classes looks like nothing more than the Actors changing. Each view is disposed once it is gone.
 *
 * Call it before the Composables that draw the view, so that their viewport is torn down before the instance it draws.
 */
@Composable
internal fun rememberSceneView(state: SceneEditingState.Open): SceneView {
    var shown by remember(state.scene.file) { mutableStateOf<SceneView?>(null) }
    val next = remember(state.revision) { SceneView(state.entryPoint) }
    LaunchedEffect(next) {
        shown?.let { previous ->
            next.load(state.actors)
            next.camera = previous.camera
        }
        shown = next
    }
    DisposableEffect(next) {
        onDispose { if (next !== shown) next.dispose() }
    }
    val view = shown ?: next
    DisposableEffect(view) {
        onDispose { view.dispose() }
    }
    return view
}
