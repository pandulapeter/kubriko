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
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
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
import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import com.pandulapeter.kubrikoCreator.commands.resources.SaveSprite
import com.pandulapeter.kubrikoCreator.commands.resources.SetPalette
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.ui.actorTypes.spriteLabel
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close
import kubriko.creator.generated.resources.sprite_editor_changed_elsewhere
import kubriko.creator.generated.resources.sprite_editor_keep_mine
import kubriko.creator.generated.resources.sprite_editor_load_change
import kubriko.creator.generated.resources.sprite_editor_size
import kubriko.creator.generated.resources.sprite_editor_size_and_cursor
import kubriko.creator.generated.resources.sprite_editor_unreadable
import org.jetbrains.compose.resources.stringResource
import java.io.File

/**
 * The sprite editor, showing the sprite at [path] (from `composeResources`): its tools, the frame shown, and the colors.
 * Unlike the rest of Kubriko Creator, it keeps what is drawn ([SpriteEditorState.draft], with undo and redo of its own)
 * until it is saved ([SaveSprite], as one step of the workspace's history), and asks before the workspace shows something
 * else while it holds changes, so a sprite reaches the game and git only as the user finished it. The palette is the
 * project's, so a change to it ([SetPalette]) applies at once.
 *
 * @param file The sprite's image, or null while the scan hasn't found it.
 * @param sheet How the sprite is cut into frames, if it is.
 * @param revision Changes whenever the image may have changed, so it is read again.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SpriteEditor(
    path: String,
    file: File?,
    sheet: SpriteSheet?,
    revision: Any?,
    state: SpriteEditorState,
    editing: SpriteEditing,
    modifier: Modifier = Modifier,
) {
    remember(path) { state.showSprite(path) }
    val read = rememberSpriteFrames(file, sheet, revision)
    LaunchedEffect(read) { (read as? SpriteFramesRead.Read)?.let { state.onRead(it.frames) } }
    val frames = when (read) {
        SpriteFramesRead.Reading -> return Box(modifier) { ProgressScreen(spriteLabel(path)) }
        SpriteFramesRead.Unreadable -> return Box(modifier) { UnreadableNotice(path) }
        is SpriteFramesRead.Read -> state.draft?.current ?: read.frames
    }
    val frameIndex = state.frameIndex.coerceIn(0, frames.frames.size - 1)
    val frame = frames.frames[frameIndex]
    val draw: (List<Pixel>) -> Unit = { state.draw(frameIndex, it) }
    var askedAction by remember { mutableStateOf<SpriteImageAction?>(null) }
    var problem by remember { mutableStateOf<Rejection?>(null) }
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(canvasSize, frame.width, frame.height) {
        if (state.isFitPending && canvasSize != IntSize.Zero) state.zoomToFit(frame.width, frame.height, canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }
    val hasChanges = state.hasChanges
    DisposableEffect(hasChanges, editing.onGuardLeaving) {
        editing.onGuardLeaving(if (hasChanges) { proceed -> pendingLeave = proceed } else null)
        onDispose { editing.onGuardLeaving(null) }
    }
    val editImage: (EditSprite.Operation) -> Unit = {
        draw(state.settle(frame))
        problem = state.edit(path, it, frameIndex = if (state.isEveryFrame) null else frameIndex)
    }
    val save: (onSaved: () -> Unit) -> Unit = { onSaved -> problem = state.save(path, frame, frameIndex, editing.onCommand, onSaved) }
    Column(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) { focusRequester.requestFocus() }
            .onKeyEvent { event ->
                val isShortcut = event.isCtrlPressed || event.isMetaPressed
                event.type == KeyEventType.KeyDown && (state.onDraftKey(event.key, isShortcut, event.isShiftPressed) { save {} } || state.onKey(event.key, isShortcut, frame, draw))
            },
    ) {
        SpriteEditorToolbar(
            path = path,
            frameCount = frames.frames.size,
            state = state,
            onImageAction = { action -> action.operation?.let(editImage) ?: run { askedAction = action } },
            onFit = { state.zoomToFit(frame.width, frame.height, canvasSize.width.toFloat(), canvasSize.height.toFloat()) },
            onSave = { save {} },
        )
        HorizontalDivider()
        SpriteToolOptionsBar(state = state)
        HorizontalDivider()
        SpriteEditorNotices(path = path, state = state, problem = problem, onDismissProblem = { problem = null })
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
                palette = editing.palette,
                frameColors = remember(frame) { frame.colors().take(MAXIMUM_FRAME_COLORS) },
                onPaletteChange = { problem = editing.onCommand(Invocation(SetPalette, SetPalette.Input(it)), {}, {}) },
            )
        }
        HorizontalDivider()
        SpriteFrameStrip(
            frames = frames.frames,
            shownIndex = frameIndex,
            onShow = { index -> state.frameIndex = index.also { draw(state.settle(frame)) } },
            onEdit = { operation -> problem = state.editFrames(path, operation.also { draw(state.settle(frame)) }) },
        )
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
    pendingLeave?.let { proceed ->
        SpriteUnsavedDialog(
            path = path,
            onSave = {
                pendingLeave = null
                save(proceed)
            },
            onDiscard = {
                pendingLeave = null
                state.discard()
                proceed()
            },
            onDismiss = { pendingLeave = null },
        )
    }
}

/**
 * Saves the draft through [onCommand] (putting down what floats on [frame], frame [frameIndex], first), calling [onSaved]
 * once it is saved, or at once when there is nothing to save. A save refused or failing brings the changes back.
 *
 * @return Why the save was refused, or null.
 */
