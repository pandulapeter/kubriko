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

/**
 * Which Event is shown, given the Events' [keys] ([ListItemKeys]): the one of [selectedKey] while it exists, else the one
 * at [fallbackIndex] (where the selected one was, clamped), else the first; none without Events.
 */
internal fun selectedEventIndex(keys: List<Long>, selectedKey: Long?, fallbackIndex: Int?): Int? = when {
    keys.isEmpty() -> null
    selectedKey != null && selectedKey in keys -> keys.indexOf(selectedKey)
    fallbackIndex == null -> 0
    else -> fallbackIndex.coerceIn(0, keys.lastIndex)
}
