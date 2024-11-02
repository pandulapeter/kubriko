package com.pandulapeter.kubrikoPong.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubrikoPong.implementation.models.Metadata
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
import kubriko.game_pong.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class GameplayController(
    private val kubriko: Kubriko,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    val metadata = combine(
        kubriko.metadataManager.fps,
        kubriko.actorManager.allActors,
        kubriko.actorManager.visibleActorsWithinViewport,
        kubriko.metadataManager.runtimeInMilliseconds,
    ) { fps, allActors, visibleActorsWithinViewport, runtimeInMilliseconds ->
        // TODO: Should be an extension
        Metadata(
            fps = fps,
            totalActorCount = allActors.count(),
            visibleGameObjectCount = visibleActorsWithinViewport.count(),
            playTimeInSeconds = runtimeInMilliseconds / 1000,
        )
    }.stateIn(this, SharingStarted.Eagerly, Metadata())

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