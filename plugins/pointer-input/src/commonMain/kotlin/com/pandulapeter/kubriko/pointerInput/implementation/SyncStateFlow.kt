/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput.implementation

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * A StateFlow wrapper that intercepts [value] reads to calculate them synchronously via [getSyncValue].
 * This keeps [value] correct even while [delegate] has no active collectors and
 * `SharingStarted.WhileSubscribed` has let its upstream combine go idle.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class SyncStateFlow<T>(
    private val delegate: StateFlow<T>,
    private val getSyncValue: () -> T,
) : StateFlow<T> {

    override val replayCache: List<T> get() = delegate.replayCache

    override suspend fun collect(collector: FlowCollector<T>): Nothing = delegate.collect(collector)

    override val value: T get() = getSyncValue()
}
