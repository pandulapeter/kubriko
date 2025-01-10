package com.pandulapeter.kubriko.logger

import com.pandulapeter.kubriko.logger.implementation.getCurrentTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object Logger {

    private val _logs = MutableStateFlow(emptyList<Entry>())
    val logs = _logs.asStateFlow()

    data class Entry(
        val message: String,
        val details: String?,
        val source: String?,
        val timestamp: Long,
    )

    fun log(
        message: String,
        details: String? = null,
        source: String? = null,
    ) = _logs.update {
        buildList {
            add(
                Entry(
                    message = message,
                    details = details,
                    source = source,
                    timestamp = getCurrentTimestamp()
                )
            )
            addAll(it)
        }
    }

    fun clearLogs() = _logs.update { emptyList() }
}