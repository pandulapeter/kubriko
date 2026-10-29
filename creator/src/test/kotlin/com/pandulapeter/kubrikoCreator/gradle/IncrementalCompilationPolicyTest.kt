/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class IncrementalCompilationPolicyTest {
    private var hasExpectDeclarations = false
    private val policy = IncrementalCompilationPolicy { hasExpectDeclarations }
    private val modes = mutableListOf<CompilationMode>()
    private val incremental = CompilationMode(isCommonCodeIncremental = true, isClean = false)
    private val clean = CompilationMode(isCommonCodeIncremental = true, isClean = true)

    private suspend fun build(isSuccessful: Boolean = true) = policy.build { mode ->
        modes += mode
        isSuccessful
    }

    @Test
    fun commonCodeCompilesIncrementally() = runTest {
        build()
        build()
        assertEquals(listOf(incremental, incremental), modes)
    }

    @Test
    fun theBuildAfterAFailedOneIsCleanOnce() = runTest {
        assertFalse(build(isSuccessful = false))
        build()
        build()
        assertEquals(listOf(incremental, clean, incremental), modes)
    }

    @Test
    fun aRequestedCleanBuildIsTheNextOneOnly() = runTest {
        policy.requestCleanBuild()
        policy.requestCleanBuild()
        build()
        build()
        assertEquals(listOf(clean, incremental), modes)
    }

    @Test
    fun aProjectDeclaringExpectsNeverCompilesCommonCodeIncrementally() = runTest {
        hasExpectDeclarations = true
        build()
        policy.requestCleanBuild()
        build()
        assertEquals(listOf(CompilationMode(isCommonCodeIncremental = false, isClean = false), CompilationMode(isCommonCodeIncremental = false, isClean = true)), modes)
    }

    @Test
    fun aCancelledCleanBuildIsMadeUpForButACancelledIncrementalOneIsnt() = runTest {
        assertFailsWith<CancellationException> { policy.build { throw CancellationException() } }
        build()
        policy.requestCleanBuild()
        assertFailsWith<CancellationException> { policy.build { throw CancellationException() } }
        build()
        build()
        assertEquals(listOf(incremental, clean, incremental), modes)
    }
}
