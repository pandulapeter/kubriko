/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

/**
 * Where the transcript list is scrolled to: its first visible item and how far into it.
 */
internal data class TranscriptScroll(
    val index: Int,
    val offset: Int,
) : Comparable<TranscriptScroll> {
    override fun compareTo(other: TranscriptScroll) = compareValuesBy(this, other, TranscriptScroll::index, TranscriptScroll::offset)
}

/**
 * Whether the transcript list keeps following what the agent writes: it stops once the user scrolls up (from [previous]
 * to [current]), and starts again once they reach the end. Content growing at the end moves neither.
 */
internal fun followsEnd(wasFollowing: Boolean, previous: TranscriptScroll, current: TranscriptScroll, canScrollForward: Boolean) = when {
    !canScrollForward -> true
    current < previous -> false
    else -> wasFollowing
}
