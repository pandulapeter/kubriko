package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLAudioElement

@Suppress("UNCHECKED_CAST")
@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        buildList {
            repeat(maximumSimultaneousStreamsOfTheSameSound) {
                add(
                    (document.createElement("audio") as HTMLAudioElement).apply {
                        src = uri
                    }
                )
            }
        }
    }

    override suspend fun play(sound: Any) {
        sound as List<HTMLAudioElement>
        withContext(Dispatchers.Default) {
            sound.firstOrNull { it.paused }?.play()
        }
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as List<HTMLAudioElement>
        cachedSound.forEach {
            if (!it.paused) {
                it.pause()
            }
            it.src = ""
            it.remove()
        }
    }

    override fun dispose() = Unit
}