package com.pandulapeter.kubriko.audioPlayback.implementation

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.pandulapeter.kubriko.ActivityHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
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
    private var mediaPlayer: MediaPlayer? = null

    private fun preloadSound(uri: String) {
        coroutineScope.launch(Dispatchers.IO) {
            ActivityHolder.currentActivity.value?.applicationContext?.let { context ->
                if (soundsIds[uri] == null) {
                    soundsIds[uri] = soundPool.load(context.getFileDescriptor(uri), 1)
                }
            }
        }
    }

    private fun Context.getFileDescriptor(uri: String) = assets.openFd(uri.removePrefix("file:///android_asset/"))

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            ActivityHolder.currentActivity.value?.applicationContext?.let { context ->
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
                mediaPlayer?.start()
            }
        }
    }

    override fun resumeMusic() {
        mediaPlayer?.start()
    }

    override fun pauseMusic() {
        mediaPlayer?.pause()
    }

    override fun stopMusic() {
        mediaPlayer?.run {
            stop()
            release()
            mediaPlayer = null
        }
    }

    override fun preloadSounds(uris: Collection<String>) = uris.forEach(::preloadSound)

    override fun playSound(uri: String) {
        soundsIds[uri].let { soundId ->
            if (soundId == null) {
                soundPool.setOnLoadCompleteListener { _, _, _ ->
                    soundPool.setOnLoadCompleteListener(null)
                    playSound(uri)
                }
                preloadSound(uri)
            } else {
                coroutineScope.launch(Dispatchers.Default) {
                    soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                }
            }
        }
    }

    override fun unloadSounds(uris: Collection<String>) = uris.forEach { uri ->
        soundsIds[uri]?.unload()
        soundsIds.remove(uri)
    }

    override fun dispose() {
        stopMusic()
        soundsIds.values.forEach { it.unload() }
        soundsIds.clear()
        soundPool.release()
    }

    private fun Int.unload() {
        soundPool.stop(this)
        soundPool.unload(this)
    }
}