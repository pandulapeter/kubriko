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
    override val fps = _fps.asStateFlow()
    override val visibleGameObjectCount = EngineImpl.gameObjectManager.visibleGameObjectsInViewport.map { it.count() }.stateIn(EngineImpl, SharingStarted.Eagerly, 0)
    override val totalGameObjectCount = EngineImpl.gameObjectManager.gameObjects.map { it.count() }.stateIn(EngineImpl, SharingStarted.Eagerly, 0)
    private var lastFpsUpdateTimestamp = 0L

    fun updateFps(
        gameTimeNanos: Long,
        deltaTimeMillis: Float,
    ) {
        if (gameTimeNanos - lastFpsUpdateTimestamp >= 1000000000L) {
            _fps.update { currentValue ->
                if (deltaTimeMillis == 0f) currentValue else 1000f / deltaTimeMillis
            }
            lastFpsUpdateTimestamp = gameTimeNanos
        }
    }
}