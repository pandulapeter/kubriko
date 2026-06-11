/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.gameplay.resources

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sprites.SpriteManager
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.map_01
import kubriko.examples.demo_isometric_graphics.generated.resources.texture_01

class TextureResolver : Manager() {

    private val spriteManager by manager<SpriteManager>()

    fun resolveTexture(textureName: String): ImageBitmap? = if (isInitialized.value) when (textureName) {
        "01" -> spriteManager.get(Res.drawable.texture_01)
        "map" -> spriteManager.get(Res.drawable.map_01)
        else -> null
    } else null
}