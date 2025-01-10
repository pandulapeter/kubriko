package com.pandulapeter.kubriko.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlin.math.roundToLong

internal class MetadataManagerImpl(
    isLoggingEnabled: Boolean,
) : MetadataManager(isLoggingEnabled) {
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

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        val frameCount = remember { mutableStateOf(0) }
        val lastUpdateTime = remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) {
            while (isActive) {
                val frameTime = withFrameNanos { it }
                frameCount.value++
                if (frameTime - lastUpdateTime.value >= 100_000_000L) {
                    _fps.update { frameCount.value * 1_000_000_000L / (frameTime - lastUpdateTime.value).toFloat() }
                    frameCount.value = 0
                    lastUpdateTime.value = frameTime
                }
            }
        }
    }

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        if (startTimeInMilliseconds == 0L) {
            startTimeInMilliseconds = gameTimeMilliseconds
        }
        if (stateManager.isRunning.value) {
            _activeRuntimeInMilliseconds.update { currentValue -> currentValue + deltaTimeInMilliseconds.roundToLong() }
        }
        _totalRuntimeInMilliseconds.update { currentValue -> currentValue + deltaTimeInMilliseconds.roundToLong() }
        if (gameTimeMilliseconds - lastFpsUpdateTimestamp >= 100) {
            lastFpsUpdateTimestamp = gameTimeMilliseconds
        }
    }
}