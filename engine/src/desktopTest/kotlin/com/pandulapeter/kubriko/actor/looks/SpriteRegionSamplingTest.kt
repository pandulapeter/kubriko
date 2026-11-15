/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.helpers.TriangleBatch
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals

class SpriteRegionSamplingTest {

    private fun texelColumnsSampled(filterQuality: FilterQuality): Set<Int> {
        val look = SpriteLook(ImageBitmap(32, 8), srcOffset = IntOffset(0, 0), srcSize = IntSize(16, 8), filterQuality = filterQuality)
        val batch = TriangleBatch()
        LookEmitter.emit(look, batch, 16f, 8f, LookTransform(), pixelsPerUnit = 1f, isStandIn = false)
        return (0 until batch.vertexCount).mapTo(HashSet()) { floor(batch.texCoords[it * 2]).toInt() }
    }

    @Test
    fun aNearestSampledFrameOfASheetNeverReachesTheTexelOfTheFrameBesideIt() {
        assertEquals(setOf(0, 15), texelColumnsSampled(FilterQuality.None))
    }
}
