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

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * How every row of the sidebar comes, goes, and moves, so that opening or closing anything moves all of it together:
 * the rows below slide at [CreatorMotion]'s pace while what was inside fades out quickly, or fades in as they make room.
 * Every row of the sidebar's list takes it, since one that jumped into place among sliding ones would look broken.
 */
internal fun LazyItemScope.sidebarItemMotion(modifier: Modifier = Modifier): Modifier = modifier.animateItem(
    fadeInSpec = CreatorMotion.medium(),
    placementSpec = CreatorMotion.medium(),
    fadeOutSpec = CreatorMotion.short(),
)
