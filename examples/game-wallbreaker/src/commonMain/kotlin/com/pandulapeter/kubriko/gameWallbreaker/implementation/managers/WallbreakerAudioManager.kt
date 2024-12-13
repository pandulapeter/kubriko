package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kubriko.examples.game_wallbreaker.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal class WallbreakerAudioManager(
    private val stateManager: StateManager,
    private val audioPlaybackManager: AudioPlaybackManager,
    private val wallbreakerUserPreferencesManager: WallbreakerUserPreferencesManager
) : Manager() {
    private var hasStartedMusic = false

    override fun onInitialize(kubriko: Kubriko) {
        audioPlaybackManager.preloadSound(
            Res.getUri(PATH_SOUND_EFFECT_EDGE_BOUNCE),
            Res.getUri(PATH_SOUND_EFFECT_BRICK_POP),
        )
        combine(
            stateManager.isFocused,
            wallbreakerUserPreferencesManager.isMusicEnabled,
        ) { isFocused, isMusicEnabled ->
            isFocused to isMusicEnabled
        }.onEach { (isFocused, isMusicEnabled) ->
            if (isMusicEnabled) {
                if (isFocused) {
                    if (hasStartedMusic) {
                        audioPlaybackManager.resumeMusic()
                    } else {
                        audioPlaybackManager.playMusic(
                            uri = Res.getUri(PATH_MUSIC),
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

    fun playBrickPopSoundEffect() = playSoundEffect(PATH_SOUND_EFFECT_BRICK_POP)

    fun playEdgeBounceSoundEffect() = playSoundEffect(PATH_SOUND_EFFECT_EDGE_BOUNCE)

    fun playGameOverSoundEffect() = playSoundEffect(PATH_SOUND_EFFECT_GAME_OVER)

    fun playLevelClearedSoundEffect() = playSoundEffect(PATH_SOUND_EFFECT_LEVEL_CLEARED)

    fun playPaddleHitSoundEffect() = playSoundEffect(PATH_SOUND_EFFECT_PADDLE_HIT)

    private fun playSoundEffect(uri: String) {
        if (wallbreakerUserPreferencesManager.areSoundEffectsEnabled.value) {
            audioPlaybackManager.playSound(Res.getUri(uri))
        }
    }

    companion object {
        private const val PATH_SOUND_EFFECT_BRICK_POP = "files/sounds/brick_pop.wav"
        private const val PATH_SOUND_EFFECT_EDGE_BOUNCE = "files/sounds/edge_bounce.wav"
        private const val PATH_SOUND_EFFECT_GAME_OVER = "files/sounds/game_over.wav"
        private const val PATH_SOUND_EFFECT_LEVEL_CLEARED = "files/sounds/level_cleared.wav"
        private const val PATH_SOUND_EFFECT_PADDLE_HIT = "files/sounds/paddle_hit.wav"
        private const val PATH_MUSIC = "files/music/music.mp3"
    }
}