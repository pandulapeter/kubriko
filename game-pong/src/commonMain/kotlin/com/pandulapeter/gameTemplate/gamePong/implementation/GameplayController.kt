package com.pandulapeter.gameTemplate.gamePong.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.gamePong.implementation.models.Metadata
import game.game_pong.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class GameplayController(
    private val engine: Engine,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val metadata = combine(
        engine.metadataManager.fps,
        engine.metadataManager.totalGameObjectCount,
        engine.metadataManager.visibleGameObjectCount,
        engine.metadataManager.runtimeInMilliseconds,
    ) { fps, totalGameObjectCount, visibleGameObjectCount, runtimeInMilliseconds ->
        Metadata(
            fps = fps,
            totalGameObjectCount = totalGameObjectCount,
            visibleGameObjectCount = visibleGameObjectCount,
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }.stateIn(this, SharingStarted.Eagerly, Metadata())

    init {
        engine.stateManager.isFocused
            .filterNot { it }
            .onEach { engine.stateManager.updateIsRunning(false) }
            .launchIn(this)
        engine.inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        loadMap(MAP_NAME)
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap(mapName: String) = launch {
        try {
            engine.instanceManager.deserializeState(Res.readBytes("files/maps/$mapName.json").decodeToString())
        } catch (_: MissingResourceException) {
        }
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> engine.stateManager.updateIsRunning(!engine.stateManager.isRunning.value)
        }
    }

    companion object {
        const val MAP_NAME = "map_demo"
    }
}