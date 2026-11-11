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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import java.io.File

/**
 * The images an Actor type can be drawn as, and how each is cut into frames.
 *
 * @param sheets How the sprites cut into frames are cut, by their names ([com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore]).
 */
internal data class SpriteChoices(
    val files: SpriteFiles = SpriteFiles(),
    val sheets: Map<String, SpriteSheet> = emptyMap(),
) {
    /**
     * Every image's path from `composeResources`, in order.
     */
    val paths: List<String> = files.paths

    fun fileOf(path: String): File? = files.files[path]

    fun sheetOf(path: String): SpriteSheet? = sheets[ResourceKind.nameOf(path)]
}
