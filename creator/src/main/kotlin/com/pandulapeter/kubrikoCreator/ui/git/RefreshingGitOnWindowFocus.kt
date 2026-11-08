/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.git

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Reads the repository again whenever the window gets the focus back, as IntelliJ does, since the project watcher leaves
 * `.git` out and a commit or a checkout made in a terminal would otherwise go unseen.
 */
@Composable
internal fun RefreshingGitOnWindowFocus(
    refresh: () -> Unit,
) {
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    val currentRefresh by rememberUpdatedState(refresh)
    LaunchedEffect(isWindowFocused) {
        if (isWindowFocused) currentRefresh()
    }
}
