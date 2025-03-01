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

private fun getUri(path: String) = "${window.location.origin}${resolvePathName()}/./$path"

private fun resolvePathName() = window.location.pathname.split("/").filter { it.isNotBlank() }.let { sections ->
    if (sections.size <= 1) "" else "/${sections.first()}"
}