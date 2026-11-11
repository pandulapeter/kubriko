/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites.pixels

import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpriteFramesTest {
    private val sheet = SpriteSheet(frameWidth = 2, frameHeight = 1, frameCount = 3, framesPerRow = 2)
    private val image = image("abcdz", "ef..z")

    @Test
    fun anImageIsCutAsItsSheetSaysOrIsOneFrame() {
        val frames = SpriteFrames.of(image, sheet).frames.map { it.rows() }
        assertEquals(listOf(listOf("ab"), listOf("cd"), listOf("ef")), frames)
        assertEquals(listOf(image), SpriteFrames.of(image, null).frames)
    }

    @Test
    fun framesOfTheSameSizeGoBackOverTheImageKeepingWhatIsOutsideThem() {
        val flipped = SpriteFrames.of(image, sheet).mapped(1, operation = PixelOperations::flippedHorizontally)!!
        assertEquals(listOf("abdcz", "ef..z"), flipped.toImage().rows())
        assertEquals(sheet, flipped.sheet)
        val drawn = SpriteFrames.of(image, sheet).withFrame(2, image("yy"))
        assertEquals(listOf("abcdz", "yy..z"), drawn.toImage().rows())
    }

    @Test
    fun framesOfANewSizeMakeANewImageAndSheetWithTheOriginWhereItWasMoved() {
        val scaled = SpriteFrames.of(image, sheet.copy(originX = 1, originY = 1)).mapped(null, origin = { it.originX!! * 2 to it.originY!! * 2 }) { PixelOperations.scaled(it, 2) }!!
        assertEquals(sheet.copy(frameWidth = 4, frameHeight = 2, originX = 2, originY = 2), scaled.sheet)
        assertEquals(listOf("aabbccdd", "aabbccdd", "eeff....", "eeff...."), scaled.toImage().rows())
        assertNull(SpriteFrames.of(image, sheet).mapped(0) { PixelOperations.scaled(it, 2) }, "one frame of several can't change its size")
    }

    @Test
    fun oneFrameOfSeveralLeavesTheOriginTheyShare() {
        val flipped = SpriteFrames.of(image, sheet.copy(originX = 0)).mapped(1, origin = { 2 to it.originY }, operation = PixelOperations::flippedHorizontally)!!
        assertEquals(0, flipped.sheet?.originX)
    }

    @Test
    fun theOnlyFrameMovesTheOriginAsEveryFrameWould() {
        val single = SpriteSheet(frameWidth = 4, frameHeight = 2, frameCount = 1, framesPerRow = 1, originX = 1)
        val flipped = SpriteFrames.of(image("abcd", "efgh"), single).mapped(0, origin = { 3 to it.originY }, operation = PixelOperations::flippedHorizontally)!!
        assertEquals(3, flipped.sheet?.originX)
    }

    @Test
    fun framesAddedRemovedOrReorderedAreLaidOutAnewOnAnImageJustLargeEnough() {
        val frames = SpriteFrames.of(image, sheet)
        val reordered = frames.withFrames(frames.frames.reversed(), framesPerRow = 2)
        assertEquals(listOf("efcdz", "ab..z"), reordered.toImage().rows(), "laid out the same, over the image")
        val added = frames.withFrames(frames.frames + image(".."), framesPerRow = 4)
        assertEquals(sheet.copy(frameCount = 4, framesPerRow = 4), added.sheet)
        assertEquals(listOf("abcdef.."), added.toImage().rows())
        val removed = frames.withFrames(frames.frames.take(1), framesPerRow = 2)
        assertEquals(sheet.copy(frameCount = 1, framesPerRow = 1), removed.sheet)
        assertEquals(listOf("ab"), removed.toImage().rows())
        val cut = SpriteFrames.of(image("ab"), null).withFrames(listOf(image("ab"), image("cd")), framesPerRow = 2)
        assertEquals(SpriteSheet(frameWidth = 2, frameHeight = 1, frameCount = 2, framesPerRow = 2), cut.sheet)
        assertEquals(listOf("abcd"), cut.toImage().rows())
    }

    @Test
    fun theSpeedIsTheSheetsAndLeavesTheImageAsItIs() {
        val faster = SpriteFrames.of(image, sheet).withFramesPerSecond(24f)
        assertEquals(sheet.copy(framesPerSecond = 24f), faster.sheet)
        assertEquals(image, faster.toImage())
        val single = SpriteFrames.of(image, null)
        assertEquals(single, single.withFramesPerSecond(24f))
    }
}
