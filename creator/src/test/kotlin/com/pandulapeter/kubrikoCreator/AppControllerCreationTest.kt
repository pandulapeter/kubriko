/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import com.pandulapeter.kubrikoCreator.recent.RecentProjectsStore
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.template.ProjectCreator
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Creating a project on real threads, since cancelling one interrupts the thread it is created on.
 */
class AppControllerCreationTest {
    private val parentScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val parentDirectory = testDirectory()
    private val form = NewProjectForm(parentDirectory = parentDirectory).withName("My Game")
    private val directory = File(parentDirectory, "my-game")

    private fun controller(creator: ProjectCreator) = AppController(
        parentScope = parentScope,
        workspaceOpener = { _, _ -> error("Nothing is opened.") },
        projectCreator = creator,
        ioDispatcher = Dispatchers.IO,
        recentProjectsStore = RecentProjectsStore(File(parentDirectory, "recent.json")),
    )

    @AfterTest
    fun tearDown() {
        parentScope.cancel()
    }

    @Test
    fun cancellingACreationComesBackToTheFormAndInterruptsTheCreation() = blockingTest {
        val started = CountDownLatch(1)
        val interrupted = CompletableDeferred<Unit>()
        val controller = controller { _ ->
            started.countDown()
            try {
                Thread.sleep(Long.MAX_VALUE)
                error("Not interrupted.")
            } catch (exception: InterruptedException) {
                interrupted.complete(Unit)
                throw exception
            }
        }
        controller.createProject(form)
        started.await()
        controller.cancelCreatingProject()
        assertEquals(AppScreen.NewProject(form), controller.screen.value)
        interrupted.await()
    }

    @Test
    fun theNextCreationWaitsUntilTheCancelledOneCleanedUp() = blockingTest {
        val events = Collections.synchronizedList(mutableListOf<String>())
        val firstStarted = CountDownLatch(1)
        val cleanUp = CountDownLatch(1)
        val secondStarted = CompletableDeferred<Unit>()
        var calls = 0
        val controller = controller { _ ->
            if (++calls == 1) {
                firstStarted.countDown()
                try {
                    Thread.sleep(Long.MAX_VALUE)
                } catch (exception: InterruptedException) {
                    cleanUp.await()
                    events += "first cleaned up"
                    throw exception
                }
            }
            events += "second started"
            secondStarted.complete(Unit)
            throw InterruptedException()
        }
        controller.createProject(form)
        firstStarted.await()
        controller.cancelCreatingProject()
        controller.createProject(form)
        cleanUp.countDown()
        secondStarted.await()
        assertEquals(listOf("first cleaned up", "second started"), events.toList())
    }

    @Test
    fun aProjectFinishedJustAsItWasCancelledIsRemoved() = blockingTest {
        lateinit var controller: AppController
        controller = controller { request ->
            request.directory.mkdirs()
            File(request.directory, "settings.gradle.kts").writeText("")
            controller.cancelCreatingProject()
            request.directory
        }
        controller.createProject(form)
        controller.closeProject()?.join()
        assertFalse(directory.exists())
    }
}
