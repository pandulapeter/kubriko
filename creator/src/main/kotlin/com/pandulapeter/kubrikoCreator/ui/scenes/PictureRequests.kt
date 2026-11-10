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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The pictures asked of a [SceneViewport]: a picture is the layer its next frame records, read on the thread that draws
 * it, since a layer is never recorded and read at once.
 */
@Stable
internal class PictureRequests(
    private val frameTimeout: Duration = 2.seconds,
) {
    /**
     * Whether the next frame is to be recorded, read while drawing, so that asking draws a frame.
     */
    var isPending by mutableStateOf(false)
        private set
    private var recorded: CompletableDeferred<Unit>? = null

    /**
     * The picture of the next frame [layer] records, read in [drawingScope], which runs on the thread that draws, or null
     * if no frame is drawn soon (the window is hidden, or the thread that draws is busy), the viewport has no size, or it
     * left the screen meanwhile, which ends [drawingScope].
     */
    suspend fun take(drawingScope: CoroutineScope, layer: GraphicsLayer): ImageBitmap? {
        val request = picture(drawingScope, layer)
        return try {
            withTimeoutOrNull(frameTimeout * 2) { request.await() }
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            null
        } finally {
            request.cancel()
        }
    }

    private fun picture(drawingScope: CoroutineScope, layer: GraphicsLayer) = drawingScope.async {
            val frame = CompletableDeferred<Unit>()
            recorded = frame
            isPending = true
            try {
                withTimeoutOrNull(frameTimeout) { frame.await() } ?: return@async null
            } finally {
                isPending = false
                recorded = null
            }
        layer.takeIf { it.size.width > 0 && it.size.height > 0 }?.toImageBitmap()
    }

    /**
     * Called while drawing, once the frame asked for is recorded.
     */
    fun onRecorded() {
        recorded?.complete(Unit)
    }
}
