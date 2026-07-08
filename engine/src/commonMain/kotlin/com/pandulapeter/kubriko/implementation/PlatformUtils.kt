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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.types.TargetFrameRate

internal expect fun getDefaultFocusDebounce(): Long

internal expect fun getPlatform(): MetadataManager.Platform

@Composable
internal expect fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit)

/**
 * Hints the platform to align the display's actual refresh rate with the current [targetFrameRate],
 * so a variable-refresh panel can step down while the game loop is throttled instead of staying
 * pinned at its maximum. No-op on platforms without such a mechanism.
 */
@Composable
internal expect fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate)

@Composable
internal fun LifecycleFocusEffect(
    activeLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
    onFocusChanged: (Boolean) -> Unit,
) {
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, activeLifecycleState) {
        fun updateFocus() {
            currentOnFocusChanged(lifecycle.currentState.isAtLeast(activeLifecycleState))
        }

        val lifecycleObserver = LifecycleEventObserver { _, _ ->
            updateFocus()
        }

        lifecycle.addObserver(lifecycleObserver)
        updateFocus()

        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
}
