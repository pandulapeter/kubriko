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

import com.pandulapeter.kubriko.shared.utilities.getFixedUri

internal actual fun getResourceUri(path: String, webRootPathName: String): String = getFixedUri(
    path = "composeResources/kubriko.examples.test_audio.generated.resources/" + path,
    rootPathName = webRootPathName,
)