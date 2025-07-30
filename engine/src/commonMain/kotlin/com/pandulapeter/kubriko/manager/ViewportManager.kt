/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.FrameRate
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class ViewportManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "ViewportManager",
) {
    abstract val cameraPosition: StateFlow<SceneOffset> // Center of the viewport
    abstract val size: StateFlow<Size>
    abstract val rawScaleFactor: StateFlow<Scale>
    abstract val scaleFactor: StateFlow<Scale>
    abstract val minimumScaleFactor: Float
    abstract val maximumScaleFactor: Float
    abstract val topLeft: StateFlow<SceneOffset>
    abstract val bottomRight: StateFlow<SceneOffset>

    abstract fun addToCameraPosition(offset: Offset)

    abstract fun setCameraPosition(position: SceneOffset)

    abstract fun setScaleFactor(scaleFactor: Float)

    abstract fun multiplyScaleFactor(scaleFactor: Float)

    sealed class AspectRatioMode {

        data object Dynamic : AspectRatioMode()

        data class FitHorizontal(
            val width: SceneUnit,
        ) : AspectRatioMode()

        data class FitVertical(
            val height: SceneUnit,
        ) : AspectRatioMode()

        data class Fixed(
            val ratio: Float,
            val width: SceneUnit,
            val alignment: Alignment = Alignment.Center,
        ) : AspectRatioMode()

        data class Stretched(
            val size: SceneSize,
        ) : AspectRatioMode()
    }

    companion object {
        fun newInstance(
            aspectRatioMode: AspectRatioMode = AspectRatioMode.Dynamic,
            initialScaleFactor: Float = 1f,
            minimumScaleFactor: Float = 0.2f,
            maximumScaleFactor: Float = 5f,
            viewportEdgeBuffer: SceneUnit = 0f.sceneUnit,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
            frameRate: FrameRate = FrameRate.NORMAL,
        ): ViewportManager = ViewportManagerImpl(
            aspectRatioMode = aspectRatioMode,
            initialScaleFactor = initialScaleFactor,
            minimumScaleFactor = minimumScaleFactor,
            maximumScaleFactor = maximumScaleFactor,
            viewportEdgeBuffer = viewportEdgeBuffer,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
            frameRate = frameRate,
        )
    }
}