/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A request for a field to take the focus, which stays [isPending] until the field takes it ([consume]), so a field
 * composed again later doesn't take the focus a second time.
 */
internal class FocusRequest {
    var isPending by mutableStateOf(false)
        private set

    fun request() {
        isPending = true
    }

    fun consume() {
        isPending = false
    }
}
