/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The assistant's settings, read from [store] once and saved to it on [ioDispatcher] after every change.
 */
internal class AssistantSettingsState(
    private val scope: CoroutineScope,
    private val store: AssistantSettingsStore,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val _value = MutableStateFlow(store.load())
    val value = _value.asStateFlow()
    private val saves = Mutex()

    /**
     * Changes the settings by [transform] at once, and saves them.
     */
    fun update(transform: (AssistantSettings) -> AssistantSettings) {
        _value.update(transform)
        // Saves can finish in any order, so each one writes the settings as they are by then.
        scope.launch(ioDispatcher) { saves.withLock { store.save(_value.value) } }
    }
}
