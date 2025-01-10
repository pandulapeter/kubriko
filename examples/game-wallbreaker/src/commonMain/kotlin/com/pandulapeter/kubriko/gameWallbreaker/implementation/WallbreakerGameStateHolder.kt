package com.pandulapeter.kubriko.gameWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.BackgroundAnimationManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

sealed interface WallbreakerGameStateHolder : ExampleStateHolder

internal class WallbreakerGameStateHolderImpl : WallbreakerGameStateHolder {
    private val sharedMusicManager = MusicManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedSoundManager = SoundManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val backgroundShaderManager = ShaderManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val backgroundAnimationManager = BackgroundAnimationManager()
    val backgroundLoadingManager = LoadingManager()
    val backgroundKubriko = Kubriko.newInstance(
        sharedMusicManager,
        sharedSoundManager,
        backgroundShaderManager,
        backgroundLoadingManager,
        backgroundAnimationManager,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    val stateManager = StateManager.newInstance(
        shouldAutoStart = false,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val viewportManager = ViewportManager.newInstance(
        aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
            ratio = 1f,
            width = 1200.sceneUnit,
        ),
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val persistenceManager = PersistenceManager.newInstance(
        fileName = "kubrikoWallbreaker",
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val scoreManager = ScoreManager(persistenceManager)
    val shaderManager = ShaderManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val userPreferencesManager = UserPreferencesManager(persistenceManager)
    val gameplayManager = GameplayManager(stateManager)
    val audioManager = AudioManager(stateManager, userPreferencesManager)
    val uiManager = UIManager(stateManager)
    private val collisionManager = CollisionManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val keyboardInputManager = KeyboardInputManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val pointerInputManager = PointerInputManager.newInstance(
        isActiveAboveViewport = true,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val kubriko = Kubriko.newInstance(
        sharedMusicManager,
        sharedSoundManager,
        stateManager,
        viewportManager,
        collisionManager,
        shaderManager,
        keyboardInputManager,
        pointerInputManager,
        persistenceManager,
        scoreManager,
        userPreferencesManager,
        audioManager,
        gameplayManager,
        uiManager,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )

    override fun dispose() {
        backgroundKubriko.dispose()
        kubriko.dispose()
    }
}

private const val LOG_TAG = "WB"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"