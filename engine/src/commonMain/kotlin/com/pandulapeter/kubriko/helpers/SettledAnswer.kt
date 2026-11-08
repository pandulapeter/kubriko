/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * A yes-or-no question about the device, asked by [ask] at most once successfully and remembered from then on. A
 * background attempt that never completes (its scope was cancelled before or while it ran) leaves the question open
 * for the next caller instead of marking it as being answered forever.
 */
internal class SettledAnswer(private val ask: suspend () -> Boolean) {

    @Volatile
    var known: Boolean? = null
        private set

    @Volatile
    private var isSettling = false

    suspend fun get() = known ?: ask().also { known = it }

    fun settleInBackground(scope: CoroutineScope) {
        if (known != null || isSettling) return
        isSettling = true
        scope.launch { get() }.invokeOnCompletion { isSettling = false }
    }
}
