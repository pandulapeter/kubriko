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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.workspace.SelectionController

/**
 * What the sprite editor needs of the workspace.
 *
 * @param state What the sprite editor keeps, as long as the workspace.
 * @param sheets How the sprites cut into frames are cut, by their names.
 * @param imagesRevision Changes whenever Kubriko Creator wrote a sprite's image, so the sprite editor reads it again.
 * @param palette The project's palette, each `0xAARRGGBB`.
 * @param onCommand Applies a command unless it is rejected, returning why it was, and calling `onSuccess` once it was
 *   applied or `onFailure` if applying it fails after all.
 * @param onGuardLeaving Sets what is asked before the workspace shows something else, the project closes, or Kubriko
 *   Creator quits (the sprite editor's question about its unsaved changes), or clears it for null.
 */
internal class SpriteEditing(
    val state: SpriteEditorState = SpriteEditorState(),
    val sheets: Map<String, SpriteSheet> = emptyMap(),
    val imagesRevision: Int = 0,
    val palette: List<Long> = emptyList(),
    val onCommand: (Invocation<*>, onSuccess: () -> Unit, onFailure: () -> Unit) -> Rejection? = { _, _, _ -> null },
    val onGuardLeaving: (guard: ((proceed: () -> Unit) -> Unit)?) -> Unit = {},
)

/**
 * The [SpriteEditing] of the project [commands] change, whose leaving [selection] guards, keeping [state].
 */
@Composable
internal fun rememberSpriteEditing(commands: CommandExecutor, selection: SelectionController, state: SpriteEditorState) = SpriteEditing(
    state = state,
    sheets = commands.context.spriteFolders.state.collectAsState().value.sheets,
    imagesRevision = commands.context.spriteImagesRevision.collectAsState().value,
    palette = commands.context.palette.colors.collectAsState().value,
    onCommand = commands::dispatchIfValidWatching,
    onGuardLeaving = remember(selection) { { guard -> selection.leaveGuard = guard } },
)
