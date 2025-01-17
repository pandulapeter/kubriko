/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.logger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object Logger {
    private val _logs = MutableStateFlow(emptyList<Entry>())
    val logs = _logs.asStateFlow()
    var entryLimit = 1000
        set(value) {
            if (value >= 0) {
                field = value
                _logs.update {
                    it.take(value)
                }
            }
        }

    data class Entry(
        val id: String,
        val message: String,
        val details: String?,
        val source: String?,
        val timestamp: Long,
        val importance: Importance,
    )

    enum class Importance {
        LOW,
        MEDIUM,
        HIGH;
    }

    @OptIn(ExperimentalUuidApi::class)
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

    fun clearLogs() = _logs.update { emptyList() }
}