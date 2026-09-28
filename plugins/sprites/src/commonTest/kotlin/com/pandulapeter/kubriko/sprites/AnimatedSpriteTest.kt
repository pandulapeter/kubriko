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

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimatedSpriteTest {

    @Test
    fun loopingBackwardStepPastTheFirstFrameWrapsToTheLastFrame() {
        val sprite = animatedSprite()

        sprite.stepBackwards(deltaTimeInMilliseconds = 1, shouldLoop = true)

        assertEquals(3, sprite.frameIndex)
        assertTrue(sprite.isLastFrame)
    }

    @Test
    fun loopingBackwardStepsStayInRange() {
        val sprite = animatedSprite()

        sprite.stepBackwards(deltaTimeInMilliseconds = 5, shouldLoop = true)
        assertEquals(3, sprite.frameIndex)
        repeat(20) {
            sprite.stepBackwards(deltaTimeInMilliseconds = 5, shouldLoop = true)
            assertTrue(sprite.frameIndex in 0..3, "Frame index ${sprite.frameIndex} is out of range.")
        }
    }

    @Test
    fun nonLoopingBackwardStepStopsAtTheFirstFrame() {
        val sprite = animatedSprite()

        sprite.stepBackwards(deltaTimeInMilliseconds = 1)

        assertEquals(0, sprite.frameIndex)
    }

    @Test
    fun loopingForwardStepPastTheLastFrameWrapsToTheFirstFrame() {
        val sprite = animatedSprite().apply { frameIndex = 3 }

        sprite.stepForward(deltaTimeInMilliseconds = 1, shouldLoop = true)

        assertEquals(0, sprite.frameIndex)
    }

    @Test
    fun everyOrientationMapsEveryFrameToItsOwnCellInsideTheSheet() {
        listOf(6, 5).forEach { frameCount ->
            SpriteResource.Rotation.entries.forEach { orientation ->
                val sprite = AnimatedSprite(
                    getImageBitmap = { null },
                    frameSize = IntSize(1, 1),
                    frameCount = frameCount,
                    framesPerRow = FRAMES_PER_ROW,
                    orientation = orientation,
                )
                val cells = (0 until frameCount).map { sprite.getXIndex(it) to sprite.getYIndex(it) }
                val isRotatedSideways = orientation == SpriteResource.Rotation.DEGREES_90 || orientation == SpriteResource.Rotation.DEGREES_270
                val columnCount = if (isRotatedSideways) ROW_COUNT else FRAMES_PER_ROW
                val rowCount = if (isRotatedSideways) FRAMES_PER_ROW else ROW_COUNT

                assertEquals(frameCount, cells.toSet().size, "$orientation maps several frames of $frameCount to one cell: $cells")
                cells.forEach { (x, y) ->
                    assertTrue(x in 0 until columnCount && y in 0 until rowCount, "$orientation maps a frame outside the sheet: $cells")
                }
            }
        }
    }

    @Test
    fun rotationBy270DegreesWalksTheFirstRowUpwards() {
        val sprite = AnimatedSprite(
            getImageBitmap = { null },
            frameSize = IntSize(1, 1),
            frameCount = 6,
            framesPerRow = FRAMES_PER_ROW,
            orientation = SpriteResource.Rotation.DEGREES_270,
        )

        assertEquals(0 to FRAMES_PER_ROW - 1, sprite.getXIndex(0) to sprite.getYIndex(0))
        assertEquals(0 to FRAMES_PER_ROW - 2, sprite.getXIndex(1) to sprite.getYIndex(1))
    }

    private fun animatedSprite() = AnimatedSprite(
        getImageBitmap = { null },
        frameSize = IntSize(1, 1),
        frameCount = 4,
        framesPerRow = 2,
        framesPerSecond = 1000f,
    )

    private companion object {
        const val FRAMES_PER_ROW = 3
        const val ROW_COUNT = 2
    }
}
