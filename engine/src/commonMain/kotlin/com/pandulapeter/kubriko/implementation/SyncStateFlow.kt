/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * A custom StateFlow wrapper that intercepts `.value` reads to calculate them synchronously.
 * This prevents 1-frame asynchronous lag when the engine reads combined viewport bounds.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class SyncStateFlow<T>(
    private val delegate: StateFlow<T>,
    private val getSyncValue: () -> T
) : StateFlow<T> {
    override val replayCache: List<T> get() = delegate.replayCache
    override suspend fun collect(collector: FlowCollector<T>): Nothing = delegate.collect(collector)
    override val value: T get() = getSyncValue()
}