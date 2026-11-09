/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.game

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ScreenshotImageTest {
    private val png = BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB).apply {
        createGraphics().apply {
            color = Color.RED
            fillRect(0, 0, 200, 200)
            color = Color.BLUE
            fillRect(200, 0, 200, 200)
            dispose()
        }
    }.let { image -> ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray() }

    private fun read(bytes: ByteArray) = ImageIO.read(bytes.inputStream())

    @Test
    fun aScreenshotThatCantBeReadIsReturnedAsItIs() {
        val truncated = png.copyOf(40)
        assertContentEquals(truncated, ScreenshotImage.edited(truncated, maximumWidth = 200))
    }

    @Test
    fun aRegionStartingAtTheRightOrBottomEdgeKeepsAPixel() {
        assertEquals(1 to 200, read(ScreenshotImage.edited(png, maximumWidth = 200, left = 1f)).let { it.width to it.height })
        assertEquals(200 to 1, read(ScreenshotImage.edited(png, maximumWidth = 200, top = 1f)).let { it.width to it.height })
    }

    @Test
    fun aScreenshotIsScaledDownToTheWidthAsked() {
        val image = read(ScreenshotImage.edited(png, maximumWidth = 200))
        assertEquals(200 to 100, image.width to image.height)
    }

    @Test
    fun aRegionIsKeptAndScaledOnlyIfItIsWiderThanAsked() {
        val image = read(ScreenshotImage.edited(png, maximumWidth = 1280, left = 0.5f))
        assertEquals(200 to 200, image.width to image.height)
        assertEquals(Color.BLUE.rgb, image.getRGB(100, 100))
    }

    @Test
    fun aScreenshotWithNothingToDoIsReturnedAsItIs() {
        assertContentEquals(png, ScreenshotImage.edited(png, maximumWidth = 1280))
        assertContentEquals(byteArrayOf(9), ScreenshotImage.edited(byteArrayOf(9), maximumWidth = 160, left = 0.5f))
    }
}
