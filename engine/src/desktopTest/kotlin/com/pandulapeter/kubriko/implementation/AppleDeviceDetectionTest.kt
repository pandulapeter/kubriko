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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppleDeviceDetectionTest {

    @Test
    fun firefoxOnAndroidIsNotAnAppleDevice() {
        assertNull(classifyAppleDevice(FIREFOX_ANDROID, maxTouchPoints = 5, width = 412, height = 915))
        assertNull(classifyAppleDevice(FIREFOX_ANDROID, maxTouchPoints = 5, width = 915, height = 412))
    }

    @Test
    fun appleUserAgentsAreRecognized() {
        assertEquals(AppleDevice.Iphone, classifyAppleDevice(IPHONE_SAFARI, maxTouchPoints = 5, width = 393, height = 852))
        assertEquals(AppleDevice.Ipad, classifyAppleDevice(IPAD_SAFARI, maxTouchPoints = 5, width = 820, height = 1180))
    }

    @Test
    fun touchScreensWithAMacUserAgentAreJudgedByTheirAspectRatio() {
        assertEquals(AppleDevice.Ipad, classifyAppleDevice(MAC_SAFARI, maxTouchPoints = 5, width = 1180, height = 820))
        assertEquals(AppleDevice.Iphone, classifyAppleDevice(MAC_SAFARI, maxTouchPoints = 5, width = 852, height = 393))
        assertEquals(AppleDevice.Iphone, classifyAppleDevice(MAC_SAFARI, maxTouchPoints = 5, width = 1700, height = 1000))
    }

    @Test
    fun otherBrowsersAreNotAppleDevices() {
        assertNull(classifyAppleDevice(MAC_SAFARI, maxTouchPoints = 0, width = 1700, height = 1000))
        assertNull(classifyAppleDevice(WINDOWS_FIREFOX, maxTouchPoints = 10, width = 1280, height = 800))
        assertNull(classifyAppleDevice(MAC_CHROME, maxTouchPoints = 5, width = 1180, height = 820))
    }

    private companion object {
        const val FIREFOX_ANDROID = "Mozilla/5.0 (Android 14; Mobile; rv:131.0) Gecko/131.0 Firefox/131.0"
        const val IPHONE_SAFARI =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
        const val IPAD_SAFARI =
            "Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
        const val MAC_SAFARI =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
        const val WINDOWS_FIREFOX = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0"
        const val MAC_CHROME =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
    }
}
