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
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.ui.screenplay.open
import com.pandulapeter.kubrikoCreator.workspace.SelectionController
import java.io.File

/**
 * Opens what [target] names: a sprite in the sprite editor, a Property's type, and anything else as a page's mention of
 * it opens (sounds, music, and Kotlin files in the system's app for them).
 *
 * @param rootDirectory The project's folder, which [target]'s paths start from.
 */
internal fun SelectionController.open(target: QuickOpenTarget, rootDirectory: File) {
    when (target) {
        is QuickOpenTarget.Property -> selectType(target.type)
        is QuickOpenTarget.File -> when (val mention = target.mention) {
            is Mention.Sprite -> spritePathOf(mention.path)?.let(::openSprite) ?: open(mention, rootDirectory)
            else -> open(mention, rootDirectory)
        }
    }
}

/**
 * The path of the sprite at [path] (from the project's root) from its `composeResources` folder, as the sprite editor
 * names it, or null when it isn't in one.
 */
internal fun spritePathOf(path: String): String? =
    "/$path".indexOf("/${ResourceKind.COMPOSE_RESOURCES}/").takeIf { it >= 0 }?.let { path.substring(it + ResourceKind.COMPOSE_RESOURCES.length + 1) }
