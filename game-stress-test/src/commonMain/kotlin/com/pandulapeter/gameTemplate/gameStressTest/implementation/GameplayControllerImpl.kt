package com.pandulapeter.gameTemplate.gameStressTest.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardZoomState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.zoomState
import com.pandulapeter.gameTemplate.gameStressTest.GameObjectRegistry
import com.pandulapeter.gameTemplate.gameStressTest.GameplayController
import com.pandulapeter.gameTemplate.gameStressTest.models.Metadata
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

internal object GameplayControllerImpl : GameplayController, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override val metadata = combine(
        Engine.get().metadataManager.fps,
        Engine.get().metadataManager.totalGameObjectCount,
        Engine.get().metadataManager.visibleGameObjectCount,
        Engine.get().metadataManager.runtimeInMilliseconds,
    ) { fps, totalGameObjectCount, visibleGameObjectCount, runtimeInMilliseconds ->
        Metadata(
            fps = fps,
            totalGameObjectCount = totalGameObjectCount,
            visibleGameObjectCount = visibleGameObjectCount,
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }.stateIn(this, SharingStarted.Eagerly, Metadata())

    init {
        Engine.get().stateManager.isFocused
            .filterNot { it }
            .onEach { Engine.get().stateManager.updateIsRunning(false) }
            .launchIn(this)
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(GameplayControllerImpl::handleKeys)
            .launchIn(this)
        Engine.get().inputManager.onKeyReleased
            .onEach(GameplayControllerImpl::handleKeyReleased)
            .launchIn(this)
        start()
    }

    private fun start() {
        launch {
            Engine.get().gameObjectManager.register(
                entries = GameObjectRegistry.entries,
            )
            loadMap("map_demo")
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadMap(mapName: String) {
        try {
            Engine.get().gameObjectManager.deserializeState(Res.readBytes("files/maps/$mapName.json").decodeToString())
        } catch (_: MissingResourceException) {
        }
    }

    private fun handleKeys(keys: Set<Key>) {
        if (Engine.get().stateManager.isRunning.value) {
            Engine.get().viewportManager.multiplyScaleFactor(
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
            Key.Escape, Key.Back, Key.Backspace -> Engine.get().stateManager.updateIsRunning(!Engine.get().stateManager.isRunning.value)
        }
    }
}