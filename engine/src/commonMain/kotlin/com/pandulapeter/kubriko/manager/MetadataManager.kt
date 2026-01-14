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
 * TODO: Documentation
 */
sealed class MetadataManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "MetadataManager",
) {
    abstract val fps: StateFlow<Float>
    abstract val totalRuntimeInMilliseconds: StateFlow<Long>
    abstract val activeRuntimeInMilliseconds: StateFlow<Long>
    abstract val platform: Platform

    /**
     * Represents the target platform the application is currently running on.
     */
    sealed class Platform {

        data class Android(
            val androidSdkVersion: Int,
        ) : Platform()

        sealed class Desktop : Platform() {

            abstract val javaVersion: String

            data class MacOS(
                val macOSVersion: String,
                override val javaVersion: String,
            ) : Desktop()

            data class Linux(
                val linuxVersion: String,
                override val javaVersion: String,
            ) : Desktop()

            data class Windows(
                val windowsVersion: String,
                override val javaVersion: String,
            ) : Desktop()
        }

        data class IOS(
            val iOSVersion: String,
        ) : Platform()

        data class Web(
            val userAgent: String,
        ) : Platform()
    }

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): MetadataManager = MetadataManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}