package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kubriko.examples.game_space_squadron.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal class SpaceSquadronAudioManager(
    private val stateManager: StateManager,
    private val audioPlaybackManager: AudioPlaybackManager,
    private val userPreferencesManager: SpaceSquadronUserPreferencesManager
) : Manager() {
    private var hasStartedMusic = false
    private val soundUrisToPlay = mutableSetOf<String>()

    override fun onInitialize(kubriko: Kubriko) {
        combine(
            stateManager.isFocused,
            userPreferencesManager.isMusicEnabled,
        ) { isFocused, isMusicEnabled ->
            isFocused to isMusicEnabled
        }.onEach { (isFocused, isMusicEnabled) ->
            if (isMusicEnabled) {
                if (isFocused) {
                    if (hasStartedMusic) {
                        audioPlaybackManager.resumeMusic()
                    } else {
                        audioPlaybackManager.playMusic(
                            uri = Res.getUri("files/music/music.mp3"),
                            shouldLoop = true,
                        )
                        hasStartedMusic = true
                    }
                }
            } else {
                audioPlaybackManager.pauseMusic()
            }
        }.launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        soundUrisToPlay.forEach { audioPlaybackManager.playSound(Res.getUri(it)) }
        soundUrisToPlay.clear()
    }

    private fun playSoundEffect(uri: String) {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            soundUrisToPlay.add(uri)
        }
    }
}