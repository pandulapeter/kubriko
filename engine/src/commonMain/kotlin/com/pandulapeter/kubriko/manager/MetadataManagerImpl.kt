/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.implementation.getPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

internal class MetadataManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : MetadataManager(isLoggingEnabled, instanceNameForLogging) {
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManagerImpl
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private val _totalRuntimeInMilliseconds = MutableStateFlow(0L)
    override val totalRuntimeInMilliseconds = _totalRuntimeInMilliseconds.asStateFlow()
    private val _activeRuntimeInMilliseconds = MutableStateFlow(0L)
    override val activeRuntimeInMilliseconds = _activeRuntimeInMilliseconds.asStateFlow()
    override val platform by lazy { getPlatform() }

    override fun onInitialize(kubriko: Kubriko) {
        stateManager = (kubriko as KubrikoImpl).stateManager
        viewportManager = kubriko.viewportManager
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        val frameCount = remember { mutableStateOf(0) }
        val lastUpdateTime = remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) {
            while (isActive) {
                val frameTime = withFrameNanos { it }
                frameCount.value++
                if (frameTime - lastUpdateTime.value >= 100_000_000L) {
                    _fps.update { (frameCount.value * 1_000_000_000L / (frameTime - lastUpdateTime.value).toFloat()) / viewportManager.frameRate.factor }
                    frameCount.value = 0
                    lastUpdateTime.value = frameTime
                }
            }
        }
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            _activeRuntimeInMilliseconds.update { currentValue -> currentValue + deltaTimeInMilliseconds }
        }
        _totalRuntimeInMilliseconds.update { currentValue -> currentValue + deltaTimeInMilliseconds }
    }
}