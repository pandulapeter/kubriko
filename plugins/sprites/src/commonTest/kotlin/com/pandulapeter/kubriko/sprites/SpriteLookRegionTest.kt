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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpriteLookRegionTest {

    private val page = FakeImageBitmap(64, 64)

    @Test
    fun aLookShowsTheRegionItIsGiven() {
        val look = SpriteLook(texture = null)

        look.setRegion(SpriteRegion(page, IntOffset(3, 5), IntSize(7, 9)))

        assertSame(page, look.texture)
        assertEquals(IntOffset(3, 5), look.srcOffset)
        assertEquals(IntSize(7, 9), look.srcSize)
    }

    @Test
    fun showingTheSameRegionAgainLeavesTheLookUnchanged() {
        val look = SpriteLook(texture = null)
        look.setRegion(SpriteRegion(page, IntOffset(3, 5), IntSize(7, 9)))
        val revision = look.revision

        look.setRegion(SpriteRegion(page, IntOffset(3, 5), IntSize(7, 9)))

        assertEquals(revision, look.revision)
    }

    @Test
    fun noRegionClearsTheTexture() {
        val look = SpriteLook(texture = page)

        look.setRegion(null)

        assertNull(look.texture)
    }

    @Test
    fun anAnimatedSpriteShowsItsCurrentFrameOfASheet() {
        val sheet = FakeImageBitmap(40, 20)
        val sprite = AnimatedSprite(getImageBitmap = { sheet }, frameSize = IntSize(10, 10), frameCount = 8, framesPerRow = 4)
        val look = SpriteLook(texture = null)
        sprite.frameIndex = 5

        sprite.applyTo(look)

        assertSame(sheet, look.texture)
        assertEquals(IntOffset(10, 10), look.srcOffset)
        assertEquals(IntSize(10, 10), look.srcSize)
    }

    @Test
    fun anAnimatedSpriteFromAnAtlasShowsItsFramesAsRegionsOfThePage() {
        var region: SpriteRegion? = null
        val sprite = AnimatedSprite.fromAtlas(getRegion = { region }, frameSize = IntSize(10, 10), frameCount = 8, framesPerRow = 4)
        val look = SpriteLook(texture = page)

        sprite.applyTo(look)
        assertNull(look.texture)
        assertFalse(sprite.isLoaded)

        region = SpriteRegion(page, IntOffset(12, 30), IntSize(40, 20))
        sprite.frameIndex = 6
        sprite.applyTo(look)

        assertTrue(sprite.isLoaded)
        assertSame(page, look.texture)
        assertEquals(IntOffset(12 + 20, 30 + 10), look.srcOffset)
        assertEquals(IntSize(10, 10), look.srcSize)
    }

    @Test
    fun aRotatedSheetInAnAtlasKeepsTheOrientationOfItsFrames() {
        val region = SpriteRegion(page, IntOffset(2, 4), IntSize(20, 40))
        val sprite = AnimatedSprite.fromAtlas(
            getRegion = { region },
            frameSize = IntSize(10, 10),
            frameCount = 8,
            framesPerRow = 4,
            orientation = SpriteResource.Rotation.DEGREES_90,
        )
        val sheetSprite = AnimatedSprite(
            getImageBitmap = { page as ImageBitmap },
            frameSize = IntSize(10, 10),
            frameCount = 8,
            framesPerRow = 4,
            orientation = SpriteResource.Rotation.DEGREES_90,
        )
        val atlasLook = SpriteLook(texture = null)
        val sheetLook = SpriteLook(texture = null)

        for (frame in 0 until 8) {
            sprite.frameIndex = frame
            sheetSprite.frameIndex = frame
            sprite.applyTo(atlasLook)
            sheetSprite.applyTo(sheetLook)

            assertEquals(sheetLook.srcOffset.x + 2, atlasLook.srcOffset.x)
            assertEquals(sheetLook.srcOffset.y + 4, atlasLook.srcOffset.y)
        }
    }
}
