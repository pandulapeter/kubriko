package com.pandulapeter.gameTemplate.engine.implementation.managers

import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.managers.MetadataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class MetadataManagerImpl : MetadataManager {

    private val _fps = MutableStateFlow(0f)
    override val fps by lazy { _fps.asStateFlow() }
    override val visibleGameObjectCount by lazy { EngineImpl.gameObjectManager.visibleGameObjectsInViewport.map { it.count() }.stateIn(EngineImpl, SharingStarted.Eagerly, 0) }
    override val totalGameObjectCount by lazy { EngineImpl.gameObjectManager.gameObjects.map { it.count() }.stateIn(EngineImpl, SharingStarted.Eagerly, 0) }
    private val _runtimeInMilliseconds = MutableStateFlow(0L)
    override val runtimeInMilliseconds by lazy { _runtimeInMilliseconds.asStateFlow() }
    private var lastFpsUpdateTimestamp = 0L

    fun updateFps(
        gameTimeNanos: Long,
        deltaTimeInMillis: Float,
    ) {
        if (EngineImpl.stateManager.isRunning.value) {
            _runtimeInMilliseconds.update { currentValue ->
                (currentValue + deltaTimeInMillis).toLong()
            }
        }
        if (gameTimeNanos - lastFpsUpdateTimestamp >= 1000000000L) {
            _fps.update { currentValue ->
                if (deltaTimeInMillis == 0f) currentValue else 1000f / deltaTimeInMillis
            }
            lastFpsUpdateTimestamp = gameTimeNanos
        }
    }
}