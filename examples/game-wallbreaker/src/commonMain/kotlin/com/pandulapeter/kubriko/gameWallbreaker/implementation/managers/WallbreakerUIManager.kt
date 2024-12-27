package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.Invisible
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.WallbreakerPauseMenuBackground
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class WallbreakerUIManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique {

    private val gameManager by manager<WallbreakerGameManager>()

    @Composable
    override fun getModifier(layerIndex: Int?) = Modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value) PointerIcon.Invisible else PointerIcon.Default
    )

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
    }

    override fun onKeyReleased(key: Key) {
        when (key) {
            Key.Escape -> {
                stateManager.updateIsRunning(!stateManager.isRunning.value)
                if (gameManager.isGameOver.value) {
                    gameManager.restartGame()
                }
            }

            Key.Spacebar, Key.Enter -> {
                if (!stateManager.isRunning.value) {
                    if (gameManager.isGameOver.value) {
                        gameManager.restartGame()
                    } else {
                        gameManager.resumeGame()
                    }
                }
            }

            else -> Unit
        }
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) = WallbreakerPauseMenuBackground(
        isVisible = !stateManager.isRunning.collectAsState().value,
    )
}