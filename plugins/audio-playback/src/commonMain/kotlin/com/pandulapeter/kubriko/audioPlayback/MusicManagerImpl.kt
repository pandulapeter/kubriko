package com.pandulapeter.kubriko.audioPlayback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.audioPlayback.implementation.MusicPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createMusicPlayer
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class MusicManagerImpl : MusicManager() {
    private val cache = MutableStateFlow(persistentMapOf<String, Any?>())
    private var musicPlayer: MusicPlayer? = null
    private val stateManager by manager<StateManager>()

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        if (musicPlayer == null && isInitialized.value) {
            musicPlayer = createMusicPlayer(scope).also { soundPlayer ->
                scope.launch {
                    cache.value.keys.forEach { uri ->
                        soundPlayer.preload(uri)?.let { music -> addToCache(uri, music) }
                    }
                }
            }
            stateManager.isFocused
                .onEach { isFocused ->
                    if (!isFocused) {
                        cache.value.keys.forEach(::pause)
                    }
                }
                .launchIn(scope)
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
                    musicPlayer?.preload(uri)?.let { music -> addToCache(uri, music) }
                }
            }
        }
    }

    override fun isPlaying(uri: String) = cache.value[uri].let { music ->
        music != null && musicPlayer?.isPlaying(music) == true
    }

    private fun addToCache(uri: String, music: Any?) {
        cache.value = cache.value.toMutableMap().apply {
            put(uri, music)
        }.toPersistentMap()
    }

    override fun play(uri: String, shouldLoop: Boolean) {
        scope.launch {
            musicPlayer?.let { musicPlayer ->
                val cachedSound = cache.value[uri]
                if (cachedSound == null) {
                    musicPlayer.preload(uri)?.let { music ->
                        addToCache(uri, music)
                        musicPlayer.play(music, shouldLoop)
                    }
                } else {
                    musicPlayer.play(cachedSound, shouldLoop)
                }
            }
        }
    }

    override fun pause(uri: String) {
        cache.value[uri]?.let { music -> musicPlayer?.pause(music) }
    }

    override fun stop(uri: String) {
        scope.launch {
            cache.value[uri]?.let { music -> musicPlayer?.stop(music) }
        }
    }

    override fun unload(uri: String) {
        scope.launch {
            cache.value[uri]?.let { music -> musicPlayer?.dispose(music) }
            cache.value = cache.value.remove(uri)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDispose() {
        // We cannot use the Manager scope to do the cleanup, as it will get cancelled shortly after this function is called
        // However, the cleanup needs to be done on a background thread (it blocks the desktop UI otherwise)
        GlobalScope.launch(Dispatchers.Default) {
            musicPlayer?.let { musicPlayer ->
                cache.value.values.filterNotNull().forEach { music -> musicPlayer.dispose(music) }
                musicPlayer.dispose()
            }
            cache.value = persistentMapOf()
            musicPlayer = null
        }
    }
}