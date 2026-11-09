/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

/**
 * Catches what a coroutine of Kubriko Creator throws and nothing caught (an unexpected exception from JGit, Gradle, or a
 * project's class), so that it is printed and [report]ed instead of being lost or ending the app, and the rest keeps working.
 */
public fun unexpectedProblemHandler(report: (Throwable) -> Unit): CoroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
    exception.printStackTrace()
    report(exception)
}

/**
 * Reports [problem], caught where it can't go on, through this scope's handler ([unexpectedProblemHandler]), so the rest
 * keeps working; it never throws. A cancellation is no problem and is left out: whoever was cancelled stops by checking
 * that itself. Without a handler, or when it throws, only the error stream is left, with [problem] as suppressed.
 */
public fun CoroutineScope.reportProblem(problem: Throwable) {
    if (problem is CancellationException) return
    val handler = coroutineContext[CoroutineExceptionHandler] ?: return problem.printStackTrace()
    try {
        handler.handleException(coroutineContext, problem)
    } catch (handlerFailure: Throwable) {
        handlerFailure.addSuppressed(problem)
        handlerFailure.printStackTrace()
    }
}
