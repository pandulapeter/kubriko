package com.pandulapeter.kubriko.sprites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.sprites.implementation.processUri
import com.pandulapeter.kubriko.sprites.implementation.toImageBitmap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

internal class SpriteManagerImpl : SpriteManager() {

    private val cache = mutableStateOf(persistentMapOf<String, ImageBitmap?>())

    override fun loadSprite(uri: String): ImageBitmap? {
        val actualUri = uri.processUri()
        if (!cache.value.containsKey(actualUri)) {
            cache.value = cache.value.put(actualUri, null)
        }
        return cache.value[actualUri]
    }

    override fun unloadSprite(uri: String) {
        cache.value = cache.value.remove(uri.processUri())
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        cache.value
        scope.launch {
            val newCache = mutableMapOf<String, ImageBitmap?>()
            cache.value.let { cache ->
                cache.keys.forEach { key ->
                    newCache[key] = cache[key] ?: loadImage(key)
                }
            }
            cache.value = newCache.toPersistentMap()
        }
    }

    @OptIn(InternalResourceApi::class)
    private suspend fun loadImage(uri: String): ImageBitmap? = try {
        readResourceBytes(uri).toImageBitmap(DensityQualifier.MDPI.dpi, DensityQualifier.MDPI.dpi)
    } catch (e: Exception) {
        println("EXCEPTION: ${e.message}")
        null
    }

    override fun onDispose() {
        cache.value = persistentMapOf()
    }
}