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
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.pandulapeter.kubriko.types.TargetFrameRate
import java.util.WeakHashMap
import kotlin.math.roundToInt

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    LifecycleFocusEffect(onFocusChanged = onFocusChanged)
}

/**
 * The requests of every viewport in a window, combined into the one hint the window gets, so that neither the last
 * viewport to compose nor the first to leave decides for all of them. Only touched from the main thread.
 */
private val frameRateHintRequests = WeakHashMap<Window, ArrayList<TargetFrameRate>>()

@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate?) {
    val window = LocalContext.current.findActivity()?.window ?: return
    if (targetFrameRate == null) return
    DisposableEffect(window, targetFrameRate) {
        val requests = frameRateHintRequests.getOrPut(window) { ArrayList() }
        requests.add(targetFrameRate)
        window.applyFrameRateHint(combineFrameRateHints(requests))
        onDispose {
            val index = requests.indexOfFirst { it === targetFrameRate }
            if (index >= 0) {
                requests.removeAt(index)
            }
            if (requests.isEmpty()) {
                frameRateHintRequests.remove(window)
            }
            window.applyFrameRateHint(combineFrameRateHints(requests))
        }
    }
}

@Composable
internal actual fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit) {
    val context = LocalContext.current
    val currentOnMaximumDisplayRefreshRateChanged by rememberUpdatedState(onMaximumDisplayRefreshRateChanged)
    DisposableEffect(context) {
        fun update() = currentOnMaximumDisplayRefreshRateChanged(context.findDisplay()?.maximumRefreshRate())
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = update()
            override fun onDisplayRemoved(displayId: Int) = update()
            override fun onDisplayChanged(displayId: Int) = update()
        }
        displayManager?.registerDisplayListener(displayListener, null)
        update()
        onDispose { displayManager?.unregisterDisplayListener(displayListener) }
    }
}

/**
 * The panel's ceiling at the resolution it is running at: switching resolution can unlock rates the
 * current one doesn't offer, and those are not rates the game can actually be shown at.
 */
private fun Display.maximumRefreshRate() = mode?.let { currentMode ->
    supportedModes.filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
        .maxOfOrNull { it.refreshRate }
}

/**
 * Some panels ignore the [preferredRefreshRate] hint and stay in their highest refresh mode
 * (observed on HyperOS), so a [TargetFrameRate.Limit] preferably names one of the panel's own display
 * modes, which the system honors as a hard request. The float hint remains the fallback when the
 * supported modes are unknown. Both fields are always written so that changing the target releases
 * whichever lever the previous one used.
 */
@Suppress("DEPRECATION") // preferredRefreshRate is the refresh-rate lever reachable from a Window across minSdk 29+.
private fun Window.applyFrameRateHint(targetFrameRate: TargetFrameRate) {
    val targetMode = (targetFrameRate as? TargetFrameRate.Limit)?.let { findDisplayMode(it) }
    attributes = attributes.apply {
        preferredDisplayModeId = targetMode?.modeId ?: SYSTEM_DEFAULT_DISPLAY_MODE
        preferredRefreshRate = if (targetMode == null) targetFrameRate.toPreferredRefreshRate() else SYSTEM_DEFAULT_REFRESH_RATE
    }
}

private fun Window.findDisplayMode(targetFrameRate: TargetFrameRate.Limit): Display.Mode? {
    val display = context.findDisplay()
    val currentMode = display?.mode ?: return null
    val candidates = display.supportedModes.filter {
        it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
    }
    // The game loop can throttle a display frame away but never conjure one, so a panel slower than the
    // target caps the achieved rate at the panel's own: ask for the slowest mode that still covers the
    // target - an exact match where the panel has one, the next one up where it doesn't (a 90 fps target
    // on a panel whose modes step 120, 80, 60 runs at 120 and ticks three frames out of four, rather than
    // settling at 80). The fastest mode is the best available when even that falls short of the target.
    return candidates.filter { it.refreshRate.roundToInt() >= targetFrameRate.framesPerSecond }.minByOrNull { it.refreshRate }
        ?: candidates.maxByOrNull { it.refreshRate }
}

private fun TargetFrameRate.toPreferredRefreshRate() = when (this) {
    // DisplayDivider derives its rate by counting real vsync pulses, so it must run at the panel's
    // native rate; hinting the panel down would make the divider count an already-reduced rate and
    // drop the result twice over. DisplayDefault simply wants the maximum. Both therefore release the
    // hint — only an absolute Limit names a rate the panel can settle on directly.
    TargetFrameRate.DisplayDefault, is TargetFrameRate.DisplayDivider -> SYSTEM_DEFAULT_REFRESH_RATE
    is TargetFrameRate.Limit -> framesPerSecond.toFloat()
}

private fun Context.findDisplay(): Display? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            display
        } catch (_: UnsupportedOperationException) {
            // A context not associated with a display (a Service, for example) is drawn on the default one.
            (getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
        }
    } else {
        @Suppress("DEPRECATION") findActivity()?.windowManager?.defaultDisplay
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 0 lets the system pick, releasing any earlier hint so the panel returns to its default. */
private const val SYSTEM_DEFAULT_REFRESH_RATE = 0f
private const val SYSTEM_DEFAULT_DISPLAY_MODE = 0
