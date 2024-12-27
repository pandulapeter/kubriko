package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class MetadataManagerImpl : MetadataManager() {

    private lateinit var stateManager: StateManager
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private var startTimeInMilliseconds = 0L
    private val _totalRuntimeInMilliseconds = MutableStateFlow(0L)
    override val totalRuntimeInMilliseconds = _totalRuntimeInMilliseconds.asStateFlow()
    private val _activeRuntimeInMilliseconds = MutableStateFlow(0L)
    override val activeRuntimeInMilliseconds = _activeRuntimeInMilliseconds.asStateFlow()
    private var lastFpsUpdateTimestamp = 0L

    override fun onInitialize(kubriko: Kubriko) {
        stateManager = (kubriko as KubrikoImpl).stateManager
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (stateManager.isRunning.value) {
            _activeRuntimeInMilliseconds.update { currentValue ->
                (currentValue + deltaTimeInMillis).toLong()
            }
        }
        if (startTimeInMilliseconds == 0L) {
            startTimeInMilliseconds = gameTimeNanos / 1000000
        }
        _totalRuntimeInMilliseconds.update { (gameTimeNanos - startTimeInMilliseconds) / 1000000 }
        if (gameTimeNanos - lastFpsUpdateTimestamp >= 1000000000L) {
            _fps.update { currentValue ->
                if (deltaTimeInMillis == 0f) currentValue else 1000f / deltaTimeInMillis
            }
            lastFpsUpdateTimestamp = gameTimeNanos
        }
    }
}