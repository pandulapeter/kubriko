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
import com.pandulapeter.kubriko.sprites.helpers.toSpriteResource
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import kotlinx.collections.immutable.persistentMapOf
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

internal class SpriteManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : SpriteManager(isLoggingEnabled, instanceNameForLogging) {

    private val cache = MutableStateFlow(persistentMapOf<SpriteResource, ImageBitmap?>())

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

    override fun preloadSprites(vararg resources: SpriteResource) = preloadSprites(resources.toList())

    override fun preloadSprites(resources: Collection<SpriteResource>) = resources.forEach { get(it) }

    override fun get(resource: SpriteResource): ImageBitmap? {
        val currentCache = cache.value
        if (!currentCache.containsKey(resource)) {
            cache.update { it.put(resource, null) }
            scope.launch {
                val bitmap = loadImage(resource)
                cache.update { latestCache ->
                    if (latestCache.containsKey(resource)) {
                        latestCache.put(resource, bitmap)
                    } else {
                        latestCache
                    }
                }
            }
        }
        return cache.value[resource]
    }

    override fun unload(resource: SpriteResource) = cache.update { it.remove(resource) }

    @OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
    private suspend fun loadImage(spriteResource: SpriteResource): ImageBitmap? = try {
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
}