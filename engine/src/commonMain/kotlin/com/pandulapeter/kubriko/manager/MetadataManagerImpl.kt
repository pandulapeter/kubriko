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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.implementation.getPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class MetadataManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : MetadataManager(isLoggingEnabled, instanceNameForLogging) {

    private lateinit var stateManager: StateManager
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private val _totalRuntimeInMilliseconds = MutableStateFlow(0L)
    override val totalRuntimeInMilliseconds = _totalRuntimeInMilliseconds.asStateFlow()
    private val _activeRuntimeInMilliseconds = MutableStateFlow(0L)
    override val activeRuntimeInMilliseconds = _activeRuntimeInMilliseconds.asStateFlow()
    override val platform by lazy { getPlatform() }
    private var fpsFrameCount = 0
    private var fpsAccumulatedTimeMs = 0L

    override fun onInitialize(kubriko: Kubriko) {
        stateManager = (kubriko as KubrikoImpl).stateManager
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            _activeRuntimeInMilliseconds.value += deltaTimeInMilliseconds
        }
        _totalRuntimeInMilliseconds.value += deltaTimeInMilliseconds
        fpsFrameCount++
        fpsAccumulatedTimeMs += deltaTimeInMilliseconds
        if (fpsAccumulatedTimeMs >= FPS_SAMPLE_INTERVAL_MS) {
            _fps.value = fpsFrameCount * 1000f / fpsAccumulatedTimeMs
            fpsFrameCount = 0
            fpsAccumulatedTimeMs = 0L
        }
    }

    companion object {
        private const val FPS_SAMPLE_INTERVAL_MS = 100L
    }
}
