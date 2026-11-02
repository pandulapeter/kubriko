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

import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Catches what a coroutine of Kubriko Creator throws and nothing caught (an unexpected exception from JGit, Gradle, or a
 * project's class), so that it is printed and [report]ed instead of being lost or ending the app, and the rest keeps working.
 */
internal fun unexpectedProblemHandler(report: (Throwable) -> Unit) = CoroutineExceptionHandler { _, exception ->
    exception.printStackTrace()
    report(exception)
}
