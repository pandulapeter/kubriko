package com.pandulapeter.kubriko.sprites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

internal class SpriteManagerImpl : SpriteManager() {

    private val cache = MutableStateFlow(persistentMapOf<DrawableResource, ImageBitmap?>())

    override fun getLoadingProgress(drawableResources: Collection<DrawableResource>) = cache.map { cache ->
        cache.filter { (key, _) -> key in drawableResources }.count { (_, value) -> value != null }.toFloat() / drawableResources.size
    }

    override fun load(drawableResource: DrawableResource): ImageBitmap? {
        if (!cache.value.containsKey(drawableResource)) {
            cache.value = cache.value.put(drawableResource, null)
        }
        return cache.value[drawableResource]
    }

    override fun unload(drawableResource: DrawableResource) {
        cache.value = cache.value.remove(drawableResource)
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        cache.collectAsState().value // This line ensures that this Composable is invoked every time the cache is changed
        scope.launch {
            val newCache = mutableMapOf<DrawableResource, ImageBitmap?>()
            cache.value.let { cache ->
                cache.keys.forEach { key ->
                    newCache[key] = cache[key] ?: loadImage(key)
                }
            }
            cache.value = newCache.toPersistentMap()
        }
    }

    @OptIn(InternalResourceApi::class, ExperimentalResourceApi::class)
    private suspend fun loadImage(drawableResource: DrawableResource): ImageBitmap? = try {
        getDrawableResourceBytes(getSystemResourceEnvironment(), drawableResource).toImageBitmap(DensityQualifier.MDPI.dpi, DensityQualifier.MDPI.dpi)
    } catch (e: Exception) {
        null
    }

    override fun onDispose() {
        cache.value = persistentMapOf()
    }
}