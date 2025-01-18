/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
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