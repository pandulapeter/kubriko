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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuildAlongsideTest {

    @Test
    fun aDeviceMissingAtOnceStopsTheBuildThatBegan() = runTest {
        val events = mutableListOf<String>()
        val result = builtAlongside(
            build = {
                events += "build started"
                try {
                    awaitCancellation()
                } finally {
                    events += "build stopped"
                }
            },
            ready = { null as String? },
        )
        assertNull(result)
        assertEquals(listOf("build started", "build stopped"), events)
    }

    @Test
    fun theDeviceIsReturnedOnceTheBuildIsDone() = runTest {
        val built = CompletableDeferred<Unit>()
        assertEquals("phone", builtAlongside(build = { built.complete(Unit) }, ready = { "phone" }))
        assertTrue(built.isCompleted)
    }

    @Test
    fun aFailedBuildStopsGettingTheDeviceReady() = runTest {
        var isStopped = false
        val result = builtAlongside(
            build = {
                yield()
                false
            },
            ready = {
                try {
                    awaitCancellation()
                } finally {
                    isStopped = true
                }
            },
        )
        assertNull(result)
        assertTrue(isStopped)
    }
}
