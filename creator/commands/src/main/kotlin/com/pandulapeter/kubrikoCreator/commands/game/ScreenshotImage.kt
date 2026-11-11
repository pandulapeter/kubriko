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

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Makes a screenshot cheaper for a model to read: cropped to a region and scaled down to a width. Every call blocks.
 */
internal object ScreenshotImage {

    /**
     * The widths a tool may ask for, at most the width the game's screenshots are taken at.
     */
    val widths = 160..1280

    /**
     * [png] cropped to the region from [left], [top] to [right], [bottom] (fractions of its size, from 0 to 1) and scaled
     * down to at most [maximumWidth] pixels wide, or [png] itself when there is nothing to do or it can't be read.
     */
    fun edited(png: ByteArray, maximumWidth: Int, left: Float = 0f, top: Float = 0f, right: Float = 1f, bottom: Float = 1f): ByteArray {
        val image = try {
            ImageIO.read(png.inputStream())
        } catch (_: Exception) {
            // ImageIO throws unchecked exceptions too on a malformed image.
            null
        } ?: return png
        val x = (left.coerceIn(0f, 1f) * image.width).toInt().coerceAtMost(image.width - 1)
        val y = (top.coerceIn(0f, 1f) * image.height).toInt().coerceAtMost(image.height - 1)
        val width = ((right.coerceIn(0f, 1f) * image.width).toInt() - x).coerceAtLeast(1)
        val height = ((bottom.coerceIn(0f, 1f) * image.height).toInt() - y).coerceAtLeast(1)
        val isCropped = x > 0 || y > 0 || width < image.width || height < image.height
        if (!isCropped && image.width <= maximumWidth) return png
        val cropped = image.getSubimage(x, y, width.coerceAtMost(image.width - x), height.coerceAtMost(image.height - y))
        val scale = (maximumWidth.toFloat() / cropped.width).coerceAtMost(1f)
        val result = BufferedImage((cropped.width * scale).toInt().coerceAtLeast(1), (cropped.height * scale).toInt().coerceAtLeast(1), BufferedImage.TYPE_INT_RGB)
        result.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(cropped, 0, 0, result.width, result.height, null)
            dispose()
        }
        return ByteArrayOutputStream().also { ImageIO.write(result, "png", it) }.toByteArray()
    }
}
