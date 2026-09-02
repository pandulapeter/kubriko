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

import kotlinx.coroutines.flow.StateFlow

/**
 * Provides metadata and statistics about the game's execution environment.
 * This includes frame rate (FPS), runtime tracking, and platform information.
 */
sealed class MetadataManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "MetadataManager",
) {
    /**
     * The current frame rate (frames per second).
     */
    abstract val fps: StateFlow<Float>

    /**
     * The total elapsed time since the game started, in milliseconds.
     */
    abstract val totalRuntimeInMilliseconds: StateFlow<Long>

    /**
     * The total elapsed time while the game was not paused, in milliseconds.
     */
    abstract val activeRuntimeInMilliseconds: StateFlow<Long>

    /**
     * The highest refresh rate the display showing the game can present at, in Hz, or `null` while
     * the platform does not report one (the web, where browsers expose no such API). This is the
     * panel's own ceiling, not the rate it is currently running at, so it is unaffected by any
     * refresh-rate hint a [com.pandulapeter.kubriko.types.TargetFrameRate] applies - which makes it
     * the rate to derive a frame rate menu from, since targets that divide it evenly are the ones a
     * variable-refresh panel can settle on and present with even pacing. Follows the display, so it
     * updates when the device unfolds or the window moves to another screen.
     */
    abstract val maximumDisplayRefreshRate: StateFlow<Float?>

    /**
     * Information about the platform the game is running on.
     */
    abstract val platform: Platform

    /**
     * Represents the target platform the application is currently running on.
     */
    sealed class Platform {

        /**
         * Android platform information.
         */
        data class Android(
            val androidSdkVersion: Int,
        ) : Platform()

        /**
         * Base class for desktop platform information.
         */
        sealed class Desktop : Platform() {

            abstract val javaVersion: String

            /**
             * macOS platform information.
             */
            data class MacOS(
                val macOSVersion: String,
                override val javaVersion: String,
            ) : Desktop()

            /**
             * Linux platform information.
             */
            data class Linux(
                val linuxVersion: String,
                override val javaVersion: String,
            ) : Desktop()

            /**
             * Windows platform information.
             */
            data class Windows(
                val windowsVersion: String,
                override val javaVersion: String,
            ) : Desktop()
        }

        /**
         * iOS platform information.
         */
        data class IOS(
            val iOSVersion: String,
        ) : Platform()

        /**
         * Web platform information.
         */
        data class Web(
            val userAgent: String,
        ) : Platform()
    }

    companion object {
        /**
         * Creates a new [MetadataManager] instance.
         *
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name to use for this instance in log messages.
         */
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): MetadataManager = MetadataManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}