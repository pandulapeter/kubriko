package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.Invisible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class SpaceSquadronUIManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique {

    private val gameManager by manager<SpaceSquadronGameManager>()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
    }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value) PointerIcon.Invisible else PointerIcon.Default
    )

    override fun onKeyReleased(key: Key) {
        when (key) {
            Key.Escape -> if (stateManager.isRunning.value) {
                gameManager.pauseGame()
            } else {
                gameManager.playGame()
            }

            Key.Spacebar, Key.Enter -> {
                if (!stateManager.isRunning.value) {
                    gameManager.playGame()
                }
            }

            else -> Unit
        }
    }
}