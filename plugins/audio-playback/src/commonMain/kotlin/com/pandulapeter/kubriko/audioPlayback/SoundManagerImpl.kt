package com.pandulapeter.kubriko.audioPlayback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.audioPlayback.implementation.SoundPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createSoundPlayer
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class SoundManagerImpl(
    private val maximumSimultaneousStreamsOfTheSameSound: Int,
) : SoundManager() {
    private val cache = MutableStateFlow(persistentMapOf<String, Any?>())
    private var soundPlayer: SoundPlayer? = null

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        if (soundPlayer == null) {
            soundPlayer = createSoundPlayer(maximumSimultaneousStreamsOfTheSameSound).also { soundPlayer ->
                scope.launch {
                    cache.value.keys.forEach { uri ->
                        soundPlayer.preload(uri)?.let { sound -> addToCache(uri, sound) }
                    }
                }
            }
        }
    }

    override fun getLoadingProgress(uris: Collection<String>) = if (uris.isEmpty()) flowOf(1f) else cache.map { cache ->
        cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
    }

    override fun preload(vararg uris: String) = preload(uris.toSet())

    override fun preload(uris: Collection<String>) {
        scope.launch {
            uris.forEach { uri ->
                if (!cache.value.contains(uri)) {
                    addToCache(uri, null)
                    soundPlayer?.preload(uri)?.let { sound -> addToCache(uri, sound) }
                }
            }
        }
    }

    private fun addToCache(key: String, sound: Any?) {
        cache.value = cache.value.toMutableMap().apply {
            put(key, sound)
        }.toPersistentMap()
    }

    override fun play(uri: String) {
        scope.launch {
            soundPlayer?.let { soundPlayer ->
                val cachedSound = cache.value[uri]
                if (cachedSound == null) {
                    soundPlayer.preload(uri)?.let { sound ->
                        addToCache(uri, sound)
                        soundPlayer.play(sound)
                    }
                } else {
                    soundPlayer.play(cachedSound)
                }
            }
        }
    }

    override fun unload(uri: String) {
        scope.launch {
            cache.value[uri]?.let { sound -> soundPlayer?.dispose(sound) }
            cache.value = cache.value.remove(uri)
        }
    }

    override fun onDispose() {
        soundPlayer?.let { soundPlayer ->
            cache.value.values.filterNotNull().forEach { sound -> soundPlayer.dispose(sound) }
            soundPlayer.dispose()
        }
        cache.value = persistentMapOf()
        soundPlayer = null
    }
}