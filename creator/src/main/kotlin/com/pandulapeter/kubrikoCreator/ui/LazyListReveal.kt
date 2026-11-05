/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState

/**
 * Scrolls the list from its top until the item of [key] is in view, a viewport at a time, since a lazy list knows the
 * index only of what it has laid out; stops at the end if there is no such item.
 */
internal suspend fun LazyListState.revealItem(key: Any) {
    scrollToItem(0)
    while (true) {
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        if (item != null) {
            animateScrollToItem((item.index - 1).coerceAtLeast(0))
            return
        }
        if (!canScrollForward) return
        scrollBy(layoutInfo.viewportSize.height.toFloat())
    }
}
