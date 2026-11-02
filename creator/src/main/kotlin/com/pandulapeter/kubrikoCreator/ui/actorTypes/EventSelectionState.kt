/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.actors.EventDefinition

/**
 * Which Event the type editor shows, kept as the Event's [ListItemKeys] key rather than its index, so it stays on the same
 * Event when Events are added, moved, or removed before it (by undo, redo, or the AI too), and moves nowhere when a command
 * is rejected.
 */
@Stable
internal class EventSelectionState {
    private val itemKeys = ListItemKeys<EventDefinition>()
    private var selectedKey by mutableStateOf<Long?>(null)
    private var pendingKey by mutableStateOf<Long?>(null)
    private var lastShownIndex: Int? = null

    /**
     * The keys of [events], index by index, following them from the previous version.
     */
    fun keysOf(events: List<EventDefinition>) = itemKeys.update(events)

    /**
     * Whether the Event of [key] was just added, so it is pointed out.
     */
    fun isRecentlyAdded(key: Long) = itemKeys.isRecentlyAdded(key)

    /**
     * Shows the Event of [key].
     */
    fun select(key: Long) {
        selectedKey = key
    }

    /**
     * Shows the last of [events], the type's Events right after an add that appended one succeeded, once the Events shown
     * hold it: they may show it before the add's success arrives, or only after it, with other changes made since.
     */
    fun selectAdded(events: List<EventDefinition>) {
        pendingKey = itemKeys.update(events).lastOrNull()
    }

    /**
     * The index of the Event shown of the ones with [keys], selecting the one [highlight] points at when it changes.
     */
    @Composable
    fun shownIndex(keys: List<Long>, highlight: ActorTypeHighlight?): Int? {
        val currentKeys by rememberUpdatedState(keys)
        LaunchedEffect(highlight?.key) {
            highlight?.eventIndex?.let(currentKeys::getOrNull)?.let(::select)
        }
        LaunchedEffect(keys, pendingKey) {
            pendingKey?.takeIf { it in keys }?.let {
                selectedKey = it
                pendingKey = null
            }
        }
        val index = selectedEventIndex(keys, selectedKey, lastShownIndex)
        SideEffect {
            lastShownIndex = index
            // Only when the selected Event is gone: an effect of this same composition may already have selected another.
            if (selectedKey !in keys) {
                selectedKey = index?.let(keys::get)
            }
        }
        return index
    }
}
