package com.pandulapeter.kubriko.audioPlayback.implementation

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private val context = LocalContext.current
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private var mediaPlayer: MediaPlayer? = null
    private var shouldPlayMusic = false

    private fun Context.getFileDescriptor(uri: String) = assets.openFd(uri.removePrefix("file:///android_asset/"))

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, _ ->
            coroutineScope.launch(Dispatchers.Default) {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        shouldPlayMusic = true
        coroutineScope.launch(Dispatchers.IO) {
            mediaPlayer = MediaPlayer().apply {
                val fileDescriptor = context.getFileDescriptor(uri)
                setDataSource(
                    fileDescriptor.fileDescriptor,
                    fileDescriptor.startOffset,
                    fileDescriptor.length
                )
                fileDescriptor.close()
                prepare()
                if (shouldLoop) {
                    isLooping = true
                } else {
                    setOnCompletionListener { stopMusic() }
                }
            }
            if (shouldPlayMusic) {
                resumeMusic()
            }
        }
    }

    override fun resumeMusic() {
        shouldPlayMusic = true
        mediaPlayer?.start()
    }

    override fun pauseMusic() {
        shouldPlayMusic = false
        mediaPlayer?.pause()
    }

    override fun stopMusic() {
        shouldPlayMusic = false
        mediaPlayer?.run {
            stop()
            release()
            mediaPlayer = null
        }
    }

    override fun playSound(uri: String) {
        coroutineScope.launch(Dispatchers.IO) {
            soundPool.load(context.getFileDescriptor(uri), 1)
        }
    }

    override fun dispose() {
        stopMusic()
        soundPool.release()
    }
}