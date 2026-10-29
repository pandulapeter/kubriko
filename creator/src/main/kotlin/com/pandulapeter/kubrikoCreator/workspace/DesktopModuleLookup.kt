/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicReference

/**
 * The Gradle path of the project's desktop module, asked from Gradle in the background as soon as the workspace opens,
 * and asked again after a lookup that failed (a build file fixed since, or Gradle's download back online).
 */
internal class DesktopModuleLookup(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val findDesktopModulePath: () -> String,
) {
    private val lookup = AtomicReference(start())

    private fun start() = scope.async(ioDispatcher) { findDesktopModulePath() }

    suspend fun await(): String {
        val current = lookup.get()
        return try {
            current.await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            lookup.compareAndSet(current, start())
            throw exception
        }
    }
}
