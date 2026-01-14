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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val cache = MutableStateFlow(persistentMapOf<DrawableResource, ImageBitmap?>())

    override fun getLoadingProgress(drawableResources: Collection<DrawableResource>) = if (drawableResources.isEmpty()) flowOf(1f) else cache.map { cache ->
        cache.filter { (key, _) -> key in drawableResources }.count { (_, value) -> value != null }.toFloat() / drawableResources.size
    }

    override fun preload(vararg drawableResources: DrawableResource) = preload(drawableResources.toList())

    override fun preload(drawableResources: Collection<DrawableResource>) {
        drawableResources.forEach { drawableResource -> get(drawableResource) }
    }

    override fun get(drawableResource: DrawableResource): ImageBitmap? {
        if (!cache.value.containsKey(drawableResource)) {
            cache.update { it.put(drawableResource, null) }
        }
        return cache.value[drawableResource]
    }

    override fun unload(drawableResource: DrawableResource) = cache.update { it.remove(drawableResource) }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        cache.collectAsState().value // This line ensures that this Composable is invoked every time the cache is changed
        scope.launch {
            cache.update { cache ->
                val newCache = mutableMapOf<DrawableResource, ImageBitmap?>()
                cache.keys.forEach { key ->
                    newCache[key] = cache[key] ?: loadImage(key)
                }
                newCache.toPersistentMap()
            }
        }
    }

    @OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
    private suspend fun loadImage(drawableResource: DrawableResource): ImageBitmap? = try {
        getDrawableResourceBytes(getSystemResourceEnvironment(), drawableResource).toImageBitmap(DensityQualifier.MDPI.dpi, DensityQualifier.MDPI.dpi)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    override fun onDispose() = cache.update { persistentMapOf() }
}