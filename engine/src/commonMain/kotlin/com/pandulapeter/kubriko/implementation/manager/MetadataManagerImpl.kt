package com.pandulapeter.kubriko.implementation.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class MetadataManagerImpl : MetadataManager() {

    private lateinit var stateManager: StateManager
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private val _runtimeInMilliseconds = MutableStateFlow(0L)
    override val runtimeInMilliseconds = _runtimeInMilliseconds.asStateFlow()
    private var lastFpsUpdateTimestamp = 0L

    override fun initialize(kubriko: Kubriko) {
        stateManager = kubriko.get<StateManager>()
    }

    override fun update(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (stateManager.isRunning.value) {
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