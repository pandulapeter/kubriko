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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.sprites.helpers.toSpriteResource
import com.pandulapeter.kubriko.sprites.implementation.AtlasPageBuilder
import com.pandulapeter.kubriko.sprites.implementation.CanvasAtlasPageBuilder
import com.pandulapeter.kubriko.sprites.implementation.PackedAtlas
import com.pandulapeter.kubriko.sprites.implementation.packAtlas
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import com.pandulapeter.kubriko.sprites.implementation.unpackedAtlas
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import kotlin.time.Duration.Companion.milliseconds

internal class SpriteManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
    private val initialRetryDelayInMilliseconds: Long = INITIAL_RETRY_DELAY_MS,
    private val imageLoader: (suspend (SpriteResource) -> ImageBitmap?)? = null,
    private val mipChains: TextureMipChainCache = TextureMipChainCache(),
    private val atlasPageBuilder: AtlasPageBuilder = CanvasAtlasPageBuilder,
) : SpriteManager(isLoggingEnabled, instanceNameForLogging) {

    private val cache = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap?>())
    private val pendingWarmingUp = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap>())
    private val atlases = MutableStateFlow<List<SpriteAtlas>>(emptyList())
    private val pendingAtlases = MutableStateFlow(persistentMapOf<SpriteAtlas, PackedAtlas>())

    // One canonical SpriteResource per DrawableResource. Games reach cached sprites through the
    // DrawableResource overload from their drawing code and from AnimatedSprite callbacks, which
    // otherwise wraps the resource afresh on every one of those lookups.
    private val defaultSpriteResources = MutableStateFlow(persistentMapOf<DrawableResource, SpriteResource>())

    override fun getLoadingProgress(drawableResources: Collection<DrawableResource>) = if (drawableResources.isEmpty()) flowOf(1f) else
        getSpriteLoadingProgress(drawableResources.map { it.toSpriteResource() })

    override fun preload(vararg drawableResources: DrawableResource) = preload(drawableResources.toList())

    override fun preload(drawableResources: Collection<DrawableResource>) {
        drawableResources.forEach { get(it) }
    }

    override fun get(drawableResource: DrawableResource): ImageBitmap? =
        get(drawableResource.asDefaultSpriteResource())

    private fun DrawableResource.asDefaultSpriteResource(): SpriteResource {
        defaultSpriteResources.value[this]?.let { return it }
        val spriteResource = toSpriteResource()
        defaultSpriteResources.update { current ->
            if (current.containsKey(this)) current else current.putting(this, spriteResource)
        }
        // Read back rather than returning the local: a concurrent caller may have won the update, and
        // every lookup has to land on the same instance for this to be a cache at all.
        return defaultSpriteResources.value.getValue(this)
    }

    override fun unload(drawableResource: DrawableResource) = unload(drawableResource.toSpriteResource())

    override fun getSpriteLoadingProgress(resources: Collection<SpriteResource>): Flow<Float> {
        if (resources.isEmpty()) return flowOf(1f)
        return cache.map { currentCache ->
            var loadedCount = 0
            for (res in resources) {
                if (currentCache[res] != null) {
                    loadedCount++
                }
            }
            loadedCount.toFloat() / resources.size
        }.distinctUntilChanged()
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        val pending = pendingWarmingUp.collectAsState().value
        val pendingAtlasPages = pendingAtlases.collectAsState().value
        if (pending.isNotEmpty() || pendingAtlasPages.isNotEmpty()) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    pending.values.forEach { warmUp(it) }
                    pendingAtlasPages.values.forEach { packedAtlas -> packedAtlas.bitmaps.forEach { warmUp(it) } }
                },
            )
            LaunchedEffect(pending.keys) {
                pending.keys.forEach { promoteToCache(it) }
            }
            LaunchedEffect(pendingAtlasPages.keys) {
                pendingAtlasPages.keys.forEach { promoteAtlas(it) }
            }
        }
    }

    private fun DrawScope.warmUp(bitmap: ImageBitmap) = drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(1, 1),
        alpha = WARM_UP_ALPHA,
    )

    override fun preloadSprites(vararg resources: SpriteResource) = preloadSprites(resources.toList())

    override fun preloadSprites(resources: Collection<SpriteResource>) = resources.forEach { get(it) }

    override fun get(resource: SpriteResource): ImageBitmap? {
        val currentCache = cache.value
        val bitmap = currentCache[resource]
        if (bitmap != null) return bitmap
        if (resource in currentCache) return null
        if (resource in pendingWarmingUp.value) return null
        cache.update { it.putting(resource, null) }
        scope.launch {
            // A load that finishes after unload() is dropped, so the sprite does not become resident again.
            val loadedBitmap = loadUntilLoaded(resource) { resource in cache.value } ?: return@launch
            var isPublished = false
            pendingWarmingUp.update {
                isPublished = resource in cache.value
                if (isPublished) it.putting(resource, loadedBitmap) else it
            }
            if (isPublished) {
                delay(WARM_UP_TIMEOUT_MS.milliseconds)
                promoteToCache(resource)
            }
        }
        return null
    }

    internal fun promoteToCache(resource: SpriteResource) {
        val bitmap = pendingWarmingUp.value[resource] ?: return
        pendingWarmingUp.update { it.removing(resource) }
        cache.update { it.putting(resource, bitmap) }
    }

    override fun unload(resource: SpriteResource) {
        pendingWarmingUp.update { it.removing(resource) }
        cache.update { it.removing(resource) }
        mipChains.remove(resource)
    }

    override fun getMipChain(drawableResource: DrawableResource) = getMipChain(drawableResource.asDefaultSpriteResource())

    override fun getMipChain(resource: SpriteResource) = get(resource)?.let { mipChains.get(resource, it) }

    override fun getMipChain(region: SpriteRegion): TextureMipChain? {
        val loadedAtlases = atlases.value
        for (index in loadedAtlases.indices) {
            val packedAtlas = loadedAtlases[index].packedAtlas ?: continue
            if (region.page in packedAtlas.bitmaps) return packedAtlas.mipChains.get(region.page, region.page)
        }
        return null
    }

    override fun loadAtlas(
        drawableResources: Collection<DrawableResource>,
        maximumPageSize: Int,
        paddingInPixels: Int,
    ) = loadSpriteAtlas(drawableResources.map { it.asDefaultSpriteResource() }, maximumPageSize, paddingInPixels)

    override fun loadSpriteAtlas(
        resources: Collection<SpriteResource>,
        maximumPageSize: Int,
        paddingInPixels: Int,
    ): SpriteAtlas {
        require(paddingInPixels >= 0) { "The padding can't be negative." }
        require(maximumPageSize > paddingInPixels * 2) { "A page must have room for more than its padding." }
        val atlas = SpriteAtlas(resources.distinct(), maximumPageSize, paddingInPixels)
        atlases.update { it + atlas }
        scope.launch { loadAndPack(atlas) }
        return atlas
    }

    private suspend fun loadAndPack(atlas: SpriteAtlas) {
        val loadedCount = MutableStateFlow(0)
        val bitmaps = coroutineScope {
            atlas.resources.map { resource ->
                async {
                    loadUntilLoaded(resource, maximumAttempts = ATLAS_ATTEMPTS) { !atlas.isUnloaded }?.also {
                        atlas.reportLoadedSprites(loadedCount.updateAndGet { count -> count + 1 })
                    }
                }
            }.awaitAll()
        }
        if (atlas.isUnloaded) return
        val loadedIndices = bitmaps.indices.filter { bitmaps[it] != null }
        val loadedResources = loadedIndices.map { atlas.resources[it] }
        val loadedBitmaps = loadedIndices.map { checkNotNull(bitmaps[it]) }
        val packedAtlas = try {
            packAtlas(loadedResources, loadedBitmaps, atlas.maximumPageSize, atlas.paddingInPixels, atlasPageBuilder, newAtlasMipChains())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Android cannot draw a hardware bitmap (which a custom image loader may return) into a page.
            log(message = "Failed to pack an atlas, its sprites keep their own bitmaps", details = exception.message)
            unpackedAtlas(loadedResources, loadedBitmaps, newAtlasMipChains())
        }
        pendingAtlases.update { it.putting(atlas, packedAtlas) }
        delay(WARM_UP_TIMEOUT_MS.milliseconds)
        promoteAtlas(atlas)
        // One sprite that keeps failing doesn't hold back the others: it joins later, keeping a bitmap of its own.
        atlas.resources.filterIndexed { index, _ -> bitmaps[index] == null }.forEach { resource ->
            scope.launch {
                val bitmap = loadUntilLoaded(resource) { !atlas.isUnloaded } ?: return@launch
                atlas.addOwnBitmap(resource, bitmap)
            }
        }
    }

    internal fun promoteAtlas(atlas: SpriteAtlas) {
        val packedAtlas = pendingAtlases.value[atlas] ?: return
        pendingAtlases.update { it.removing(atlas) }
        atlas.publish(packedAtlas)
    }

    override fun unloadAtlas(atlas: SpriteAtlas) {
        atlases.update { it - atlas }
        pendingAtlases.update { it.removing(atlas) }
        atlas.markUnloaded()
    }

    override fun getRegion(drawableResource: DrawableResource): SpriteRegion? {
        val loadedAtlases = atlases.value
        for (index in loadedAtlases.indices) {
            loadedAtlases[index].getRegion(drawableResource)?.let { return it }
        }
        return null
    }

    override fun getRegion(resource: SpriteResource): SpriteRegion? {
        val loadedAtlases = atlases.value
        for (index in loadedAtlases.indices) {
            loadedAtlases[index].getRegion(resource)?.let { return it }
        }
        return null
    }

    /** The mip chains of one atlas's bitmaps, which go with it when it is unloaded, built as this manager's other chains are. */
    private fun newAtlasMipChains() = TextureMipChainCache(mipChains.createChain)

    /**
     * Loads [resource], retrying a failed load with a growing delay, until it loads or [isStillWanted] turns false
     * (then it returns null).
     */
    private suspend fun loadUntilLoaded(resource: SpriteResource, maximumAttempts: Int = Int.MAX_VALUE, isStillWanted: () -> Boolean): ImageBitmap? {
        var retryDelayInMilliseconds = initialRetryDelayInMilliseconds
        var isFirstAttempt = true
        var attempts = 0
        while (true) {
            val loadedBitmap = loadImage(resource, isFirstAttempt)
            if (!isStillWanted()) return null
            if (loadedBitmap != null) return loadedBitmap
            if (++attempts >= maximumAttempts) return null
            isFirstAttempt = false
            delay(retryDelayInMilliseconds.milliseconds)
            if (!isStillWanted()) return null
            retryDelayInMilliseconds = (retryDelayInMilliseconds * 2).coerceAtMost(MAXIMUM_RETRY_DELAY_MS)
        }
    }

    private suspend fun loadImage(spriteResource: SpriteResource, isFirstAttempt: Boolean): ImageBitmap? = try {
        if (imageLoader == null) decodeImage(spriteResource) else imageLoader(spriteResource)
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        // Kotlin/Wasm surfaces a rejected JavaScript promise as a JsException, which is not an Exception.
        if (isFirstAttempt) {
            throwable.printStackTrace()
        } else {
            log(
                message = "Failed to load $spriteResource again, retrying",
                details = throwable.message,
            )
        }
        null
    }

    @OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
    private suspend fun decodeImage(spriteResource: SpriteResource): ImageBitmap = getDrawableResourceBytes(
        getSystemResourceEnvironment(),
        spriteResource.drawableResource
    ).toImageBitmap(
        DensityQualifier.MDPI.dpi,
        DensityQualifier.MDPI.dpi,
        spriteResource.rotation
    )

    companion object {
        private const val WARM_UP_TIMEOUT_MS = 100L

        /** Skia quick-rejects a fully transparent source-over draw before it touches the image, so alpha 0 would upload nothing. */
        private const val WARM_UP_ALPHA = 1f / 255f
        private const val INITIAL_RETRY_DELAY_MS = 500L
        private const val MAXIMUM_RETRY_DELAY_MS = 8_000L

        /** How many times an atlas tries a sprite before it is packed without it, which then joins once it loads. */
        private const val ATLAS_ATTEMPTS = 3
    }
}
