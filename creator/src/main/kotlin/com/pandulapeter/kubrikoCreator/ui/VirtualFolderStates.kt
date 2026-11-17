/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.project.FontFolderStore
import com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore

/**
 * The sidebar's virtual folders, which are Kubriko Creator's own ([com.pandulapeter.kubrikoCreator.project.VirtualFolderStore]):
 * the sprites' and the fonts'.
 */
internal data class VirtualFolderStates(
    val sprites: SpriteFolderStore.State = SpriteFolderStore.State(),
    val fonts: FontFolderStore.State = FontFolderStore.State(),
)

/**
 * The virtual folders [context] keeps, as they are now.
 */
@Composable
internal fun collectVirtualFoldersOf(context: CommandContext) = VirtualFolderStates(
    sprites = context.spriteFolders.state.collectAsState().value,
    fonts = context.fontFolders.state.collectAsState().value,
)
