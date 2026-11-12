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
 * Opens what [target] names: a sprite the sidebar opens in the sprite editor there too, a Property's type, and anything
 * else as a page's link to it opens (sounds, music, Kotlin files, and other images in the system's app for them).
 *
 * @param rootDirectory The project's folder, which [target]'s paths start from.
 */
internal fun SelectionController.open(target: QuickOpenTarget, rootDirectory: File) {
    when (target) {
        is QuickOpenTarget.Property -> selectType(target.type)
        is QuickOpenTarget.File -> when (val mention = target.mention) {
            is Mention.Sprite -> ResourceKind.pathFromComposeResources(mention.path)?.takeIf { ResourceKind.of(it) == ResourceKind.SPRITE }?.let(::openSprite) ?: open(mention, rootDirectory)
            else -> open(mention, rootDirectory)
        }
    }
}
