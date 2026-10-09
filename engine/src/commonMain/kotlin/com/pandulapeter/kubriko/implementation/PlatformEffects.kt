/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.types.TargetFrameRate

@Composable
internal expect fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit)

/**
 * Hints the platform to align the display's actual refresh rate with the current [targetFrameRate],
 * so a variable-refresh panel can step down while the game loop is throttled instead of staying
 * pinned at its maximum. Null while the viewport cannot tick, so it requests nothing. No-op on
 * platforms without such a mechanism.
 */
@Composable
internal expect fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate?)

/**
 * Reports the highest refresh rate the display showing the game can present at, and reports again
 * whenever the display changes. Passes null on platforms that don't expose it.
 */
@Composable
internal expect fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit)
