/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.seconds

class AssistantSettingsStateTest {
    private val settingsDirectory = createTempDirectory().toFile()
    private val ioThread = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob())

    @AfterTest
    fun tearDown() {
        scope.cancel()
        ioThread.shutdownNow()
        settingsDirectory.deleteRecursively()
    }

    @Test
    fun savesHappenOffTheCallingThreadAndTheLastValueWins() = runBlocking {
        withTimeout(10.seconds) {
            val file = File(settingsDirectory, "assistant.json")
            val ioIsBusy = CountDownLatch(1)
            ioThread.execute { ioIsBusy.await() }
            val state = AssistantSettingsState(scope, AssistantSettingsStore(file), ioThread.asCoroutineDispatcher())
            state.update { it.copy(agentId = "first") }
            state.update { it.copy(agentId = "second") }
            assertEquals("second", state.value.value.agentId)
            assertFalse(file.exists())
            ioIsBusy.countDown()
            while (AssistantSettingsStore(file).load().agentId != "second") delay(10)
            scope.coroutineContext.job.children.forEach { it.join() }
            assertEquals("second", AssistantSettingsStore(file).load().agentId)
        }
    }
}
