/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testAudio.implementation.utilities

import kotlinx.browser.window

internal actual fun getResourceUri(path: String): String = getUri("composeResources/kubriko.examples.test_audio.generated.resources/" + path)

private fun getUri(path: String): String {
    // The change in this line is the only reason for this entire mess. Web navigation breaks URI handling. windowPathname has to be empty.
    return getResourceUrl(window.location.origin, "", path)
}

private fun getResourceUrl(windowOrigin: String, windowPathname: String, resourcePath: String): String {
    val path = "/./$resourcePath"
    return when {
        path.startsWith("/") -> windowOrigin + path
        path.startsWith("http://") || path.startsWith("https://") -> path
        else -> windowOrigin + windowPathname + path
    }
}