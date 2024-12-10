package com.pandulapeter.kubriko.audioPlayback.implementation

import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager

internal class AudioPlaybackManagerImpl : AudioPlaybackManager() {

    override fun playSound(uri: String) = println(uri)
}