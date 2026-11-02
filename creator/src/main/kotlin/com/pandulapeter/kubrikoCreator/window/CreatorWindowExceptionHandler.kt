/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.window

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DefaultWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import java.awt.Window

/**
 * Reports what the window's UI threw and nothing caught ([report]) instead of closing the window, and asks for its content
 * to be built again ([onRecover]). Every failure is counted first, so failures that keep coming ([RepeatedFailureGuard]),
 * a [report] that itself makes the UI throw too, fall back to Compose's own handler rather than loop.
 *
 * Giving up loses no work: Compose's handler shows the error and then sends the window `WINDOW_CLOSING`, which quits
 * through `AppGraph.shutdown` as a click on the close button does, so [fallback] must stay that handler, never a
 * `dispose()` or an exit of its own. Runs on the AWT event thread, so [report] and [onRecover] only update state, and
 * never throw.
 */
internal class CreatorWindowExceptionHandler(
    private val report: (Throwable) -> Unit,
    private val onRecover: () -> Unit,
    private val guard: RepeatedFailureGuard = RepeatedFailureGuard(),
    private val fallback: (Window?, Throwable) -> Unit = ::showAndClose,
) {
    /**
     * What `LocalWindowExceptionHandlerFactory` provides to the window.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    val factory = WindowExceptionHandlerFactory { window -> WindowExceptionHandler { throwable -> handle(window, throwable) } }

    /**
     * Handles [throwable] thrown in [window], which is null only in tests.
     */
    fun handle(window: Window?, throwable: Throwable) {
        throwable.printStackTrace()
        if (guard.shouldGiveUp()) {
            fallback(window, throwable)
        } else {
            report(throwable)
            onRecover()
        }
    }

    private companion object {
        @OptIn(ExperimentalComposeUiApi::class)
        fun showAndClose(window: Window?, throwable: Throwable) {
            if (window == null) throw throwable
            DefaultWindowExceptionHandlerFactory.exceptionHandler(window).onException(throwable)
        }
    }
}