internal fun SpriteEditorState.save(
    path: String,
    frame: PixelImage,
    frameIndex: Int,
    onCommand: (Invocation<*>, onSuccess: () -> Unit, onFailure: () -> Unit) -> Rejection?,
    onSaved: () -> Unit,
): Rejection? {
    draw(frameIndex, settle(frame))
    val frames = draft?.takeIf { it.isChanged }?.current ?: return null.also { onSaved() }
    val previouslySaved = markSaved() ?: return null
    keepOwnChanges()
    return onCommand(Invocation(SaveSprite, SaveSprite.Input(path, frames)), onSaved) { onSaveFailed(previouslySaved) }?.also { onSaveFailed(previouslySaved) }
}

/**
 * What a key does to the draft, with Ctrl or Cmd held when [isShortcut]: saving it ([onSave]), and undoing and redoing its
 * steps, which let go of the selection and what floats first. Returns whether the key did something, so undo and redo go
 * on to the workspace once the draft has no steps left.
 */
internal fun SpriteEditorState.onDraftKey(key: Key, isShortcut: Boolean, isShift: Boolean, onSave: () -> Unit): Boolean {
    if (!isShortcut) return false
    val isRedo = (key == Key.Z && isShift) || key == Key.Y
    return when {
        key == Key.S -> true.also { onSave() }
        key == Key.Z || isRedo -> {
            val isDone = draft?.let { if (isRedo) it.canRedo else it.canUndo } == true
            if (isDone) {
                floating = null
                selection = null
                if (isRedo) redo() else undo()
            }
            isDone
        }
        else -> false
    }
}

/**
 * What the sprite editor tells above its canvas: that the sprite changed elsewhere while it holds changes, and why the
 * last thing asked of it was refused.
 */
@Composable
private fun SpriteEditorNotices(path: String, state: SpriteEditorState, problem: Rejection?, onDismissProblem: () -> Unit) {
    if (state.outsideChange != null) {
        SpriteEditorNotice(text = stringResource(Res.string.sprite_editor_changed_elsewhere, spriteLabel(path)), isError = false) {
            TextButton(onClick = state::keepOwnChanges) { Text(stringResource(Res.string.sprite_editor_keep_mine)) }
            TextButton(onClick = state::loadOutsideChange) { Text(stringResource(Res.string.sprite_editor_load_change)) }
        }
        HorizontalDivider()
    }
    if (problem != null) {
        SpriteEditorNotice(text = problem.reason.text(problem.arguments), isError = true) {
            TextButton(onClick = onDismissProblem) { Text(stringResource(Res.string.action_close)) }
        }
        HorizontalDivider()
    }
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
