/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

/**
 * How long the loading overlay stays up after the content has arrived, so that it fades out over a scene
 * that has already drawn its first frames.
 */
val LoadingDismissalDelay = 300.milliseconds

/**
 * Calls [onLoaded] [LoadingDismissalDelay] after every new value that [isLoaded] accepts, in [scope].
 * A value equal to the last accepted one is not a new load.
 */
fun <T> Flow<T>.dismissLoadingWhen(
    scope: CoroutineScope,
    isLoaded: (T) -> Boolean,
    onLoaded: () -> Unit,
): Job = filter(isLoaded)
    .distinctUntilChanged()
    .onEach {
        delay(LoadingDismissalDelay)
        onLoaded()
    }
    .launchIn(scope)
