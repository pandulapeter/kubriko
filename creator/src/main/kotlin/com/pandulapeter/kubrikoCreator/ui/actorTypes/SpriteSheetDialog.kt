/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import com.pandulapeter.kubrikoCreator.ui.components.checkerboard
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.sprite_sheet_dialog_title
import kubriko.creator.generated.resources.sprite_sheet_frame_count
import kubriko.creator.generated.resources.sprite_sheet_frame_height
import kubriko.creator.generated.resources.sprite_sheet_frame_width
import kubriko.creator.generated.resources.sprite_sheet_frames_per_row
import kubriko.creator.generated.resources.sprite_sheet_frames_per_second
import kubriko.creator.generated.resources.sprite_sheet_image_size
import kubriko.creator.generated.resources.sprite_sheet_origin_middle
import kubriko.creator.generated.resources.sprite_sheet_origin_x
import kubriko.creator.generated.resources.sprite_sheet_origin_y
import kubriko.creator.generated.resources.sprite_sheet_show_whole
import org.jetbrains.compose.resources.stringResource

/**
 * Where the sprite at [path] is cut into the frames of an animation: the whole image with the frames drawn on it, the
 * animation playing, and how the frames lie. A sprite not cut yet starts from a guess ([SpriteSheet.guessFor]) once its
 * image is read.
 *
 * @param onSave Called with how to cut it, or with null to show it whole again.
 */
@Composable
internal fun SpriteSheetDialog(
    path: String,
    bitmap: ImageBitmap?,
    initialSheet: SpriteSheet?,
    onSave: (SpriteSheet?) -> Unit,
    onDismiss: () -> Unit,
) {
    var sheet by remember { mutableStateOf(initialSheet) }
    LaunchedEffect(bitmap) {
        if (sheet == null && bitmap != null) sheet = SpriteSheet.guessFor(bitmap.width, bitmap.height)
    }
    val problem = sheet?.let { it.problem ?: bitmap?.let { image -> it.problemWithImage(image.width, image.height) } }
    CreatorDialog(
        title = stringResource(Res.string.sprite_sheet_dialog_title, spriteLabel(path)),
        maxWidth = 640.dp,
        onDismiss = onDismiss,
        confirmButton = {
            TextButton(
                enabled = sheet != null && problem == null,
                onClick = { onSave(sheet) },
            ) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (initialSheet != null) TextButton(onClick = { onSave(null) }) { Text(stringResource(Res.string.sprite_sheet_show_whole)) }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val shown = sheet?.takeIf { problem == null }
            val frame = rememberPlayingFrame(shown)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SheetPreview(
                    modifier = Modifier.weight(1f).height(180.dp),
                    bitmap = bitmap,
                    sheet = shown,
                )
                SpriteThumbnail(
                    modifier = Modifier.size(120.dp),
                    bitmap = bitmap,
                    sheet = shown,
                    frame = { frame.value },
                )
            }
            if (bitmap != null) {
                Text(
                    text = stringResource(Res.string.sprite_sheet_image_size, bitmap.width, bitmap.height),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            sheet?.let { current -> SheetFields(current) { sheet = it } }
            problem?.let {
                Text(
                    text = it.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * The fields of [sheet]: whole numbers of pixels and frames, the frames per second, and the origin, in the middle unless
 * it is turned off.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetFields(
    sheet: SpriteSheet,
    onChange: (SpriteSheet) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WholeNumberField(stringResource(Res.string.sprite_sheet_frame_width), sheet.frameWidth) { onChange(sheet.copy(frameWidth = it)) }
        WholeNumberField(stringResource(Res.string.sprite_sheet_frame_height), sheet.frameHeight) { onChange(sheet.copy(frameHeight = it)) }
        WholeNumberField(stringResource(Res.string.sprite_sheet_frame_count), sheet.frameCount) { onChange(sheet.copy(frameCount = it)) }
        WholeNumberField(stringResource(Res.string.sprite_sheet_frames_per_row), sheet.framesPerRow) { onChange(sheet.copy(framesPerRow = it)) }
        NumberField(
            modifier = Modifier.width(FIELD_WIDTH),
            label = Res.string.sprite_sheet_frames_per_second,
            value = sheet.framesPerSecond,
            onValueChange = { onChange(sheet.copy(framesPerSecond = it)) },
        )
    }
    val isOriginInTheMiddle = sheet.originX == null && sheet.originY == null
    LabeledSwitch(
        label = stringResource(Res.string.sprite_sheet_origin_middle),
        checked = isOriginInTheMiddle,
        onCheckedChange = { isMiddle ->
            onChange(if (isMiddle) sheet.copy(originX = null, originY = null) else sheet.copy(originX = sheet.frameWidth / 2, originY = sheet.frameHeight / 2))
        },
    )
    if (!isOriginInTheMiddle) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WholeNumberField(stringResource(Res.string.sprite_sheet_origin_x), sheet.originX ?: (sheet.frameWidth / 2)) { onChange(sheet.copy(originX = it)) }
            WholeNumberField(stringResource(Res.string.sprite_sheet_origin_y), sheet.originY ?: (sheet.frameHeight / 2)) { onChange(sheet.copy(originY = it)) }
        }
    }
}

@Composable
private fun WholeNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) = NumberField(
    modifier = Modifier.width(FIELD_WIDTH),
    label = label,
    value = value.toFloat(),
    onValueChange = { if (it == it.toInt().toFloat()) onValueChange(it.toInt()) },
)

/**
 * The whole image, as large as fits, with the frames of [sheet] outlined on it.
 */
@Composable
private fun SheetPreview(
    bitmap: ImageBitmap?,
    sheet: SpriteSheet?,
    modifier: Modifier = Modifier,
) {
    val outline = Accent.CAST.color
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(4.dp)).checkerboard(),
    ) {
        if (bitmap == null) return@Canvas
        val imageSize = IntSize(bitmap.width, bitmap.height)
        val (offset, fitted) = fittedInto(imageSize, size)
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
            dstSize = IntSize(fitted.width.toInt(), fitted.height.toInt()),
            filterQuality = FilterQuality.None,
        )
        if (sheet == null) return@Canvas
        val scale = fitted.width / imageSize.width
        repeat(sheet.frameCount) { index ->
            val region = frameRegionOf(sheet, index, imageSize)
            drawRect(
                color = outline,
                topLeft = offset + Offset(region.left * scale, region.top * scale),
                size = Size(region.width * scale, region.height * scale),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

private val FIELD_WIDTH = 200.dp
