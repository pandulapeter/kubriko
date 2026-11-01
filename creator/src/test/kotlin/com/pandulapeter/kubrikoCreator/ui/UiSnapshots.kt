/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorTheme
import com.pandulapeter.kubrikoCreator.window.ProvideInterfaceScale
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

/**
 * Renders Kubriko Creator's UI offscreen for tests, without a window, so that what the app would show is checked (and
 * saved as an image under `build/ui-snapshots` to look at) without taking over the screen.
 */
@OptIn(ExperimentalTestApi::class)
internal fun uiTest(width: Int = PANEL_WIDTH, height: Int = PANEL_HEIGHT, block: ComposeUiTest.() -> Unit) = runDesktopComposeUiTest(width, height) { block() }

/**
 * Shows [content] as the app does (its interface scale and theme, inside a [Surface]), on a panel's background.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showing(content: @Composable () -> Unit) = setContent {
    ProvideInterfaceScale {
        CreatorTheme {
            Surface {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                    content()
                }
            }
        }
    }
}

/**
 * Saves what is shown now as `build/ui-snapshots/<name>.png`.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.saveSnapshot(name: String) {
    val bitmap = onRoot().captureToImage().asSkiaBitmap()
    val png = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG) ?: return
    File("build/ui-snapshots").apply { mkdirs() }.resolve("$name.png").writeBytes(png.bytes)
}

private const val PANEL_WIDTH = 360
private const val PANEL_HEIGHT = 640
