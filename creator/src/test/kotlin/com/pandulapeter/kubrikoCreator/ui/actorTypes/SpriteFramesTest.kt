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

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpriteFramesTest {
    private val sheet = SpriteSheet(frameWidth = 10, frameHeight = 8, frameCount = 5, framesPerRow = 2)

    @Test
    fun framesGoInRowsFromTheTopLeftAndStayInsideTheImage() {
        val image = IntSize(20, 20)
        assertEquals(IntRect(0, 0, 20, 20), frameRegionOf(null, 3, image))
        assertEquals(IntRect(0, 0, 10, 8), frameRegionOf(sheet, 0, image))
        assertEquals(IntRect(10, 8, 20, 16), frameRegionOf(sheet, 3, image))
        assertEquals(IntRect(0, 16, 10, 20), frameRegionOf(sheet, 4, image))
    }

    @Test
    fun aTypeAsLargeAsItsSpriteFollowsItIntoFramesAndBackButOneSizedByHandStays() {
        val image = IntSize(50, 8)
        assertEquals(10f to 8f, sizeForSheet(50f, 8f, image, oldSheet = null, newSheet = sheet))
        assertEquals(50f to 8f, sizeForSheet(10f, 8f, image, oldSheet = sheet, newSheet = null))
        assertEquals(20f to 4f, sizeForSheet(10f, 8f, image, oldSheet = sheet, newSheet = sheet.copy(frameWidth = 20, frameHeight = 4)))
        assertEquals(null, sizeForSheet(64f, 64f, image, oldSheet = null, newSheet = sheet))
        assertEquals(null, sizeForSheet(10f, 8f, image, oldSheet = sheet, newSheet = sheet.copy(framesPerSecond = 2f)))
    }

    @Test
    fun aPictureFitsInTheMiddleKeepingItsShape() {
        assertEquals(Offset(0f, 25f) to Size(100f, 50f), fittedInto(IntSize(20, 10), Size(100f, 100f)))
        assertEquals(Offset(25f, 0f) to Size(50f, 100f), fittedInto(IntSize(5, 10), Size(100f, 100f)))
        assertEquals(Offset.Zero to Size.Zero, fittedInto(IntSize(0, 10), Size(100f, 100f)))
    }

    @Test
    fun aChangedSheetPlaysFromItsFirstFrame() = uiTest {
        mainClock.autoAdvance = false
        var playing by mutableStateOf<SpriteSheet?>(sheet.copy(framesPerSecond = 10f))
        var frame: State<Int>? = null
        showing { frame = rememberPlayingFrame(playing) }
        mainClock.advanceTimeBy(350)
        assertEquals(3, frame?.value)
        playing = sheet.copy(frameCount = 1)
        mainClock.advanceTimeBy(500)
        assertEquals(0, frame?.value)
        playing = sheet.copy(frameCount = 3, framesPerSecond = 10f)
        mainClock.advanceTimeBy(150)
        assertEquals(1, frame?.value)
    }
}
