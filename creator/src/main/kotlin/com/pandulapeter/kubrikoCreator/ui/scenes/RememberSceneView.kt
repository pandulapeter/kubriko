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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.sceneRuntime.createDisposingIfCancelled
import com.pandulapeter.kubrikoCreator.sceneRuntime.disposeEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers

/**
 * The view that draws [state]'s Scene, or null while the first one is being created. Creating a view runs the project's
 * code (its Managers' constructors), so it happens off the main thread, and what that code throws goes to [onFailed]
 * instead of out of composition, with whether a view of older classes is still shown.
 *
 * When the Scene gets other classes ([SceneEditingState.Open.revision]), the view of the new ones creates its Actors
 * while the old one is still shown, and takes over its camera once it is ready, so that swapping classes looks like
 * nothing more than the Actors changing. Each view is disposed once it is gone.
 *
 * Call it before the Composables that draw the view, so that their viewport is torn down before the instance it draws.
 */
@Composable
internal fun rememberSceneView(
    state: SceneEditingState.Open,
    onFailed: (exception: Throwable, isShowingOlderClasses: Boolean) -> Unit,
): SceneView? {
    var shown by remember(state.scene.file) { mutableStateOf<SceneView?>(null) }
    val currentOnFailed by rememberUpdatedState(onFailed)
    LaunchedEffect(state.scene.file, state.revision) {
        val next = try {
            createDisposingIfCancelled(Dispatchers.Default, create = { SceneView(state.entryPoint) }, dispose = SceneView::dispose)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentOnFailed(exception, shown != null)
            return@LaunchedEffect
        } catch (error: LinkageError) {
            currentOnFailed(error, shown != null)
            return@LaunchedEffect
        }
        try {
            shown?.let { previous ->
                next.load(state.actors, state.previews)
                next.camera = previous.camera
            }
        } catch (exception: CancellationException) {
            disposeEach(listOf(next), SceneView::dispose)
            throw exception
        }
        shown = next
    }
    val view = shown
    DisposableEffect(view) {
        onDispose { view?.dispose() }
    }
    return view
}
