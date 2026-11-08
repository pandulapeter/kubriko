/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Where a sprite lies: a rectangle of a [page] bitmap. A [SpriteAtlas] hands these out for the sprites it packed, and a
 * [com.pandulapeter.kubriko.actor.looks.SpriteLook] shows one through [setRegion]. Sprites on the same page draw
 * together, so a run of batched actors continues across all of them.
 *
 * @param page The bitmap holding the sprite: an atlas page, or the sprite's own bitmap if it was too large for one.
 * @param offset The sprite's top-left corner on the [page], in pixels.
 * @param size The sprite's size, in pixels.
 */
class SpriteRegion(
    val page: ImageBitmap,
    val offset: IntOffset,
    val size: IntSize,
) {
    /** A region is equal to another one of the same [page] (by identity), [offset], and [size]. */
    override fun equals(other: Any?) = other is SpriteRegion && other.page === page && other.offset == offset && other.size == size

    override fun hashCode() = (page.hashCode() * 31 + offset.hashCode()) * 31 + size.hashCode()

    override fun toString() = "SpriteRegion(page=${page.width}x${page.height}, offset=$offset, size=$size)"
}
