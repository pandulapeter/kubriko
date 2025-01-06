package com.pandulapeter.kubriko.audioPlayback.implementation

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {
    private val context = LocalContext.current.applicationContext

    private fun Context.getFileDescriptor(uri: String) = assets.openFd(uri.removePrefix("file:///android_asset/"))

    override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            MediaPlayer().apply {
                val fileDescriptor = context.getFileDescriptor(uri)
                setDataSource(
                    fileDescriptor.fileDescriptor,
                    fileDescriptor.startOffset,
                    fileDescriptor.length
                )
                fileDescriptor.close()
                setOnPreparedListener {
                    continuation.resume(this)
                }
                prepare()
            }
        }
    }

    override suspend fun play(music: Any, shouldLoop: Boolean) = withContext(Dispatchers.Default) {
        (music as MediaPlayer).run {
            if (shouldLoop) {
                isLooping = true
                setOnCompletionListener(null)
            } else {
                setOnCompletionListener { stop() }
            }
            if (!isPlaying) {
                start()
            }
        }
    }

    override fun isPlaying(music: Any) = (music as MediaPlayer).isPlaying

    override fun pause(music: Any) = (music as MediaPlayer).pause()

    override suspend fun stop(music: Any) = (music as MediaPlayer).stop()

    override suspend fun dispose(music: Any) {
        stop(music)
        (music as MediaPlayer).release()
    }

    override suspend fun dispose() = Unit
}