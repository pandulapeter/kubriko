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
import com.pandulapeter.kubriko.sprites.helpers.toSpriteResource
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import kotlinx.collections.immutable.persistentMapOf
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
) : SpriteManager(isLoggingEnabled, instanceNameForLogging) {

    private val cache = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap?>())
    private val pendingWarmingUp = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap>())

    override fun getLoadingProgress(drawableResources: Collection<DrawableResource>) = if (drawableResources.isEmpty()) flowOf(1f) else
        getSpriteLoadingProgress(drawableResources.map { it.toSpriteResource() })

    override fun preload(vararg drawableResources: DrawableResource) = preload(drawableResources.toList())

    override fun preload(drawableResources: Collection<DrawableResource>) {
        drawableResources.forEach { get(it) }
    }

    override fun get(drawableResource: DrawableResource): ImageBitmap? =
        get(drawableResource.toSpriteResource())

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
                            alpha = 0f,
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
            val bitmap = loadImage(resource)
            if (bitmap != null) {
                pendingWarmingUp.update { it.putting(resource, bitmap) }
                launch {
                    delay(WARM_UP_TIMEOUT_MS.milliseconds)
                    promoteToCache(resource)
                }
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

    @OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
    internal open suspend fun loadImage(spriteResource: SpriteResource): ImageBitmap? = try {
        getDrawableResourceBytes(
            getSystemResourceEnvironment(),
            spriteResource.drawableResource
        ).toImageBitmap(
            DensityQualifier.MDPI.dpi,
            DensityQualifier.MDPI.dpi,
            spriteResource.rotation
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    companion object {
        private const val WARM_UP_TIMEOUT_MS = 100L
    }
}
