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

import kotlin.test.Test
import kotlin.test.assertEquals

class PixelShapesTest {
    private val blank = image(".....", ".....", ".....", ".....", ".....")

    private fun drawn(points: List<Pair<Int, Int>>) = blank.with(points.map { (x, y) -> Pixel(x, y, colorOf('a')) }).rows()

    @Test
    fun aLineCoversOnePixelPerStepBothEndsIncluded() {
        assertEquals(listOf("a....", ".a...", "..a..", "...a.", "....a"), drawn(PixelShapes.line(0, 0, 4, 4)))
        assertEquals(listOf(".....", ".....", "aaaaa", ".....", "....."), drawn(PixelShapes.line(4, 2, 0, 2)))
        assertEquals(listOf(0 to 0), PixelShapes.line(0, 0, 0, 0))
    }

    @Test
    fun aRectangleIsItsOutlineOrAllOfItWhicheverCornersComeFirst() {
        assertEquals(listOf(".....", ".aaa.", ".a.a.", ".aaa.", "....."), drawn(PixelShapes.rectangle(3, 3, 1, 1, isFilled = false)))
        assertEquals(listOf(".....", ".aaa.", ".aaa.", ".aaa.", "....."), drawn(PixelShapes.rectangle(1, 1, 3, 3, isFilled = true)))
    }

    @Test
    fun anEllipseFillsItsRectangleRoundly() {
        assertEquals(listOf(".aaa.", "aaaaa", "aaaaa", "aaaaa", ".aaa."), drawn(PixelShapes.ellipse(0, 0, 4, 4, isFilled = true)))
        assertEquals(listOf(".aaa.", "a...a", "a...a", "a...a", ".aaa."), drawn(PixelShapes.ellipse(4, 4, 0, 0, isFilled = false)))
        assertEquals(listOf(".....", ".....", "aaaaa", ".....", "....."), drawn(PixelShapes.ellipse(0, 2, 4, 2, isFilled = false)))
    }

    @Test
    fun aFloodFillReachesOnlyTheTouchingPixelsOfItsColorUnlessToldOtherwise() {
        val image = image("aab", "bab", "baa")
        assertEquals(setOf(0 to 0, 1 to 0, 1 to 1, 1 to 2, 2 to 2), PixelShapes.floodFill(image, 0, 0).toSet())
        assertEquals(setOf(2 to 0, 2 to 1), PixelShapes.floodFill(image, 2, 0).toSet())
        assertEquals(setOf(2 to 0, 2 to 1, 0 to 1, 0 to 2), PixelShapes.floodFill(image, 2, 0, isContiguous = false).toSet())
        assertEquals(emptyList(), PixelShapes.floodFill(image, 3, 0))
    }
}
