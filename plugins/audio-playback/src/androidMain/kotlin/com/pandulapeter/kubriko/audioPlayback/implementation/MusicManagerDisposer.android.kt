package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.collections.immutable.PersistentMap

internal actual fun MusicPlayer.onManagerDisposed(cache: PersistentMap<String, Any?>) {
    cache.values.filterNotNull().forEach { music -> dispose(music) }
    dispose()
}