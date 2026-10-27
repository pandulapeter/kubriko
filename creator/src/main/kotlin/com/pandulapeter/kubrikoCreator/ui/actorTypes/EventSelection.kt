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
 * Which Event's Blocks are shown when [requestedIndex] was selected and the Actor type has [eventCount] Events: the
 * requested one when it still exists, the last one when it was past the end (after a removal), and none without Events.
 */
internal fun selectedEventIndex(requestedIndex: Int?, eventCount: Int): Int? = when {
    eventCount == 0 -> null
    requestedIndex == null -> 0
    else -> requestedIndex.coerceIn(0, eventCount - 1)
}
