/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class TextureMipChainTest {

    private val downscaled = mutableListOf<Pair<Int, Int>>()

    private fun chainOf(base: ImageBitmap, levelCount: Int = TextureMipChain.DEFAULT_LEVEL_COUNT) = TextureMipChain(
        base = base,
        levelCount = levelCount,
        downscale = { _, width, height ->
            downscaled += width to height
            FakeImageBitmap(width, height, argb = 0xFF102030.toInt())
        },
        finish = { it },
    )

    @Test
    fun `each level halves the one before it and is built once`() {
        val chain = chainOf(FakeImageBitmap(64, 20))

        assertEquals(16, chain.level(2).width)
        assertEquals(5, chain.level(2).height)
        assertSame(chain.level(1), chain.level(1))
        chain.level(2)
        assertEquals(listOf(32 to 10, 16 to 5), downscaled)
    }

    @Test
    fun `a level never shrinks below one pixel`() {
        val chain = chainOf(FakeImageBitmap(2, 1), levelCount = 4)

        assertEquals(1, chain.level(1).width)
        assertEquals(1, chain.level(1).height)
        assertSame(chain.level(1), chain.level(3))
        assertEquals(listOf(1 to 1), downscaled)
    }

    @Test
    fun `the level follows the scale it is drawn at`() {
        val chain = chainOf(FakeImageBitmap(64, 64))

        assertEquals(0, chain.levelIndexForScale(2f))
        assertEquals(0, chain.levelIndexForScale(0.51f))
        assertEquals(1, chain.levelIndexForScale(0.5f))
        assertEquals(1, chain.levelIndexForScale(0.26f))
        assertEquals(2, chain.levelIndexForScale(0.25f))
        assertEquals(2, chain.levelIndexForScale(0.01f))
        assertSame(chain.base, chain.forScale(1f))
        assertEquals(16, chain.forScale(0.1f).width)
    }

    @Test
    fun `the mean color is read from the smallest level`() {
        val chain = chainOf(FakeImageBitmap(64, 64, argb = 0xFFFFFFFF.toInt()))

        assertEquals(0xFF102030.toInt(), chain.meanArgb)
    }

    @Test
    fun `a chain holds at least its base`() {
        assertFailsWith<IllegalArgumentException> { chainOf(FakeImageBitmap(4, 4), levelCount = 0) }
    }

    @Test
    fun `the cache shares one chain per texture until the texture is replaced`() {
        val cache = TextureMipChainCache { chainOf(it) }
        val texture = FakeImageBitmap(8, 8)

        val chain = cache.get("grass", texture)
        assertSame(chain, cache.get("grass", texture))
        assertNotSame(chain, cache.get("bark", texture))
        val replacement = cache.get("grass", FakeImageBitmap(8, 8))
        assertNotSame(chain, replacement)
        cache.remove("grass")
        assertNotSame(replacement, cache.get("grass", replacement.base))
    }
}
