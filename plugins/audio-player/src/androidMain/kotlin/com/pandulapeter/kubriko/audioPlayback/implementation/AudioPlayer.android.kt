package com.pandulapeter.kubriko.audioPlayback.implementation

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {
            private val soundPool = SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            private val soundsIds = mutableMapOf<String, Int>()

            private fun preloadSound(uri: String) {
                coroutineScope.launch(Dispatchers.IO) {
                    if (soundsIds[uri] == null) {
                        soundsIds[uri] = soundPool.load(context.assets.openFd(uri.removePrefix("file:///android_asset/")), 1)
                    }
                }
            }

            override fun preloadSounds(uris: List<String>) = uris.forEach(::preloadSound)

            override fun playSound(uri: String) {
                soundsIds[uri].let { soundId ->
                    if (soundId == null) {
                        soundPool.setOnLoadCompleteListener { _, _, _ ->
                            soundPool.setOnLoadCompleteListener(null)
                            playSound(uri)
                        }
                        preloadSound(uri)
                    } else {
                        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                    }
                }
            }

            override fun unloadSound(uri: String) {
                soundsIds[uri]?.unload()
                soundsIds.remove(uri)
            }

            override fun dispose() {
                soundsIds.values.forEach { it.unload() }
                soundsIds.clear()
                soundPool.release()
            }

            private fun Int.unload() {
                soundPool.stop(this)
                soundPool.unload(this)
            }
        }
    }
}