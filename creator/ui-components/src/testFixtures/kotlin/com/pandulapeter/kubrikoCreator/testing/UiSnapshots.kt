/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

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
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorTheme
import com.pandulapeter.kubrikoCreator.ui.theme.ProvideInterfaceScale
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

/**
 * Renders Kubriko Creator's UI offscreen for tests, without a window, so that what the app would show is checked (and
 * saved as an image under `build/ui-snapshots` to look at) without taking over the screen.
 */
@OptIn(ExperimentalTestApi::class)
public fun uiTest(width: Int = PANEL_WIDTH, height: Int = PANEL_HEIGHT, block: ComposeUiTest.() -> Unit): Unit = runDesktopComposeUiTest(width, height) { block() }

/**
 * Shows [content] as the app does (its interface scale and theme, inside a [Surface]), on a panel's background.
 */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.showing(content: @Composable () -> Unit): Unit = setContent {
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
 * Saves what is shown now as `build/ui-snapshots/<name>.png`, and an open menu or popup, which is a root of its own, as
 * `<name>-popup.png` (numbered after the first).
 */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.saveSnapshot(name: String) {
    val roots = onAllNodes(isRoot())
    roots.fetchSemanticsNodes().indices.forEach { index ->
        val png = Image.makeFromBitmap(roots[index].captureToImage().asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG) ?: return@forEach
        val suffix = when (index) {
            0 -> ""
            1 -> "-popup"
            else -> "-popup$index"
        }
        File("build/ui-snapshots").apply { mkdirs() }.resolve("$name$suffix.png").writeBytes(png.bytes)
    }
}

/**
 * The width [uiTest] renders at unless told otherwise, about a side panel's.
 */
public const val PANEL_WIDTH: Int = 360

/**
 * The height [uiTest] renders at unless told otherwise.
 */
public const val PANEL_HEIGHT: Int = 640
