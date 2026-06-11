/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.manager

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ControlManager : Manager() {

    private val viewportManager by manager<ViewportManager>()
    private val _controlDirection = MutableStateFlow<AngleRadians?>(null)
    internal val controlDirection = _controlDirection.asStateFlow()
    private val _controlSpeedFactor = MutableStateFlow(0f)
    internal val controlSpeedFactor = _controlSpeedFactor.asStateFlow()
    private val _cameraOffset = MutableStateFlow(SceneOffset.Zero)
    val cameraOffset = _cameraOffset.asStateFlow()

    fun onControlDirectionChanged(direction: AngleRadians?, speedFactor: Float = 1f) {
        _controlDirection.update { direction }
        _controlSpeedFactor.update { if (direction != null) speedFactor else 0f }
    }

    internal fun onCameraOffsetChanged(offset: SceneOffset) = _cameraOffset.update { offset }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        viewportManager.setCameraPosition(_cameraOffset.value)
    }
}
