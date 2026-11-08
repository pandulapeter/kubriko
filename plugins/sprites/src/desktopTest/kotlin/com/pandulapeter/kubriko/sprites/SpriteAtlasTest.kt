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
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.sprites.implementation.AtlasPageBuilder
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpriteAtlasTest {

    private val pendingLoads = ConcurrentHashMap<SpriteResource, CompletableDeferred<ImageBitmap>>()
    private val builtPages: MutableList<BuiltPage> = Collections.synchronizedList(mutableListOf())
    private var pageBuilder = AtlasPageBuilder { width, height, sprites, xs, ys, padding ->
        FakeImageBitmap(width, height).also { builtPages += BuiltPage(it, sprites, xs.toList(), ys.toList(), padding) }
    }
    private val spriteManager = SpriteManagerImpl(
        isLoggingEnabled = false,
        instanceNameForLogging = null,
        imageLoader = { resource -> pendingLoadOf(resource).await() },
        mipChains = TextureMipChainCache { base ->
            TextureMipChain(base, TextureMipChain.DEFAULT_LEVEL_COUNT, downscale = { _, width, height -> FakeImageBitmap(width, height) }, finish = { it })
        },
        atlasPageBuilder = { width, height, sprites, xs, ys, padding -> pageBuilder.build(width, height, sprites, xs, ys, padding) },
    )
    private val kubriko: ManualKubriko = newManualKubriko(spriteManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun `regions are null until every sprite is loaded and packed onto one page`() {
        val first = spriteResource("first")
        val second = spriteResource("second")
        val firstBitmap = FakeImageBitmap(10, 20)
        val secondBitmap = FakeImageBitmap(30, 5)

        val atlas = spriteManager.loadSpriteAtlas(listOf(first, second), paddingInPixels = 1)
        finishLoading(first, firstBitmap)
        awaitCondition { atlas.loadingProgress.value > 0f }

        assertEquals(1f / 3f, atlas.loadingProgress.value)
        assertNull(atlas.getRegion(first))
        assertFalse(atlas.isLoaded)
        finishLoading(second, secondBitmap)
        awaitCondition { atlas.isLoaded }

        val page = builtPages.single()
        assertEquals(listOf(firstBitmap, secondBitmap), page.sprites)
        assertEquals(1f, atlas.loadingProgress.value)
        assertEquals(listOf<ImageBitmap>(page.bitmap), atlas.pages)
        assertEquals(1, page.padding)
        assertEquals(SpriteRegion(page.bitmap, IntOffset(page.xs[0], page.ys[0]), IntSize(10, 20)), atlas.getRegion(first))
        assertEquals(SpriteRegion(page.bitmap, IntOffset(page.xs[1], page.ys[1]), IntSize(30, 5)), atlas.getRegion(second))
    }

    @Test
    fun `sprites are found by their drawable and through the manager`() {
        val drawable = drawableResource("sprite")
        val rotated = SpriteResource(drawable, SpriteResource.Rotation.DEGREES_90)

        val atlas = spriteManager.loadSpriteAtlas(listOf(SpriteResource(drawable), rotated))
        finishLoading(SpriteResource(drawable), FakeImageBitmap(4, 4))
        finishLoading(rotated, FakeImageBitmap(4, 4))
        awaitCondition { atlas.isLoaded }

        val region = assertNotNull(atlas.getRegion(drawable))
        assertSame(region, atlas.getRegion(SpriteResource(drawable)))
        assertNotSame(region, atlas.getRegion(rotated))
        assertSame(region, spriteManager.getRegion(drawable))
        assertSame(atlas.getRegion(rotated), spriteManager.getRegion(rotated))
        assertNull(spriteManager.getRegion(drawableResource("elsewhere")))
    }

    @Test
    fun `a sprite listed twice is packed once`() {
        val resource = spriteResource("sprite")

        val atlas = spriteManager.loadAtlas(listOf(resource.drawableResource, resource.drawableResource))
        finishLoading(resource, FakeImageBitmap(4, 4))
        awaitCondition { atlas.isLoaded }

        assertEquals(listOf(resource), atlas.resources)
        assertEquals(1, builtPages.single().sprites.size)
    }

    @Test
    fun `a sprite too large for a page keeps its own bitmap`() {
        val small = spriteResource("small")
        val large = spriteResource("large")
        val largeBitmap = FakeImageBitmap(100, 10)

        val atlas = spriteManager.loadSpriteAtlas(listOf(small, large), maximumPageSize = 64)
        finishLoading(small, FakeImageBitmap(4, 4))
        finishLoading(large, largeBitmap)
        awaitCondition { atlas.isLoaded }

        val largeRegion = assertNotNull(atlas.getRegion(large))
        assertEquals(SpriteRegion(largeBitmap, IntOffset.Zero, IntSize(100, 10)), largeRegion)
        assertSame(builtPages.single().bitmap, atlas.getRegion(small)?.page)
        assertSame(largeBitmap, spriteManager.getMipChain(largeRegion)?.base)
    }

    @Test
    fun `sprites keep their own bitmaps when a page can't be drawn`() {
        pageBuilder = AtlasPageBuilder { _, _, _, _, _, _ -> throw IllegalStateException("Hardware bitmap") }
        val resource = spriteResource("sprite")
        val bitmap = FakeImageBitmap(4, 4)

        val atlas = spriteManager.loadSpriteAtlas(listOf(resource))
        finishLoading(resource, bitmap)
        awaitCondition { atlas.isLoaded }

        assertEquals(emptyList(), atlas.pages)
        assertEquals(SpriteRegion(bitmap, IntOffset.Zero, IntSize(4, 4)), atlas.getRegion(resource))
    }

    @Test
    fun `an atlas unloaded while loading is never packed`() {
        val resource = spriteResource("sprite")

        val atlas = spriteManager.loadSpriteAtlas(listOf(resource))
        spriteManager.unloadAtlas(atlas)
        finishLoading(resource, FakeImageBitmap(4, 4))
        Thread.sleep(300)

        assertFalse(atlas.isLoaded)
        assertTrue(builtPages.isEmpty())
        assertNull(spriteManager.getRegion(resource))
    }

    @Test
    fun `an unloaded atlas releases its regions and the mip chains of its pages`() {
        val first = spriteResource("first")
        val second = spriteResource("second")
        val atlas = spriteManager.loadSpriteAtlas(listOf(first, second))
        finishLoading(first, FakeImageBitmap(8, 8))
        finishLoading(second, FakeImageBitmap(8, 8))
        awaitCondition { atlas.isLoaded }
        val firstRegion = assertNotNull(atlas.getRegion(first))
        val chain = assertNotNull(spriteManager.getMipChain(firstRegion))

        assertSame(chain, spriteManager.getMipChain(assertNotNull(atlas.getRegion(second))))
        assertSame(firstRegion.page, chain.base)
        spriteManager.unloadAtlas(atlas)

        assertFalse(atlas.isLoaded)
        assertNull(atlas.getRegion(first))
        assertNull(spriteManager.getRegion(first))
        assertEquals(emptyList(), atlas.pages)
        assertNull(spriteManager.getMipChain(firstRegion))
    }

    @Test
    fun `a region on no loaded atlas has no mip chain`() {
        assertNull(spriteManager.getMipChain(SpriteRegion(FakeImageBitmap(4, 4), IntOffset.Zero, IntSize(4, 4))))
    }

    @Test
    fun `an atlas of nothing loads with no pages`() {
        val atlas = spriteManager.loadSpriteAtlas(emptyList())

        awaitCondition { atlas.isLoaded }

        assertEquals(emptyList(), atlas.pages)
        assertEquals(1f, atlas.loadingProgress.value)
    }

    @Test
    fun `showing atlas regions every frame allocates nothing`() {
        val sprite = spriteResource("sprite")
        val sheet = spriteResource("sheet")
        val atlas = spriteManager.loadSpriteAtlas(listOf(sprite, sheet))
        finishLoading(sprite, FakeImageBitmap(4, 4))
        finishLoading(sheet, FakeImageBitmap(8, 8))
        awaitCondition { atlas.isLoaded }
        val spriteLook = SpriteLook(texture = null)
        val sheetLook = SpriteLook(texture = null)
        val animatedSprite = AnimatedSprite.fromAtlas(
            getRegion = { spriteManager.getRegion(sheet) },
            frameSize = IntSize(4, 4),
            frameCount = 4,
            framesPerRow = 2,
        )

        val bytes = measureAllocatedBytesPerRun {
            spriteLook.setRegion(spriteManager.getRegion(sprite.drawableResource))
            animatedSprite.stepForward(deltaTimeInMilliseconds = 16, shouldLoop = true)
            animatedSprite.applyTo(sheetLook)
        }

        assertTrue(bytes < 1, "$bytes B per frame of showing atlas regions")
        assertSame(atlas.pages.single(), sheetLook.texture)
    }

    @Test
    fun `settings that leave no room are refused`() {
        assertFailsWith<IllegalArgumentException> { spriteManager.loadSpriteAtlas(emptyList(), maximumPageSize = 4, paddingInPixels = 2) }
        assertFailsWith<IllegalArgumentException> { spriteManager.loadSpriteAtlas(emptyList(), paddingInPixels = -1) }
    }

    private class BuiltPage(val bitmap: ImageBitmap, val sprites: List<ImageBitmap>, val xs: List<Int>, val ys: List<Int>, val padding: Int)

    private fun finishLoading(resource: SpriteResource, bitmap: ImageBitmap) {
        pendingLoadOf(resource).complete(bitmap)
    }

    private fun pendingLoadOf(resource: SpriteResource) = pendingLoads.computeIfAbsent(resource) { CompletableDeferred() }

    @OptIn(InternalResourceApi::class)
    private fun drawableResource(path: String) = DrawableResource(path, emptySet())

    private fun spriteResource(path: String) = SpriteResource(drawableResource(path))
}
