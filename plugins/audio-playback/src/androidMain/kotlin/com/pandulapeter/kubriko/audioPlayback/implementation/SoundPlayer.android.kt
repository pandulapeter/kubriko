package com.pandulapeter.kubriko.audioPlayback.implementation

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {
    private val context = LocalContext.current.applicationContext
    private var preloadListeners = mutableListOf<PreloadListener>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(maximumSimultaneousStreamsOfTheSameSound)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .apply {
            setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    preloadListeners.forEach { it.onSampleLoaded(sampleId) }
                }
            }
        }

    private fun Context.getFileDescriptor(uri: String) = assets.openFd(uri.removePrefix("file:///android_asset/"))

    override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val preloadListener = object : PreloadListener {
                override fun onSampleLoaded(sampleId: Int) {
                    preloadListeners.remove(this)
                    continuation.resume(sampleId)
                }
            }
            preloadListeners.add(preloadListener)
            soundPool.load(context.getFileDescriptor(uri), 1)
        }
    }

    override suspend fun play(sound: Any) {
        withContext(Dispatchers.Default) {
            soundPool.play(sound as Int, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as Int
        soundPool.stop(cachedSound)
        soundPool.unload(cachedSound)
    }

    override fun dispose() = soundPool.release()
}

private interface PreloadListener {
    fun onSampleLoaded(sampleId: Int)
}