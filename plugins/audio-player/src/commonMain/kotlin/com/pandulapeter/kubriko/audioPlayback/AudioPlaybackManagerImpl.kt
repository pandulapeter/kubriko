package com.pandulapeter.kubriko.audioPlayback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.audioPlayback.implementation.AudioPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createAudioPlayer
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class AudioPlaybackManagerImpl : AudioPlaybackManager() {

    private var audioPlayer: AudioPlayer? = null
    private var musicUri: String? = null
    private var shouldLoopMusic = false
    private var shouldPauseMusicAfterInitialization = false
    private val stateManager by manager<StateManager>()

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        if (audioPlayer == null && isInitialized.value) {
            audioPlayer = createAudioPlayer(scope)
            musicUri?.let {
                if (stateManager.isFocused.value && !shouldPauseMusicAfterInitialization) {
                    audioPlayer?.playMusic(it, shouldLoopMusic)
                    musicUri = null
                }
            }
            stateManager.isFocused
                .onEach { isFocused ->
                    if (!isFocused) {
                        pauseMusic()
                    }
                }
                .launchIn(scope)
        }
    }

    override fun onDispose() {
        musicUri = null
        audioPlayer?.dispose()
        audioPlayer = null
    }

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        audioPlayer.let { audioPlayer ->
            if (audioPlayer != null) {
                audioPlayer.playMusic(uri, shouldLoop)
            } else {
                musicUri = uri
                shouldLoopMusic = shouldLoop
            }
        }
    }

    override fun resumeMusic() {
        musicUri.let { musicUri ->
            if (musicUri == null) {
                audioPlayer?.resumeMusic()
            } else {
                playMusic(musicUri, shouldLoopMusic)
            }
        }
    }

    override fun pauseMusic() {
        audioPlayer.let { audioPlayer ->
            if (audioPlayer == null) {
                shouldPauseMusicAfterInitialization = true
            } else {
                audioPlayer.pauseMusic()
            }
        }
    }

    override fun stopMusic() {
        audioPlayer?.stopMusic()
        musicUri = null
    }

    override fun playSound(uri: String) {
        audioPlayer?.playSound(uri)
    }
}