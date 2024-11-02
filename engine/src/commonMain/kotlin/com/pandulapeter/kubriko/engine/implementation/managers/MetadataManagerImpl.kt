package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.managers.MetadataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class MetadataManagerImpl(private val engineImpl: KubrikoImpl) : MetadataManager {

    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private val _runtimeInMilliseconds = MutableStateFlow(0L)
    override val runtimeInMilliseconds = _runtimeInMilliseconds.asStateFlow()
    private var lastFpsUpdateTimestamp = 0L

    fun updateFps(
        gameTimeNanos: Long,
        deltaTimeInMillis: Float,
    ) {
        if (engineImpl.stateManager.isRunning.value) {
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