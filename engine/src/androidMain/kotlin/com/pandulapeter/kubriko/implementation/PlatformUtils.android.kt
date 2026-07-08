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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.types.TargetFrameRate

internal actual fun getDefaultFocusDebounce() = 350L

internal actual fun getPlatform(): MetadataManager.Platform = MetadataManager.Platform.Android(
    androidSdkVersion = Build.VERSION.SDK_INT,
)

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    LifecycleFocusEffect(onFocusChanged = onFocusChanged)
}

@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate) {
    val window = LocalContext.current.findActivity()?.window ?: return
    DisposableEffect(window, targetFrameRate) {
        window.setPreferredRefreshRate(targetFrameRate.toPreferredRefreshRate())
        onDispose { window.setPreferredRefreshRate(SYSTEM_DEFAULT_REFRESH_RATE) }
    }
}

private fun TargetFrameRate.toPreferredRefreshRate() = when (this) {
    // DisplayDivider derives its rate by counting real vsync pulses, so it must run at the panel's
    // native rate; hinting the panel down would make the divider count an already-reduced rate and
    // drop the result twice over. DisplayDefault simply wants the maximum. Both therefore release the
    // hint — only an absolute Limit names a rate the panel can settle on directly.
    TargetFrameRate.DisplayDefault, is TargetFrameRate.DisplayDivider -> SYSTEM_DEFAULT_REFRESH_RATE
    is TargetFrameRate.Limit -> framesPerSecond.toFloat()
}

@Suppress("DEPRECATION") // preferredRefreshRate is the refresh-rate lever reachable from a Window across minSdk 29+.
private fun Window.setPreferredRefreshRate(refreshRate: Float) {
    attributes = attributes.apply { preferredRefreshRate = refreshRate }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// 0 lets the system pick the mode, releasing any earlier hint so the panel returns to its default.
private const val SYSTEM_DEFAULT_REFRESH_RATE = 0f
