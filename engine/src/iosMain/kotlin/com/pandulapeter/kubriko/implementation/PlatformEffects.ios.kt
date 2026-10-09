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
import com.pandulapeter.kubriko.types.TargetFrameRate
import platform.Foundation.NSBundle
import platform.Foundation.NSNumber
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiomPhone

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    LifecycleFocusEffect(onFocusChanged = onFocusChanged)
}

@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate?) = Unit

@Composable
internal actual fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit) {
    val currentOnMaximumDisplayRefreshRateChanged by rememberUpdatedState(onMaximumDisplayRefreshRateChanged)
    DisposableEffect(Unit) {
        currentOnMaximumDisplayRefreshRateChanged(maximumPresentableFramesPerSecond())
        onDispose { }
    }
}

/**
 * Core Animation caps an iPhone app at 60 Hz unless its Info.plist sets CADisableMinimumFrameDurationOnPhone, while
 * [UIScreen.maximumFramesPerSecond] still reports the ProMotion panel's 120. iPads need no such key.
 */
private fun maximumPresentableFramesPerSecond(): Float {
    val maximum = UIScreen.mainScreen.maximumFramesPerSecond.toFloat()
    val isPhone = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone
    return if (isPhone && !isProMotionUnlockedOnPhone) minOf(maximum, IPHONE_DEFAULT_MAXIMUM_FRAMES_PER_SECOND) else maximum
}

/** Read once: the Info.plist cannot change at runtime. The flag arrives as an NSNumber, so that is checked first. */
private val isProMotionUnlockedOnPhone by lazy {
    when (val value = NSBundle.mainBundle.objectForInfoDictionaryKey("CADisableMinimumFrameDurationOnPhone")) {
        is NSNumber -> value.boolValue
        is Boolean -> value
        else -> false
    }
}

private const val IPHONE_DEFAULT_MAXIMUM_FRAMES_PER_SECOND = 60f
