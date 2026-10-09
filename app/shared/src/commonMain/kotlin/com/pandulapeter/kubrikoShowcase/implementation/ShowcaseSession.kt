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
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubrikoShowcase.implementation.ui.createShowcaseStateHolder

/**
 * The Showcase's process-scoped state: the selected entry, the [StateHolder]s of the entries that are open, their
 * [SceneEditorConnection]s, the examples' info panel visibility and the welcome screen's expanded section. It outlives the Activity on Android, so a running
 * game survives a configuration change.
 *
 * @param createStateHolder Builds the [StateHolder] of an entry the first time it is needed, handing it the entry's
 * [SceneEditorConnection].
 */
internal class ShowcaseSession(
    private val createStateHolder: (ShowcaseEntry, SceneEditorConnection) -> StateHolder = ::createShowcaseStateHolder,
) {
    private val _selectedEntry = mutableStateOf<ShowcaseEntry?>(null)
    val selectedEntry: State<ShowcaseEntry?> = _selectedEntry
    private val stateHolders = mutableMapOf<ShowcaseEntry, StateHolder>()
    private val sceneEditorConnections = mutableMapOf<ShowcaseEntry, SceneEditorConnection>()

    /** Whether the examples show their info panel, toggled by the top bar's info button. */
    var isInfoPanelVisible by mutableStateOf(true)
        private set

    /** Whether the compact welcome screen shows its "more details" section. */
    var isWelcomeMoreInfoVisible by mutableStateOf(false)

    /** Selects [entry] (`null` for the welcome screen), creating its [StateHolder] before the frame that shows it. */
    fun select(entry: ShowcaseEntry?) {
        entry?.let(::holderFor)
        _selectedEntry.value = entry
    }

    fun toggleInfoPanelVisibility() {
        isInfoPanelVisible = !isInfoPanelVisible
    }

    fun isSelected(entry: ShowcaseEntry) = _selectedEntry.value == entry

    /** The [StateHolder] of [entry], created if it does not exist yet. */
    fun holderFor(entry: ShowcaseEntry): StateHolder = stateHolders.getOrPut(entry) {
        createStateHolder(entry, sceneEditorConnectionFor(entry))
    }

    /**
     * The [SceneEditorConnection] of [entry]. It is created once and never released, so a scene editor window stays
     * connected while the entry's [StateHolder] is released and created again.
     */
    fun sceneEditorConnectionFor(entry: ShowcaseEntry): SceneEditorConnection = sceneEditorConnections.getOrPut(entry, ::SceneEditorConnection)

    /** Disposes the [StateHolder] of [entry], if it has one, so the next [holderFor] creates a new one. */
    fun release(entry: ShowcaseEntry) {
        stateHolders.remove(entry)?.dispose()
    }
}
