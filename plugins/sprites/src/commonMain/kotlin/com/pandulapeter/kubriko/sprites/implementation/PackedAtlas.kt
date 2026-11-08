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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.sprites.SpriteRegion
import com.pandulapeter.kubriko.sprites.SpriteResource
import org.jetbrains.compose.resources.DrawableResource

/** Where a [com.pandulapeter.kubriko.sprites.SpriteAtlas] is in its life. */
internal sealed interface AtlasState {
    data object Loading : AtlasState
    data object Unloaded : AtlasState
}

/** A finished atlas: its pages, and the region of every sprite it holds. */
internal class PackedAtlas(
    val pages: List<ImageBitmap>,
    val regions: Map<SpriteResource, SpriteRegion>,
) : AtlasState {
    val unrotatedRegions: Map<DrawableResource, SpriteRegion> = regions.entries
        .filter { it.key.rotation == SpriteResource.Rotation.NONE }
        .associate { it.key.drawableResource to it.value }

    /** Every bitmap a region of this atlas lies on: its pages, and the sprites that kept their own. */
    val bitmaps: Set<ImageBitmap> = regions.values.mapTo(HashSet()) { it.page }
}

/**
 * Packs the loaded [bitmaps] of [resources] onto pages: lays them out by size ([AtlasPacker]), then has [pageBuilder]
 * copy their pixels. A sprite the layout leaves off every page keeps its own bitmap as its region.
 */
internal fun packAtlas(
    resources: List<SpriteResource>,
    bitmaps: List<ImageBitmap>,
    maximumPageSize: Int,
    padding: Int,
    pageBuilder: AtlasPageBuilder,
): PackedAtlas {
    val layout = AtlasPacker.pack(
        widths = IntArray(bitmaps.size) { bitmaps[it].width },
        heights = IntArray(bitmaps.size) { bitmaps[it].height },
        maximumPageSize = maximumPageSize,
        padding = padding,
    )
    val pages = List(layout.pageCount) { pageIndex ->
        val members = bitmaps.indices.filter { layout.pageIndices[it] == pageIndex }
        pageBuilder.build(
            width = layout.pageWidths[pageIndex],
            height = layout.pageHeights[pageIndex],
            sprites = members.map { bitmaps[it] },
            xs = IntArray(members.size) { layout.xs[members[it]] },
            ys = IntArray(members.size) { layout.ys[members[it]] },
            padding = padding,
        )
    }
    val regions = resources.indices.associate { index ->
        val bitmap = bitmaps[index]
        val pageIndex = layout.pageIndices[index]
        resources[index] to if (pageIndex == AtlasLayout.NOT_ON_A_PAGE) {
            SpriteRegion(bitmap, IntOffset.Zero, IntSize(bitmap.width, bitmap.height))
        } else {
            SpriteRegion(pages[pageIndex], IntOffset(layout.xs[index], layout.ys[index]), IntSize(bitmap.width, bitmap.height))
        }
    }
    return PackedAtlas(pages, regions)
}

/** An atlas whose sprites each keep their own bitmap, for when packing them onto pages failed. */
internal fun unpackedAtlas(resources: List<SpriteResource>, bitmaps: List<ImageBitmap>) = PackedAtlas(
    pages = emptyList(),
    regions = resources.indices.associate { index ->
        val bitmap = bitmaps[index]
        resources[index] to SpriteRegion(bitmap, IntOffset.Zero, IntSize(bitmap.width, bitmap.height))
    },
)
