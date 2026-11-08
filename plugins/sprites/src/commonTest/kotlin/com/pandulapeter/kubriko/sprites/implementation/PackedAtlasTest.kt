/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.implementation

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.sprites.FakeImageBitmap
import com.pandulapeter.kubriko.sprites.SpriteResource
import com.pandulapeter.kubriko.sprites.TextureMipChainCache
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals

/** What an atlas warms up and keeps mip chains for: every bitmap its regions lie on, pages or not. */
class PackedAtlasTest {

    private val resources = listOf(spriteResource("small"), spriteResource("large"))
    private val small = FakeImageBitmap(4, 4)
    private val large = FakeImageBitmap(100, 4)
    private val pageBuilder = AtlasPageBuilder { width, height, _, _, _, _ -> FakeImageBitmap(width, height) }

    @Test
    fun bitmapsHoldThePagesAndTheSpritesTooLargeForThem() {
        val packedAtlas = packAtlas(resources, listOf(small, large), maximumPageSize = 64, padding = 1, pageBuilder = pageBuilder, mipChains = TextureMipChainCache())

        assertEquals(setOf<ImageBitmap>(packedAtlas.pages.single(), large), packedAtlas.bitmaps)
    }

    @Test
    fun bitmapsOfAnUnpackedAtlasAreEverySprite() {
        val packedAtlas = unpackedAtlas(resources, listOf(small, large), TextureMipChainCache())

        assertEquals(setOf<ImageBitmap>(small, large), packedAtlas.bitmaps)
    }

    @OptIn(InternalResourceApi::class)
    private fun spriteResource(path: String) = SpriteResource(DrawableResource(path, emptySet()))
}
