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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.resources.DrawPixels
import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import com.pandulapeter.kubrikoCreator.commands.resources.SetPalette
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.ui.actorTypes.spriteLabel
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_editor_size
import kubriko.creator.generated.resources.sprite_editor_size_and_cursor
import kubriko.creator.generated.resources.sprite_editor_unreadable
import org.jetbrains.compose.resources.stringResource
import java.io.File

/**
 * The sprite editor, showing the sprite at [path] (from `composeResources`): its tools, the frame shown, and the colors.
 * Every change is a command ([DrawPixels] for a stroke or a selection put down, [EditSprite] for the Image menu, and
 * [SetPalette]), so it is undone like any other and the AI can make it too.
 *
 * @param file The sprite's image, or null while the scan hasn't found it.
 * @param sheet How the sprite is cut into frames, if it is.
 * @param revision Changes whenever the image may have changed, so it is read again.
 * @param palette The project's palette, each `0xAARRGGBB`.
 * @param onCommand Applies a command unless it is rejected, returning why it was, and calling its `onFailure` if
 *   applying it fails after all.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SpriteEditor(
    path: String,
    file: File?,
    sheet: SpriteSheet?,
    revision: Any?,
    palette: List<Long>,
    state: SpriteEditorState,
    onCommand: (Invocation<*>, onFailure: () -> Unit) -> Rejection?,
    modifier: Modifier = Modifier,
) {
    remember(path) { state.showSprite(path) }
    val read = rememberSpriteFrames(file, sheet, revision)
    LaunchedEffect(read) { state.pendingPixels = emptyList() }
    val frames = when (read) {
        SpriteFramesRead.Reading -> return Box(modifier) { ProgressScreen(spriteLabel(path)) }
        SpriteFramesRead.Unreadable -> return Box(modifier) { UnreadableNotice(path) }
        is SpriteFramesRead.Read -> read.frames
    }
    val frameIndex = state.frameIndex.coerceIn(0, frames.frames.size - 1)
    val frame = frames.frames[frameIndex]
    val draw: (List<Pixel>) -> Unit = { state.draw(it, path, frameIndex, onCommand) }
    var askedAction by remember { mutableStateOf<SpriteImageAction?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(canvasSize, frame.width, frame.height) {
        if (state.isFitPending && canvasSize != IntSize.Zero) state.zoomToFit(frame.width, frame.height, canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }
    val editImage: (EditSprite.Operation) -> Unit = { onCommand(Invocation(EditSprite, EditSprite.Input(path, it, frame = if (state.isEveryFrame) null else frameIndex))) {} }
    Column(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) { focusRequester.requestFocus() }
            .onKeyEvent { event -> event.type == KeyEventType.KeyDown && state.onKey(event.key, event.isCtrlPressed || event.isMetaPressed, frame, draw) },
    ) {
        SpriteEditorToolbar(
            path = path,
            frameCount = frames.frames.size,
            state = state,
            onImageAction = { action -> action.operation?.let(editImage) ?: run { askedAction = action } },
            onFit = { state.zoomToFit(frame.width, frame.height, canvasSize.width.toFloat(), canvasSize.height.toFloat()) },
        )
        HorizontalDivider()
        Row(Modifier.weight(1f).fillMaxWidth()) {
            SpriteToolColumn(state = state, modifier = Modifier.fillMaxHeight())
            VerticalDivider()
            SpriteCanvas(
                modifier = Modifier.weight(1f).fillMaxHeight().onSizeChanged { canvasSize = it },
                frame = frame,
                state = state,
                onDraw = draw,
            )
            VerticalDivider()
            SpriteColorsColumn(
                modifier = Modifier.width(COLORS_WIDTH).fillMaxHeight(),
                state = state,
                palette = palette,
                frameColors = remember(frame) { frame.colors().take(MAXIMUM_FRAME_COLORS) },
                onPaletteChange = { onCommand(Invocation(SetPalette, SetPalette.Input(it))) {} },
            )
        }
        HorizontalDivider()
        SpriteStatusLine(frame.width, frame.height, state)
    }
    askedAction?.let { action ->
        SpriteOperationDialog(
            action = action,
            frameWidth = frame.width,
            frameHeight = frame.height,
            onApply = {
                askedAction = null
                editImage(it)
            },
            onDismiss = { askedAction = null },
        )
    }
}

/**
 * Draws [pixels] on frame [frameIndex] of the sprite at [path] through [onCommand], as one step. They show over the frame
 * ([SpriteEditorState.pendingPixels], which the canvas sets as it draws) until the image is read again, or until the
 * write is refused or fails, which leaves the image as it was.
 */
internal fun SpriteEditorState.draw(pixels: List<Pixel>, path: String, frameIndex: Int, onCommand: (Invocation<*>, onFailure: () -> Unit) -> Rejection?) {
    if (pixels.isEmpty()) return
    val dropPending = { if (pendingPixels === pixels) pendingPixels = emptyList() }
    val drawn = pixels.map { DrawPixels.DrawnPixel(it.x, it.y, it.color.toLong() and 0xFFFFFFFFL) }
    if (onCommand(Invocation(DrawPixels, DrawPixels.Input(path, frameIndex, drawn)), dropPending) != null) dropPending()
}

@Composable
private fun SpriteStatusLine(width: Int, height: Int, state: SpriteEditorState) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
) {
    val size = stringResource(Res.string.sprite_editor_size, width, height)
    val text = state.cursor?.let { stringResource(Res.string.sprite_editor_size_and_cursor, size, it.x, it.y) } ?: size
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun UnreadableNotice(path: String) = Text(
    modifier = Modifier.padding(24.dp),
    text = stringResource(Res.string.sprite_editor_unreadable, spriteLabel(path)),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

private val COLORS_WIDTH = 200.dp
private const val MAXIMUM_FRAME_COLORS = 48
