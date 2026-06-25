/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

/** Polls [currentSystemTheme] every 100 ms because Compose does not receive system-theme change notifications on JVM. */
@Composable
internal actual fun dynamicIsSystemInDarkTheme() = produceState(initialValue = currentSystemTheme == SystemTheme.DARK) {
    while (isActive) {
        delay(100)
        value = currentSystemTheme == SystemTheme.DARK
    }
}.value
