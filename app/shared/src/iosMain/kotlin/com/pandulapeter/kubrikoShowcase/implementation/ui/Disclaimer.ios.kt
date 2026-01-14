/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.welcome_disclaimer_obfuscation
import kotlin.experimental.ExperimentalNativeApi

internal actual fun getWarningTexts() = buildList {
    if (isUnoptimized()) {
        add(Res.string.welcome_disclaimer_obfuscation)
    }
}

@OptIn(ExperimentalNativeApi::class)
private fun isUnoptimized() = Platform.isDebugBinary