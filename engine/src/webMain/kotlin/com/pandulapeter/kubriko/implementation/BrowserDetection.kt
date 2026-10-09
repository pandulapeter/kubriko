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

import org.w3c.dom.Window

/** Whether the browser runs on an Android device, judged by its user agent. */
fun Window.isRunningOnAndroid() =
    navigator.userAgent.contains("Android")

/**
 * Whether the browser runs on an iPhone: its user agent says so, or it is a touch screen with a macOS user agent
 * (desktop-mode Safari) and a wide (phone-in-landscape) window.
 */
fun Window.isRunningOnIphone() =
    classifyAppleDevice() == AppleDevice.Iphone

/**
 * Whether the browser runs on an iPad: its user agent says so, or it is a touch screen with a macOS user agent
 * (iPadOS Safari and desktop-mode Safari) and a window that is not that wide.
 */
fun Window.isRunningOnIpad() =
    classifyAppleDevice() == AppleDevice.Ipad

private fun Window.classifyAppleDevice() = classifyAppleDevice(
    userAgent = navigator.userAgent,
    maxTouchPoints = navigator.maxTouchPoints,
    width = innerWidth,
    height = innerHeight,
)
