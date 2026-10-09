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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.sprites.helpers.toSpriteResource
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
) : SpriteManager(isLoggingEnabled, instanceNameForLogging) {

    private val cache = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap?>())
    private val pendingWarmingUp = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap>())

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
        if (pending.isNotEmpty()) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    pending.values.forEach { bitmap ->
                        drawImage(
                            image = bitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(bitmap.width, bitmap.height),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(1, 1),
                            alpha = WARM_UP_ALPHA,
                        )
                    }
                }
            )
            LaunchedEffect(pending.keys) {
                pending.keys.forEach { promoteToCache(it) }
            }
        }
    }

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
            var retryDelayInMilliseconds = initialRetryDelayInMilliseconds
            var isFirstAttempt = true
            while (true) {
                val loadedBitmap = loadImage(resource, isFirstAttempt)
                // A load that finishes after unload() is dropped, so the sprite does not become resident again.
                if (resource !in cache.value) return@launch
                if (loadedBitmap != null) {
                    var isPublished = false
                    pendingWarmingUp.update {
                        isPublished = resource in cache.value
                        if (isPublished) it.putting(resource, loadedBitmap) else it
                    }
                    if (isPublished) {
                        launch {
                            delay(WARM_UP_TIMEOUT_MS.milliseconds)
                            promoteToCache(resource)
                        }
                    }
                    return@launch
                }
                isFirstAttempt = false
                delay(retryDelayInMilliseconds.milliseconds)
                if (resource !in cache.value) return@launch
                retryDelayInMilliseconds = (retryDelayInMilliseconds * 2).coerceAtMost(MAXIMUM_RETRY_DELAY_MS)
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
    }
}
