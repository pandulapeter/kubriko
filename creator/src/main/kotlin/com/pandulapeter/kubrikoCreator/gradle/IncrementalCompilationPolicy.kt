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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decides how each build compiles ([CompilationMode]). Common code compiles incrementally, which Kotlin 2.4 only does
 * with an internal property (KT-62686): without it, every change to `commonMain`, where generated code lives, compiles
 * everything again. Kotlin calls that property unsafe, so it is left off for projects that declare an `expect` (the
 * case it is suspected for), and the build after a failed one or after [requestCleanBuild] compiles everything from
 * scratch, in case the classes no longer match the sources. Remove this with Kotlin 2.5's separate compilation of
 * common code.
 */
internal class IncrementalCompilationPolicy(
    private val hasExpectDeclarations: suspend () -> Boolean,
) {
    private val isCleanBuildRequested = AtomicBoolean(false)

    /**
     * Makes the next build compile everything from scratch.
     */
    fun requestCleanBuild() = isCleanBuildRequested.set(true)

    /**
     * Runs [build] in the mode it should compile in, and returns whether it succeeded. A failed build requests a clean
     * one, and so does a cancelled clean one, which didn't happen.
     */
    suspend fun build(build: suspend (CompilationMode) -> Boolean): Boolean {
        val mode = CompilationMode(isCommonCodeIncremental = !hasExpectDeclarations(), isClean = isCleanBuildRequested.getAndSet(false))
        val isSuccessful = try {
            build(mode)
        } catch (exception: CancellationException) {
            if (mode.isClean) requestCleanBuild()
            throw exception
        }
        if (!isSuccessful) requestCleanBuild()
        return isSuccessful
    }
}
