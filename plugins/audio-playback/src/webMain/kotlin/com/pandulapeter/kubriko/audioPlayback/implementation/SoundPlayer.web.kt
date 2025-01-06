package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLAudioElement

@Suppress("UNCHECKED_CAST")
@Composable
internal actual fun createSoundPlayer() = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        buildList {
            repeat(SIMULTANEOUSLY_PLAYED_INSTANCE_LIMIT) {
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

    override suspend fun dispose(sound: Any) {
        sound as List<HTMLAudioElement>
        sound.forEach {
            if (!it.paused) {
                it.pause()
            }
            it.src = ""
            it.remove()
        }
    }

    override suspend fun dispose() = Unit
}

private const val SIMULTANEOUSLY_PLAYED_INSTANCE_LIMIT = 5