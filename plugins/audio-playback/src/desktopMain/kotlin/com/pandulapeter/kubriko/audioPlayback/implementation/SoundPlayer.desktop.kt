package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem

@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int, // TODO: On Desktop, due to the issue below, this limit is not applied
) = object : SoundPlayer {

    // Couldn't figure out how to re-use cached Clips, so preloading on desktop is not supported
    override suspend fun preload(uri: String) = uri

    override suspend fun play(sound: Any) = withContext(Dispatchers.IO) {
        val uri = sound as String
        val clip = AudioSystem.getClip()
        val inputStream = URI(uri).let { resolvedUri ->
            if (resolvedUri.isAbsolute) {
                resolvedUri.toURL().openStream()
            } else {
                FileInputStream(resolvedUri.toString())
            }
        }
        clip.open(AudioSystem.getAudioInputStream(BufferedInputStream(inputStream)))
        clip.start()
    }

    // TODO: Stop the Clip
    override suspend fun dispose(sound: Any) = Unit

    override suspend fun dispose() = Unit
}