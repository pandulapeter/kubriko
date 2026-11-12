/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Files changing on disk outside Kubriko Creator's own writes, for what reads them again then: as events ([changes]),
 * and as a count ([count]) for a view to key what it reads on.
 */
internal class OutsideChanges {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _count = MutableStateFlow(0)

    val changes: Flow<Unit> = _changes

    /**
     * How many times files changed outside since the project opened.
     */
    val count: StateFlow<Int> = _count.asStateFlow()

    fun record() {
        _count.update { it + 1 }
        _changes.tryEmit(Unit)
    }
}
