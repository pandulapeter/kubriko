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

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AiStepNumberingTest {
    private val root = testDirectory()

    /**
     * A session of Kubriko Creator on the project: its executor, numbered as the workspace numbers it.
     */
    private fun TestScope.session(): CommandExecutor = UnconfinedTestDispatcher(testScheduler).let { dispatcher ->
        val scope = CoroutineScope(backgroundScope.coroutineContext + dispatcher)
        CommandExecutor(commandContext(rootDirectory = root), scope).also { AiStepNumbering(root, it, scope, dispatcher) }
    }

    private suspend fun CommandExecutor.step() = call("get_project_digest", JsonObject(emptyMap()))

    @Test
    fun stepsGoOnFromWhereTheProjectsLastSessionLeftOff() = runTest {
        val first = session()
        first.step()
        first.step()
        assertEquals(listOf(0, 1), first.aiSteps.value.map { it.id })
        val second = session()
        second.step()
        assertEquals(listOf(2), second.aiSteps.value.map { it.id })
    }

    @Test
    fun aNumberThatCantBeReadStartsFromTheBeginning() = runTest {
        File(root, "${LocalProjectStore.FOLDER}/ai-steps.json").apply { parentFile.mkdirs() }.writeText("{not json")
        val session = session()
        session.step()
        assertEquals(listOf(0), session.aiSteps.value.map { it.id })
    }

    @Test
    fun theNumberingIsNeverLowered() = runTest {
        val session = session()
        session.step()
        session.continueAiStepsFrom(0)
        session.step()
        assertEquals(listOf(0, 1), session.aiSteps.value.map { it.id })
    }
}
