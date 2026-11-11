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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun aPictureFitsInTheMiddleKeepingItsShape() {
        assertEquals(Offset(0f, 25f) to Size(100f, 50f), fittedInto(IntSize(20, 10), Size(100f, 100f)))
        assertEquals(Offset(25f, 0f) to Size(50f, 100f), fittedInto(IntSize(5, 10), Size(100f, 100f)))
        assertEquals(Offset.Zero to Size.Zero, fittedInto(IntSize(0, 10), Size(100f, 100f)))
    }
}
