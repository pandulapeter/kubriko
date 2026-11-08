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
import com.pandulapeter.kubriko.sprites.implementation.AtlasState
import com.pandulapeter.kubriko.sprites.implementation.PackedAtlas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.DrawableResource

/**
 * Sprites a game declared as belonging together, packed onto shared bitmaps (pages) by [SpriteManager.loadAtlas] or
 * [SpriteManager.loadSpriteAtlas], so that batched actors showing any of them draw in one run (see [SpriteRegion]).
 *
 * The sprites are loaded and packed in the background, off the main thread. Until that is done, [getRegion] returns
 * null for every sprite, as [SpriteManager.get] does for a sprite still loading. Each sprite is surrounded by
 * [paddingInPixels] copies of its own edge pixels, so filtering at its border never picks up a neighbor. A sprite sheet
 * is packed whole, so the frames of an [AnimatedSprite] (see [AnimatedSprite.fromAtlas]) stay regions of one page. A
 * sprite too large for a page keeps a bitmap of its own, and its region is all of it.
 *
 * Pages are no larger than [maximumPageSize] in either direction, and only as large as what they hold. They are marked
 * immutable, so the Skia targets draw them without copying their pixels, and [SpriteManager.getMipChain] gives each
 * page a mip chain of its own.
 */
class SpriteAtlas internal constructor(
    /** The sprites this atlas packs, each once, in the order they were declared. */
    val resources: List<SpriteResource>,
    /** The largest width and height a page may have, in pixels. */
    val maximumPageSize: Int,
    /** How many pixels of its own edges surround each sprite on its page. */
    val paddingInPixels: Int,
) {
    private val state = MutableStateFlow<AtlasState>(AtlasState.Loading)
    private val _loadingProgress = MutableStateFlow(0f)

    /**
     * The share of the atlas that is ready, from 0 to 1: it grows as its sprites load, and reaches 1 only once its
     * pages are packed and ready to draw. It stays where it is if the atlas is unloaded before that.
     */
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    /** Whether the pages are packed and [getRegion] returns the sprites' regions. */
    val isLoaded get() = state.value is PackedAtlas

    /**
     * The packed pages, or an empty list until [isLoaded] (and again once the atlas is unloaded). A sprite too large
     * for a page keeps its own bitmap, which isn't listed here, and so does every sprite if the pages could not be
     * drawn (Android can't draw a hardware bitmap into one), leaving this empty.
     */
    val pages: List<ImageBitmap> get() = (state.value as? PackedAtlas)?.pages.orEmpty()

    /**
     * Where the sprite [resource] lies on this atlas's pages, or null while the atlas is loading, after it was
     * unloaded, or if it doesn't hold [resource]. Allocates nothing, so it can be called every frame.
     */
    fun getRegion(resource: SpriteResource): SpriteRegion? = (state.value as? PackedAtlas)?.regions?.get(resource)

    /**
     * Where the unrotated sprite of [drawableResource] lies on this atlas's pages, or null while the atlas is loading,
     * after it was unloaded, or if it doesn't hold that sprite. Allocates nothing, so it can be called every frame.
     */
    fun getRegion(drawableResource: DrawableResource): SpriteRegion? = (state.value as? PackedAtlas)?.unrotatedRegions?.get(drawableResource)

    internal val isUnloaded get() = state.value == AtlasState.Unloaded

    internal val packedAtlas get() = state.value as? PackedAtlas

    internal fun reportLoadedSprites(loadedCount: Int) {
        val progress = loadedCount.toFloat() / (resources.size + 1)
        _loadingProgress.update { if (state.value == AtlasState.Loading) maxOf(it, progress) else it }
    }

    /** Publishes [packedAtlas] unless the atlas was unloaded meanwhile, and returns whether it did. */
    internal fun publish(packedAtlas: PackedAtlas): Boolean {
        var isPublished = false
        state.update {
            isPublished = it == AtlasState.Loading
            if (isPublished) packedAtlas else it
        }
        if (isPublished) _loadingProgress.value = 1f
        return isPublished
    }

    internal fun markUnloaded() {
        state.value = AtlasState.Unloaded
    }

    /** Default values for the settings of a new atlas. */
    companion object {
        /** A page size that every target can use as a texture. */
        const val DEFAULT_MAXIMUM_PAGE_SIZE = 2048

        /**
         * Two pixels: one is enough for interpolated sampling at the sprite's own size, and the second keeps the
         * first, half-sized level of a page's mip chain from mixing neighbors too.
         */
        const val DEFAULT_PADDING_IN_PIXELS = 2
    }
}
