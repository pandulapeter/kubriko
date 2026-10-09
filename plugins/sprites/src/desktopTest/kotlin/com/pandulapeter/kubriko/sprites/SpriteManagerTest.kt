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
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class SpriteManagerTest {

    private val requestedResources: MutableList<SpriteResource> = Collections.synchronizedList(mutableListOf())
    private val pendingLoads = ConcurrentHashMap<SpriteResource, CompletableDeferred<ImageBitmap>>()
    private val spriteManager = SpriteManagerImpl(
        isLoggingEnabled = false,
        instanceNameForLogging = null,
        imageLoader = { resource ->
            requestedResources.add(resource)
            pendingLoadOf(resource).await()
        },
    )
    private val kubriko: ManualKubriko = newManualKubriko(spriteManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun getReturnsNullWhileLoadingAndTheBitmapOnceLoaded() {
        val resource = spriteResource("sprite")
        val bitmap = FakeImageBitmap()

        assertNull(spriteManager.get(resource))
        finishLoading(resource, bitmap)

        awaitCondition { spriteManager.get(resource) != null }
        assertSame(bitmap, spriteManager.get(resource))
    }

    @Test
    fun repeatedRequestsLoadAResourceOnce() {
        val resource = spriteResource("sprite")

        repeat(10) { spriteManager.get(resource) }
        finishLoading(resource, FakeImageBitmap())
        awaitCondition { spriteManager.get(resource) != null }

        assertEquals(listOf(resource), requestedResources.toList())
    }

    @Test
    fun differentRotationsOfOneDrawableAreSeparateSprites() {
        val drawableResource = drawableResource("sprite")
        val upright = SpriteResource(drawableResource)
        val sideways = SpriteResource(drawableResource, SpriteResource.Rotation.DEGREES_90)
        val uprightBitmap = FakeImageBitmap()
        val sidewaysBitmap = FakeImageBitmap()

        spriteManager.preloadSprites(upright, sideways)
        finishLoading(upright, uprightBitmap)
        finishLoading(sideways, sidewaysBitmap)

        awaitCondition { spriteManager.get(upright) != null && spriteManager.get(sideways) != null }
        assertSame(uprightBitmap, spriteManager.get(upright))
        assertSame(sidewaysBitmap, spriteManager.get(sideways))
    }

    @Test
    fun drawableResourceOverloadsShareTheUnrotatedSprite() {
        val drawableResource = drawableResource("sprite")
        val bitmap = FakeImageBitmap()

        spriteManager.preload(drawableResource)
        finishLoading(SpriteResource(drawableResource), bitmap)

        awaitCondition { spriteManager.get(drawableResource) != null }
        assertSame(bitmap, spriteManager.get(SpriteResource(drawableResource)))
        assertEquals(1, requestedResources.size)
    }

    @Test
    fun loadingProgressReportsTheLoadedShare() {
        val loaded = spriteResource("loaded")
        val loading = spriteResource("loading")
        spriteManager.preloadSprites(loaded, loading)

        finishLoading(loaded, FakeImageBitmap())

        awaitProgress(listOf(loaded, loading), 0.5f)
        finishLoading(loading, FakeImageBitmap())
        awaitProgress(listOf(loaded, loading), 1f)
    }

    @Test
    fun loadingProgressOfNoResourcesIsComplete() {
        awaitProgress(emptyList(), 1f)
        runBlocking {
            assertEquals(1f, spriteManager.getLoadingProgress(emptyList()).first())
        }
    }

    @Test
    fun unloadedSpriteIsLoadedAgainOnTheNextRequest() {
        val resource = spriteResource("sprite")
        spriteManager.get(resource)
        finishLoading(resource, FakeImageBitmap())
        awaitCondition { spriteManager.get(resource) != null }
        pendingLoads.remove(resource)

        spriteManager.unload(resource)

        assertNull(spriteManager.get(resource))
        awaitCondition { requestedResources.size == 2 }
    }

    private fun finishLoading(resource: SpriteResource, bitmap: ImageBitmap) {
        pendingLoadOf(resource).complete(bitmap)
    }

    private fun pendingLoadOf(resource: SpriteResource) = pendingLoads.computeIfAbsent(resource) { CompletableDeferred() }

    private fun awaitProgress(resources: List<SpriteResource>, progress: Float) = runBlocking {
        withTimeout(2_000) { spriteManager.getSpriteLoadingProgress(resources).first { it == progress } }
    }

    @OptIn(InternalResourceApi::class)
    private fun drawableResource(path: String) = DrawableResource(path, emptySet())

    private fun spriteResource(path: String) = SpriteResource(drawableResource(path))
}
