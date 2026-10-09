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

internal enum class AppleDevice {
    Iphone,
    Ipad,
}

/**
 * Tells iPhones and iPads apart from a browser's user agent, falling back to a touch screen with a macOS user agent
 * (iPadOS Safari and desktop-mode Safari send one) and judging that by the window's aspect ratio. Null for anything
 * else.
 */
internal fun classifyAppleDevice(
    userAgent: String,
    maxTouchPoints: Int,
    width: Int,
    height: Int,
): AppleDevice? = when {
    userAgent.contains("iPhone") -> AppleDevice.Iphone
    userAgent.contains("iPad") -> AppleDevice.Ipad
    userAgent.contains("Macintosh") && !userAgent.contains("Chrome") && maxTouchPoints > 0 ->
        if (width.toFloat() / height.coerceAtLeast(1) > WIDE_WINDOW_ASPECT_RATIO) AppleDevice.Iphone else AppleDevice.Ipad

    else -> null
}

private const val WIDE_WINDOW_ASPECT_RATIO = 1.6f
