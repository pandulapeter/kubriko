package com.pandulapeter.kubrikoPong.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.engine.Kubriko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kubriko.game_pong.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class GameplayController(
    private val kubriko: Kubriko,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        kubriko.stateManager.isFocused
            .filterNot { it }
            .onEach { kubriko.stateManager.updateIsRunning(false) }
            .launchIn(this)
        kubriko.inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        loadMap(MAP_NAME)
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap(mapName: String) = launch {
        try {
            kubriko.actorManager.deserializeState(Res.readBytes("files/maps/$mapName.json").decodeToString())
        } catch (_: MissingResourceException) {
        }
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> kubriko.stateManager.updateIsRunning(!kubriko.stateManager.isRunning.value)
        }
    }

    companion object {
        const val MAP_NAME = "map_demo"
    }
}