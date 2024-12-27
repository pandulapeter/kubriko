package com.pandulapeter.kubriko.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal fun Flow<Long>.distinctUntilChangedWithDelay(minIntervalMillis: Long): Flow<Long> = flow {
    var lastEmissionTime = 0L
    collect { timestamp ->
        if (timestamp - lastEmissionTime >= minIntervalMillis || lastEmissionTime == 0L) {
            emit(timestamp)
            lastEmissionTime = timestamp
        }
    }
}