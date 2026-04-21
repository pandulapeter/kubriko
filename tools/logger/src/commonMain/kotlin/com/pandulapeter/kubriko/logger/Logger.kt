/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.logger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A simple global logging utility for Kubriko.
 *
 * It stores a list of [Entry] objects that can be observed via a [StateFlow].
 * This is primarily used by the Debug Menu to display logs within the application.
 */
object Logger {
    private val _logs = MutableStateFlow(emptyList<Entry>())

    /**
     * A flow of the most recent log entries.
     */
    val logs = _logs.asStateFlow()

    /**
     * The maximum number of log entries to keep in memory.
     * When the limit is reached, the oldest entries are discarded.
     */
    var entryLimit = 1000
        set(value) {
            if (value >= 0) {
                field = value
                _logs.update {
                    it.take(value)
                }
            }
        }

    /**
     * Represents a single log message.
     *
     * @property id A unique identifier for the entry.
     * @property message The main log message.
     * @property details Optional additional information.
     * @property source Optional identifier for where the log came from (e.g., a manager name).
     * @property timestamp The time the log was created, in milliseconds since the epoch.
     * @property importance The severity level of the log.
     */
    data class Entry(
        val id: String,
        val message: String,
        val details: String?,
        val source: String?,
        val timestamp: Long,
        val importance: Importance,
    )

    /**
     * Log severity levels.
     */
    enum class Importance {
        /**
         * For verbose debugging information.
         */
        LOW,

        /**
         * For standard information about system state.
         */
        MEDIUM,

        /**
         * For important events or errors.
         */
        HIGH;
    }

    /**
     * Records a new log entry.
     *
     * @param message The main log message.
     * @param details Optional additional information.
     * @param source Optional identifier for where the log came from.
     * @param importance The severity level of the log.
     */
    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    fun log(
        message: String,
        details: String? = null,
        source: String? = null,
        importance: Importance = Importance.HIGH,
    ) = _logs.update {
        buildList {
            add(
                Entry(
                    id = Uuid.random().toString(),
                    message = message,
                    details = details,
                    source = source,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    importance = importance,
                )
            )
            addAll(it)
        }.take(entryLimit)
    }

    /**
     * Clears all recorded log entries.
     */
    fun clearLogs() = _logs.update { emptyList() }
}
