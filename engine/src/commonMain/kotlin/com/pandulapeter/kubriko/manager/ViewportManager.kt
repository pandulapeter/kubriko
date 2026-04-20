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
 * Manages the camera and viewport properties of the game.
 * This includes tracking camera position, zoom levels (scale factor), and viewport dimensions.
 */
sealed class ViewportManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "ViewportManager",
) {
    /**
     * The current center point of the camera in the scene.
     */
    abstract val cameraPosition: StateFlow<SceneOffset>

    /**
     * The actual size of the viewport on the screen in pixels.
     */
    abstract val size: StateFlow<Size>

    /**
     * The raw scale factor being applied to the scene before any clamping or aspect ratio adjustments.
     */
    abstract val rawScaleFactor: StateFlow<Scale>

    /**
     * The final scale factor being applied to the scene.
     */
    abstract val scaleFactor: StateFlow<Scale>

    /**
     * The minimum allowed scale factor for the camera.
     */
    abstract val minimumScaleFactor: Float

    /**
     * The maximum allowed scale factor for the camera.
     */
    abstract val maximumScaleFactor: Float

    /**
     * The top-left corner coordinates of the visible area in the scene.
     */
    abstract val topLeft: StateFlow<SceneOffset>

    /**
     * The bottom-right corner coordinates of the visible area in the scene.
     */
    abstract val bottomRight: StateFlow<SceneOffset>

    /**
     * Offsets the camera by a specific amount.
     *
     * @param offset The amount to move the camera in screen pixels.
     */
    abstract fun addToCameraPosition(offset: Offset)

    /**
     * Moves the camera to a specific coordinate in the scene.
     *
     * @param position The new center position for the camera.
     */
    abstract fun setCameraPosition(position: SceneOffset)

    /**
     * Sets a new scale factor (zoom level) for the camera.
     *
     * @param scaleFactor The new scale level.
     */
    abstract fun setScaleFactor(scaleFactor: Float)

    /**
     * Multiplies the current scale factor by a given amount.
     *
     * @param scaleFactor The multiplier to apply to the current zoom level.
     */
    abstract fun multiplyScaleFactor(scaleFactor: Float)

    /**
     * Defines how the viewport should handle different screen aspect ratios.
     */
    sealed class AspectRatioMode {

        /**
         * The viewport always matches the screen's dimensions.
         */
        data object Dynamic : AspectRatioMode()

        /**
         * The viewport's width is fixed to a specific scene unit, and height is calculated based on screen aspect ratio.
         */
        data class FitHorizontal(
            val width: SceneUnit,
        ) : AspectRatioMode()

        /**
         * The viewport's height is fixed to a specific scene unit, and width is calculated based on screen aspect ratio.
         */
        data class FitVertical(
            val height: SceneUnit,
        ) : AspectRatioMode()

        /**
         * The viewport has a fixed aspect ratio and minimum width, potentially adding letterboxing.
         */
        data class Fixed(
            val ratio: Float,
            val width: SceneUnit,
            val alignment: Alignment = Alignment.Center,
        ) : AspectRatioMode()

        /**
         * The viewport is stretched to match a specific scene size, ignoring the screen aspect ratio.
         */
        data class Stretched(
            val size: SceneSize,
        ) : AspectRatioMode()
    }

    companion object {
        /**
         * Creates a new [ViewportManager] instance.
         *
         * @param aspectRatioMode How the viewport should scale to fit the screen.
         * @param initialScaleFactor The starting zoom level.
         * @param minimumScaleFactor The minimum zoom level allowed.
         * @param maximumScaleFactor The maximum zoom level allowed.
         * @param viewportEdgeBuffer An extra margin around the viewport where actors are still considered visible.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         * @param frameRate The target frame rate for updates.
         */
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