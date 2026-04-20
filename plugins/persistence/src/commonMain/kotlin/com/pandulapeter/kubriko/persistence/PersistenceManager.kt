/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Manager responsible for persisting game data.
 *
 * It provides [MutableStateFlow]s for different data types that are automatically
 * synchronized with local storage.
 */
sealed class PersistenceManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "PersistenceManager",
) {

    /**
     * Returns a [MutableStateFlow] for a boolean value.
     */
    abstract fun boolean(
        key: String,
        defaultValue: Boolean = false,
    ): MutableStateFlow<Boolean>

    /**
     * Returns a [MutableStateFlow] for an integer value.
     */
    abstract fun int(
        key: String,
        defaultValue: Int = 0,
    ): MutableStateFlow<Int>

    /**
     * Returns a [MutableStateFlow] for a float value.
     */
    abstract fun float(
        key: String,
        defaultValue: Float = 0f,
    ): MutableStateFlow<Float>

    /**
     * Returns a [MutableStateFlow] for a string value.
     */
    abstract fun string(
        key: String,
        defaultValue: String = "",
    ): MutableStateFlow<String>

    /**
     * Returns a [MutableStateFlow] for a generic value that can be serialized to a string.
     */
    abstract fun <T> generic(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): MutableStateFlow<T>

    companion object {
        /**
         * Creates a new [PersistenceManager] instance.
         *
         * @param fileName The name of the storage file.
         * @param isLoggingEnabled Whether to enable logging for this manager.
         * @param instanceNameForLogging Optional name for logging purposes.
         */
        fun newInstance(
            fileName: String = "kubrikoPreferences",
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): PersistenceManager = PersistenceManagerImpl(
            fileName = fileName,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
