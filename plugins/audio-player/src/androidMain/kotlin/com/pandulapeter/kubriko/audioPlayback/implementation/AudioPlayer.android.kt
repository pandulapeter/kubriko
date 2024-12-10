package com.pandulapeter.kubriko.audioPlayback.implementation

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

            override fun playSound(uri: String) {
                scope.launch(Dispatchers.IO) {
                    val soundId = soundPool.load(context.assets.openFd(uri.removePrefix("file:///android_asset/")), 1)
                    soundPool.setOnLoadCompleteListener { _, _, _ ->
                        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                        soundPool.unload(soundId)
                    }
                }
            }

            override fun dispose() = soundPool.release()
        }
    }
}