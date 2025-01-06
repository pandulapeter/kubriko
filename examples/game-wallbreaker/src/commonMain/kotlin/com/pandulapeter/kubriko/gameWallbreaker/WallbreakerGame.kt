package com.pandulapeter.kubriko.gameWallbreaker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerAudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerBackgroundManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerGameManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerScoreManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerUIManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerUserPreferencesManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.WallbreakerGameOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.WallbreakerPauseMenuOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.WallbreakerTheme
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.isFontLoaded
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import org.jetbrains.compose.resources.imageResource

/**
 * Music: https://opengameart.org/content/cyberpunk-moonlight-sonata
 */
@Composable
fun WallbreakerGame(
    stateHolder: WallbreakerGameStateHolder = createWallbreakerGameStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = WallbreakerTheme {
    stateHolder as WallbreakerGameStateHolderImpl
    val isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value
    val isGameLoaded = isFontLoaded() && imageResource(Res.drawable.img_logo).width > 1
    KubrikoViewport(
        modifier = Modifier
            .fillMaxSize()
            .background(if (stateHolder.shaderManager.areShadersSupported) Color.Black else Color.DarkGray),
        kubriko = stateHolder.backgroundKubriko,
    )
    AnimatedVisibility(
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        KubrikoViewport(
            modifier = Modifier.windowInsetsPadding(windowInsets).background(Color.Black),
            kubriko = stateHolder.kubriko,
            windowInsets = windowInsets,
        )
        WallbreakerPauseMenuOverlay(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
            isGameRunning = isGameRunning,
            shouldShowResumeButton = !stateHolder.gameManager.isGameOver.collectAsState().value,
            onResumeButtonPressed = stateHolder.gameManager::resumeGame,
            onRestartButtonPressed = stateHolder.gameManager::restartGame,
            areSoundEffectsEnabled = stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
            onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
            isMusicEnabled = stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
            onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
        )
        WallbreakerGameOverlay(
            gameAreaModifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
            isGameRunning = isGameRunning,
            score = stateHolder.scoreManager.score.collectAsState().value,
            highScore = stateHolder.scoreManager.highScore.collectAsState().value,
            onPauseButtonPressed = stateHolder.gameManager::pauseGame,
        )
    }
}


sealed interface WallbreakerGameStateHolder : ExampleStateHolder

fun createWallbreakerGameStateHolder(): WallbreakerGameStateHolder = WallbreakerGameStateHolderImpl()

private class WallbreakerGameStateHolderImpl : WallbreakerGameStateHolder {
    private val audioPlaybackManager = AudioPlaybackManager.newInstance()
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoWallbreaker")
    val scoreManager = WallbreakerScoreManager(persistenceManager)
    val shaderManager = ShaderManager.newInstance()
    val userPreferencesManager = WallbreakerUserPreferencesManager(persistenceManager)
    val gameManager = WallbreakerGameManager(stateManager)
    val backgroundKubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        WallbreakerBackgroundManager(),
    )
    val kubriko = Kubriko.newInstance(
        stateManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
                ratio = 1f,
                width = 1200.sceneUnit,
            )
        ),
        CollisionManager.newInstance(),
        shaderManager,
        KeyboardInputManager.newInstance(),
        PointerInputManager.newInstance(isActiveAboveViewport = true),
        persistenceManager,
        scoreManager,
        userPreferencesManager,
        audioPlaybackManager,
        WallbreakerAudioManager(stateManager, audioPlaybackManager, userPreferencesManager),
        gameManager,
        WallbreakerUIManager(stateManager),
    )

    override fun dispose() = kubriko.dispose()
}