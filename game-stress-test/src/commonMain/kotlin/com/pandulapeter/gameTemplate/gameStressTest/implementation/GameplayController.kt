package com.pandulapeter.gameTemplate.gameStressTest.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardZoomState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.zoomState
import com.pandulapeter.gameTemplate.gameStressTest.implementation.models.Metadata
import game.game_stress_test.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal object GameplayController : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val engine = Engine.newInstance()
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
        engine.inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        engine.inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        start()
    }

    private fun start() {
        launch {
            engine.instanceManager.register(
                entries = GameObjectRegistry.entries,
            )
            loadMap(MAP_NAME)
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadMap(mapName: String) {
        try {
            engine.instanceManager.deserializeState(Res.readBytes("files/maps/$mapName.json").decodeToString())
        } catch (_: MissingResourceException) {
        }
    }

    private fun handleKeys(keys: Set<Key>) {
        if (engine.stateManager.isRunning.value) {
            engine.viewportManager.multiplyScaleFactor(
                when (keys.zoomState) {
                    KeyboardZoomState.NONE -> 1f
                    KeyboardZoomState.ZOOM_IN -> 1.02f
                    KeyboardZoomState.ZOOM_OUT -> 0.98f
                }
            )
        }
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> engine.stateManager.updateIsRunning(!engine.stateManager.isRunning.value)
        }
    }

    const val MAP_NAME = "map_demo"
}