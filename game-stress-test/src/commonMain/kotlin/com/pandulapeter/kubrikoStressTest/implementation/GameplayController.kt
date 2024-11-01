package com.pandulapeter.kubrikoStressTest.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.implementation.extensions.KeyboardZoomState
import com.pandulapeter.kubriko.engine.implementation.extensions.zoomState
import com.pandulapeter.kubrikoStressTest.implementation.models.Metadata
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
import kubriko.game_stress_test.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal object GameplayController : CoroutineScope {

    const val MAP_NAME = "map_demo"

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val kubriko = Kubriko.newInstance(editableActorMetadata = GameObjectRegistry.typesAvailableInEditor)
    val metadata = combine(
        kubriko.metadataManager.fps,
        kubriko.metadataManager.totalGameObjectCount,
        kubriko.metadataManager.visibleGameObjectCount,
        kubriko.metadataManager.runtimeInMilliseconds,
    ) { fps, totalGameObjectCount, visibleGameObjectCount, runtimeInMilliseconds ->
        Metadata(
            fps = fps,
            totalGameObjectCount = totalGameObjectCount,
            visibleGameObjectCount = visibleGameObjectCount,
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }.stateIn(this, SharingStarted.Eagerly, Metadata())

    init {
        kubriko.stateManager.isFocused
            .filterNot { it }
            .onEach { kubriko.stateManager.updateIsRunning(false) }
            .launchIn(this)
        kubriko.inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach(::handleKeys)
            .launchIn(this)
        kubriko.inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        loadMap(MAP_NAME)
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap(mapName: String) = launch {
        try {
            kubriko.instanceManager.deserializeState(Res.readBytes("files/maps/$mapName.json").decodeToString())
        } catch (_: MissingResourceException) {
        }
    }

    private fun handleKeys(keys: Set<Key>) {
        if (kubriko.stateManager.isRunning.value) {
            kubriko.viewportManager.multiplyScaleFactor(
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
            Key.Escape, Key.Back, Key.Backspace -> kubriko.stateManager.updateIsRunning(!kubriko.stateManager.isRunning.value)
        }
    }
}