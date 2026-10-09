/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.createShowcaseStateHolder

/**
 * The Showcase's process-scoped state: the selected entry, the [StateHolder]s of the entries that are open and the
 * welcome screen's expanded section. It outlives the Activity on Android, so a running game survives a configuration
 * change.
 *
 * @param createStateHolder Builds the [StateHolder] of an entry the first time it is needed.
 */
internal class ShowcaseSession(
    private val createStateHolder: (ShowcaseEntry) -> StateHolder = ::createShowcaseStateHolder,
) {
    private val _selectedEntry = mutableStateOf<ShowcaseEntry?>(null)
    val selectedEntry: State<ShowcaseEntry?> = _selectedEntry
    private val stateHolders = mutableMapOf<ShowcaseEntry, StateHolder>()

    /** Whether the compact welcome screen shows its "more details" section. */
    var isWelcomeMoreInfoVisible by mutableStateOf(false)

    /** Selects [entry] (`null` for the welcome screen), creating its [StateHolder] before the frame that shows it. */
    fun select(entry: ShowcaseEntry?) {
        entry?.let(::holderFor)
        _selectedEntry.value = entry
    }

    fun isSelected(entry: ShowcaseEntry) = _selectedEntry.value == entry

    /** The [StateHolder] of [entry], created if it does not exist yet. */
    fun holderFor(entry: ShowcaseEntry): StateHolder = stateHolders.getOrPut(entry) { createStateHolder(entry) }

    /** Disposes the [StateHolder] of [entry], if it has one, so the next [holderFor] creates a new one. */
    fun release(entry: ShowcaseEntry) {
        stateHolders.remove(entry)?.dispose()
    }
}
