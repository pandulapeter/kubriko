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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pandulapeter.kubrikoCreator.commands.resources.EditSpriteFrames
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.ui.actorTypes.fittedInto
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_copy
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.sprite_editor_frame_add
import kubriko.creator.generated.resources.sprite_editor_frame_add_after
import kubriko.creator.generated.resources.sprite_editor_frame_add_before
import kubriko.creator.generated.resources.sprite_editor_frame_delete
import kubriko.creator.generated.resources.sprite_editor_frame_duplicate
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Under the sprite editor's canvas: a thumbnail of each of [frames], the one shown ([shownIndex]) marked, each shown when
 * clicked and moved to another place when dragged; adding a see-through frame after the one shown, duplicating it, and
 * deleting it (never the last one), which a thumbnail's right-click menu offers for its own frame too.
 *
 * @param onShow Shows the frame of the given index.
 * @param onEdit Applies the given change to the frames.
 */
@Composable
internal fun SpriteFrameStrip(
    frames: List<PixelImage>,
    shownIndex: Int,
    onShow: (Int) -> Unit,
    onEdit: (EditSpriteFrames.Operation) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    Row {
        IconActionButton(icon = Res.drawable.ic_add, label = Res.string.sprite_editor_frame_add, onClick = { onEdit(EditSpriteFrames.Operation.Add(at = shownIndex + 1)) })
        IconActionButton(icon = Res.drawable.ic_copy, label = Res.string.sprite_editor_frame_duplicate, onClick = { onEdit(EditSpriteFrames.Operation.Duplicate(shownIndex)) })
        IconActionButton(
            icon = Res.drawable.ic_delete,
            label = Res.string.sprite_editor_frame_delete,
            isEnabled = frames.size > 1,
            onClick = { onEdit(EditSpriteFrames.Operation.Delete(shownIndex)) },
        )
    }
    val pitch = with(LocalDensity.current) { (THUMBNAIL_SIZE + THUMBNAIL_SPACING).toPx() }
    var dragged by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val target = dragged?.let { frameDropIndex(it, dragOffset, pitch, frames.size) }
    Row(
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(THUMBNAIL_SPACING),
    ) {
        frames.forEachIndexed { index, frame ->
            val shift = when {
                dragged == null || target == null || index == dragged -> 0f
                index in (dragged!! + 1)..target -> -pitch
                index in target until dragged!! -> pitch
                else -> 0f
            }
            FrameThumbnail(
                modifier = Modifier
                    .zIndex(if (index == dragged) 1f else 0f)
                    .graphicsLayer { translationX = if (index == dragged) dragOffset else shift }
                    .frameDragSource(
                        index = index,
                        onStart = {
                            dragged = index
                            dragOffset = 0f
                        },
                        onDrag = { dragOffset += it },
                        onEnd = { isDropped ->
                            val from = dragged
                            val to = from?.let { frameDropIndex(it, dragOffset, pitch, frames.size) }
                            dragged = null
                            dragOffset = 0f
                            if (isDropped && from != null && to != null && to != from) onEdit(EditSpriteFrames.Operation.Move(from, to))
                        },
                    ),
                frame = frame,
                number = index + 1,
                isShown = index == shownIndex,
                canDelete = frames.size > 1,
                onClick = { onShow(index) },
                onEdit = { operation -> onEdit(operation(index)) },
            )
        }
    }
}

/**
 * Where a frame dragged from [from] by [offset] screen pixels goes, thumbnails being [pitch] pixels apart, among [count].
 */
internal fun frameDropIndex(from: Int, offset: Float, pitch: Float, count: Int): Int = (from + (offset / pitch).roundToInt()).coerceIn(0, count - 1)

@Composable
private fun Modifier.frameDragSource(index: Int, onStart: () -> Unit, onDrag: (Float) -> Unit, onEnd: (isDropped: Boolean) -> Unit): Modifier {
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnEnd by rememberUpdatedState(onEnd)
    return pointerInput(index) {
        detectDragGestures(
            onDragStart = { currentOnStart() },
            onDragEnd = { currentOnEnd(true) },
            onDragCancel = { currentOnEnd(false) },
            onDrag = { change, amount ->
                change.consume()
                currentOnDrag(amount.x)
            },
        )
    }
}

@Composable
private fun FrameThumbnail(
    frame: PixelImage,
    number: Int,
    isShown: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onEdit: ((Int) -> EditSpriteFrames.Operation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val menu = rememberRightClickMenuState()
    val bitmap = remember(frame) { frame.toImageBitmap() }
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier.clip(shape).rightClickMenu(menu).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(THUMBNAIL_SIZE)
                .clip(shape)
                .border(if (isShown) 2.dp else 1.dp, if (isShown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(4.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val (topLeft, fitted) = fittedInto(IntSize(frame.width, frame.height), size)
                drawCheckerboard(Rect(topLeft, fitted), square = THUMBNAIL_CHECKERBOARD_SQUARE)
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                    dstSize = IntSize(fitted.width.roundToInt().coerceAtLeast(1), fitted.height.roundToInt().coerceAtLeast(1)),
                    filterQuality = FilterQuality.None,
                )
            }
        }
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isShown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    RightClickMenu(
        state = menu,
        actions = listOfNotNull(
            ContextAction(label = stringResource(Res.string.sprite_editor_frame_add_before)) { onEdit { EditSpriteFrames.Operation.Add(at = it) } },
            ContextAction(label = stringResource(Res.string.sprite_editor_frame_add_after)) { onEdit { EditSpriteFrames.Operation.Add(at = it + 1) } },
            ContextAction(label = stringResource(Res.string.sprite_editor_frame_duplicate), icon = Res.drawable.ic_copy) { onEdit(EditSpriteFrames.Operation::Duplicate) },
            ContextAction(label = stringResource(Res.string.sprite_editor_frame_delete), icon = Res.drawable.ic_delete, isDestructive = true, isSeparated = true) {
                onEdit(EditSpriteFrames.Operation::Delete)
            }.takeIf { canDelete },
        ),
    )
}

private val THUMBNAIL_SIZE = 56.dp
private val THUMBNAIL_SPACING = 6.dp
private const val THUMBNAIL_CHECKERBOARD_SQUARE = 4f
