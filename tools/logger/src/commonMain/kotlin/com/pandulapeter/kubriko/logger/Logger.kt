package com.pandulapeter.kubriko.logger

import com.pandulapeter.kubriko.logger.implementation.getCurrentTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object Logger {
    private val _logs = MutableStateFlow(emptyList<Entry>())
    val logs = _logs.asStateFlow()
    var entryLimit = 100
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
                    timestamp = getCurrentTimestamp(),
                    importance = importance,
                )
            )
            addAll(it)
        }.take(entryLimit)
    }

    fun clearLogs() = _logs.update { emptyList() }
}