package com.pandulapeter.kubriko.sprites.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.sprites.SpriteManager
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

internal class SpriteManagerImpl : SpriteManager() {

    private val cache = mutableStateOf(persistentMapOf<DrawableResource, ImageBitmap?>())

    override fun loadSprite(drawableResource: DrawableResource): ImageBitmap? {
        if (!cache.value.containsKey(drawableResource)) {
            cache.value = cache.value.put(drawableResource, null)
        }
        return cache.value[drawableResource]
    }

    override fun unloadSprite(drawableResource: DrawableResource) {
        cache.value = cache.value.remove(drawableResource)
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        val newCache = mutableMapOf<DrawableResource, ImageBitmap?>()
        cache.value.let { cache ->
            cache.keys.forEach { key ->
                newCache[key] = cache[key] ?: imageResource(key)
            }
        }
        cache.value = newCache.toPersistentMap()
    }
}