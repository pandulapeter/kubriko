/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.ui.actorTypes.fittedInto
import com.pandulapeter.kubrikoCreator.ui.actorTypes.rememberPlayingFrame
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_pause
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.sprite_editor_preview_pause
import kubriko.creator.generated.resources.sprite_editor_preview_play
import kubriko.creator.generated.resources.sprite_editor_preview_speed
import kotlin.math.roundToInt

/**
 * The animation of [frames], cut as [sheet] says, playing in a loop at its speed as it is drawn, or showing frame
 * [shownIndex] while paused, with its speed, which changes the sheet's.
 *
 * @param onSpeedChange Plays it at the given frames per second.
 */
@Composable
internal fun SpriteAnimationPreview(
    frames: List<PixelImage>,
    sheet: SpriteSheet,
    shownIndex: Int,
    state: SpriteEditorState,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    val bitmaps = rememberFrameBitmaps(frames)
    val playingFrame = rememberPlayingFrame(sheet)
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val index = if (state.isPreviewPlaying) playingFrame.value else shownIndex
        val bitmap = bitmaps.getOrNull(index) ?: return@Canvas
        val (topLeft, fitted) = fittedInto(IntSize(bitmap.width, bitmap.height), size)
        drawCheckerboard(Rect(topLeft, fitted))
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
            dstSize = IntSize(fitted.width.roundToInt().coerceAtLeast(1), fitted.height.roundToInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.None,
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconActionButton(
            icon = if (state.isPreviewPlaying) Res.drawable.ic_pause else Res.drawable.ic_play,
            label = if (state.isPreviewPlaying) Res.string.sprite_editor_preview_pause else Res.string.sprite_editor_preview_play,
            onClick = { state.isPreviewPlaying = !state.isPreviewPlaying },
        )
        NumberField(
            modifier = Modifier.weight(1f),
            label = Res.string.sprite_editor_preview_speed,
            value = sheet.framesPerSecond,
            onValueChange = onSpeedChange,
        )
    }
}
