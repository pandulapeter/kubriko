/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpriteSheetTest {
    private val sheet = SpriteSheet(frameWidth = 16, frameHeight = 8, frameCount = 5, framesPerRow = 2)

    @Test
    fun eachBrokenRuleIsNamed() {
        assertNull(sheet.problem)
        assertEquals(SpriteSheetProblem.FRAME_SIZE, sheet.copy(frameHeight = 0).problem)
        assertEquals(SpriteSheetProblem.FRAME_COUNT, sheet.copy(framesPerRow = 0).problem)
        assertEquals(SpriteSheetProblem.FRAMES_PER_SECOND, sheet.copy(framesPerSecond = 0f).problem)
        assertEquals(SpriteSheetProblem.FRAMES_PER_SECOND, sheet.copy(framesPerSecond = Float.NaN).problem)
        assertEquals(SpriteSheetProblem.ORIGIN, sheet.copy(originX = 17).problem)
        assertNull(sheet.copy(originX = 16, originY = 0).problem)
    }

    @Test
    fun theFramesHaveToFitTheImage() {
        assertEquals(3, sheet.rowCount)
        assertNull(sheet.problemWithImage(32, 24))
        assertEquals(SpriteSheetProblem.LARGER_THAN_IMAGE, sheet.problemWithImage(31, 24))
        assertEquals(SpriteSheetProblem.LARGER_THAN_IMAGE, sheet.problemWithImage(32, 23))
        assertNull(sheet.copy(frameCount = 1, framesPerRow = 4).problemWithImage(16, 8))
    }

    @Test
    fun theOriginIsScaledFromAFrameToTheActor() {
        assertNull(sheet.originFraction)
        assertEquals(0.25f to 1f, sheet.copy(originX = 4, originY = 8).originFraction)
        assertEquals(0.5f to 0f, sheet.copy(originY = 0).originFraction)
    }

    @Test
    fun aStripIsGuessedAsSquareFramesInOneRow() {
        assertEquals(SpriteSheet(frameWidth = 16, frameHeight = 16, frameCount = 4, framesPerRow = 4), SpriteSheet.guessFor(64, 16))
        assertEquals(SpriteSheet(frameWidth = 16, frameHeight = 16, frameCount = 1, framesPerRow = 1), SpriteSheet.guessFor(16, 40))
    }
}
