package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
internal actual fun MusicPlayer.onManagerDisposed(cache: PersistentMap<String, Any?>) {
    GlobalScope.launch(Dispatchers.Default) {
        cache.values.filterNotNull().forEach { music -> dispose(music) }
        dispose()
    }
}