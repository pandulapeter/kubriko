package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.collections.immutable.PersistentMap

internal expect fun MusicPlayer.onManagerDisposed(cache: PersistentMap<String, Any?>)