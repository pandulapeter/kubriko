/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.implementation

/** Receives one copy of a source rectangle onto a destination rectangle, stretched with nearest-neighbor sampling. */
internal fun interface AtlasBlitTarget {
    fun blit(srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, dstX: Int, dstY: Int, dstWidth: Int, dstHeight: Int)
}

/**
 * The copies that put a sprite of [width] × [height] onto a page at ([x], [y]), padded by [padding] pixels of its own
 * edges: the sprite itself, each edge row or column stretched outward, and each corner pixel stretched into its corner.
 * Filtering near the sprite's border then samples only its own pixels, never a neighbor's.
 */
internal fun forEachAtlasBlit(width: Int, height: Int, x: Int, y: Int, padding: Int, target: AtlasBlitTarget) {
    target.blit(0, 0, width, height, x, y, width, height)
    if (padding <= 0) return
    val right = width - 1
    val bottom = height - 1
    target.blit(0, 0, 1, height, x - padding, y, padding, height)
    target.blit(right, 0, 1, height, x + width, y, padding, height)
    target.blit(0, 0, width, 1, x, y - padding, width, padding)
    target.blit(0, bottom, width, 1, x, y + height, width, padding)
    target.blit(0, 0, 1, 1, x - padding, y - padding, padding, padding)
    target.blit(right, 0, 1, 1, x + width, y - padding, padding, padding)
    target.blit(0, bottom, 1, 1, x - padding, y + height, padding, padding)
    target.blit(right, bottom, 1, 1, x + width, y + height, padding, padding)
}
