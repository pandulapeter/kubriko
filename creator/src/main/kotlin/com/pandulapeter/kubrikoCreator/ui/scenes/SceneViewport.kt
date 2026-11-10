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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Scene as the game draws it on [background], recorded into a layer that [pictureHost] can take a picture of while it
 * is shown, so the picture has the editor's background where the Scene draws nothing, rather than transparency.
 */
@Composable
internal fun SceneViewport(
    kubriko: Kubriko,
    pictureHost: ScenePictureHost?,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val pictureLayer = rememberGraphicsLayer()
    DisposableEffect(pictureLayer, pictureHost) {
        val picture = ScenePicture {
            val bitmap = pictureLayer.toImageBitmap()
            withContext(Dispatchers.Default) { ScenePicture.pngOf(bitmap) }
        }
        pictureHost?.provide(picture)
        onDispose { pictureHost?.withdraw(picture) }
    }
    KubrikoViewport(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                pictureLayer.record {
                    drawRect(background)
                    this@drawWithContent.drawContent()
                }
                drawLayer(pictureLayer)
            },
        kubriko = kubriko,
        windowInsets = WindowInsets(0),
    )
}
