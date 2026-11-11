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

/**
 * What the sprite editor does to a whole image (a frame, or each of them): each returns a new image.
 */
public object PixelOperations {

    public fun flippedHorizontally(image: PixelImage): PixelImage = mapped(image, image.width, image.height) { x, y -> image[image.width - 1 - x, y] }

    public fun flippedVertically(image: PixelImage): PixelImage = mapped(image, image.width, image.height) { x, y -> image[x, image.height - 1 - y] }

    /**
     * [image] turned by 90 degrees, clockwise or not, so its width and height swap.
     */
    public fun rotated(image: PixelImage, isClockwise: Boolean): PixelImage = mapped(image, image.height, image.width) { x, y ->
        if (isClockwise) image[y, image.height - 1 - x] else image[image.width - 1 - y, x]
    }

    /**
     * [image] moved by [dx] to the right and [dy] down, what leaves one edge coming back at the other.
     */
    public fun shifted(image: PixelImage, dx: Int, dy: Int): PixelImage = mapped(image, image.width, image.height) { x, y ->
        image[Math.floorMod(x - dx, image.width), Math.floorMod(y - dy, image.height)]
    }

    /**
     * [image] in a canvas of [width] by [height] pixels, placed by [anchor]: cut where it is larger, see-through where the
     * canvas is.
     */
    public fun resizedCanvas(image: PixelImage, width: Int, height: Int, anchor: CanvasAnchor): PixelImage {
        val left = anchor.offsetAcross(width - image.width)
        val top = anchor.offsetDown(height - image.height)
        return PixelImage(width, height).withPasted(image, left, top)
    }

    /**
     * [image] with each pixel [factor] by [factor] pixels, the way pixel art is scaled up.
     */
    public fun scaled(image: PixelImage, factor: Int): PixelImage {
        require(factor >= 1) { "Images are scaled up by a whole number from 1, not $factor." }
        return mapped(image, image.width * factor, image.height * factor) { x, y -> image[x / factor, y / factor] }
    }

    private inline fun mapped(image: PixelImage, width: Int, height: Int, pixelAt: (x: Int, y: Int) -> Int): PixelImage =
        PixelImage(width, height, IntArray(width * height) { index -> pixelAt(index % width, index / width) })
}
