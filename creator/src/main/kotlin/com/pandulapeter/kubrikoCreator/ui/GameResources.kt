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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.resources.gameResourcePaths

/**
 * The sprites and fonts of the game's own module, by their paths from `composeResources`, which its types can use, since
 * the generated code looks them up there; null where the last resource scan isn't known, so everything found is offered.
 */
internal data class GameResources(
    val sprites: Set<String>? = null,
    val fonts: List<String> = emptyList(),
) {
    /**
     * Those of [files] that are the game's own.
     */
    fun spritesAmong(files: SpriteFiles) = if (sprites == null) files else SpriteFiles(files.files.filterKeys { it in sprites })

    companion object {
        fun of(context: CommandContext) = GameResources(
            sprites = context.gameResourcePaths(ResourceKind.SPRITE)?.toSet(),
            fonts = context.gameResourcePaths(ResourceKind.FONT).orEmpty(),
        )
    }
}
