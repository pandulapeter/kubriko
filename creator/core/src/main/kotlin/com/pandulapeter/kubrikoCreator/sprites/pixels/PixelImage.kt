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

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * An image as its pixels, each `0xAARRGGBB`, row by row from the top left: what the sprite editor and the AI's drawing
 * tools change, with pure operations ([PixelOperations], [PixelShapes]), so that nothing here needs Skia. Changing one
 * makes a copy.
 */
public class PixelImage(
    public val width: Int,
    public val height: Int,
    pixels: IntArray = IntArray(width * height),
) {
    private val pixels: IntArray = pixels.copyOf()

    init {
        require(width > 0 && height > 0) { "An image is at least 1 by 1 pixels, not $width by $height." }
        require(pixels.size == width * height) { "A $width by $height image has ${width * height} pixels, not ${pixels.size}." }
    }

    public operator fun get(x: Int, y: Int): Int = pixels[y * width + x]

    public fun contains(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    /**
     * This image with each pixel of [changes] (`x`, `y`, and its color) set, the ones outside it left out.
     */
    public fun with(changes: List<Pixel>): PixelImage {
        val copy = pixels.copyOf()
        changes.forEach { if (contains(it.x, it.y)) copy[it.y * width + it.x] = it.color }
        return PixelImage(width, height, copy)
    }

    /**
     * The part of this image from [left], [top] that is [cropWidth] by [cropHeight] pixels, transparent where it reaches
     * past the image.
     */
    public fun cropped(left: Int, top: Int, cropWidth: Int, cropHeight: Int): PixelImage = PixelImage(
        cropWidth,
        cropHeight,
        IntArray(cropWidth * cropHeight) { index ->
            val x = left + index % cropWidth
            val y = top + index / cropWidth
            if (contains(x, y)) this[x, y] else TRANSPARENT
        },
    )

    /**
     * This image with [image] put over it at [left], [top], replacing what is under it (see-through pixels too), the part
     * reaching past this image left out.
     */
    public fun withPasted(image: PixelImage, left: Int, top: Int): PixelImage {
        val copy = pixels.copyOf()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val targetX = left + x
                val targetY = top + y
                if (contains(targetX, targetY)) copy[targetY * width + targetX] = image[x, y]
            }
        }
        return PixelImage(width, height, copy)
    }

    /**
     * Every pixel, row by row from the top left.
     */
    public fun toIntArray(): IntArray = pixels.copyOf()

    /**
     * The colors this image uses, see-through ones left out, the most used first.
     */
    public fun colors(): List<Int> = pixels.filter { it ushr 24 != 0 }.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

    /**
     * This image as a PNG.
     */
    public fun toPng(): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, width, height, pixels, 0, width)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    override fun equals(other: Any?): Boolean = other is PixelImage && other.width == width && other.height == height && other.pixels.contentEquals(pixels)

    override fun hashCode(): Int = 31 * (31 * width + height) + pixels.contentHashCode()

    override fun toString(): String = "PixelImage($width by $height)"

    public companion object {
        public const val TRANSPARENT: Int = 0

        /**
         * The image an encoded PNG (or JPEG or WebP, as far as `javax.imageio` reads them) holds, or null if it can't be read.
         */
        public fun of(bytes: ByteArray): PixelImage? = try {
            ImageIO.read(bytes.inputStream())?.let { image ->
                PixelImage(image.width, image.height, image.getRGB(0, 0, image.width, image.height, null, 0, image.width))
            }
        } catch (_: Exception) {
            // ImageIO throws unchecked exceptions too on a malformed image.
            null
        }
    }
}

/**
 * One pixel of a [PixelImage] and its color, `0xAARRGGBB`.
 */
public data class Pixel(
    val x: Int,
    val y: Int,
    val color: Int,
)
