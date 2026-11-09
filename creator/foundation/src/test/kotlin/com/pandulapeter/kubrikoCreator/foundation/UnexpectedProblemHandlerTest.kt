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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals

class UnexpectedProblemHandlerTest {
    private val reported = mutableListOf<Throwable>()
    private val scope = CoroutineScope(Job() + unexpectedProblemHandler { reported += it })

    @Test
    fun aProblemIsReportedThroughTheScopesHandler() {
        val problem = IllegalStateException("Broken.")
        scope.reportProblem(problem)
        assertEquals(listOf<Throwable>(problem), reported)
    }

    @Test
    fun aCancellationIsNoProblemAndIsntThrown() {
        scope.reportProblem(CancellationException("A read that timed out."))
        scope.cancel()
        scope.reportProblem(CancellationException("Closing."))
        assertEquals(emptyList(), reported)
    }

    @Test
    fun aScopeWithoutAHandlerOrAHandlerThatThrowsLeavesOnlyTheErrorStream() {
        CoroutineScope(Job()).reportProblem(IllegalStateException("Broken."))
        CoroutineScope(Job() + CoroutineExceptionHandler { _, _ -> error("Not reported.") }).reportProblem(IllegalStateException("Broken."))
    }
}
