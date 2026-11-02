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

import com.pandulapeter.kubrikoCreator.session.GameLaunchPreparation
import com.pandulapeter.kubrikoCreator.session.JavaArgumentFile
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.testing.FakeSessionChannel
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class GameBuildsTest {
    private val rootDirectory = testDirectory()
    private val playClasspathFile = SessionInitScript.playClasspathFileOf(rootDirectory)
    private val console = ConsoleLog()
    private val tasks = mutableListOf<Pair<String, Map<String, String>>>()

    /**
     * What the classpath build writes into [playClasspathFile], or null for a build that fails.
     */
    private var classpath: String? = "kubrikoVersion=1.0.0\n"

    private fun TestScope.createGameBuilds(): GameBuilds {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(backgroundScope.coroutineContext + dispatcher)
        val runTask: suspend (String, Map<String, String>) -> Boolean = { taskName, properties ->
            tasks += taskName to properties
            classpath?.let(playClasspathFile::writeText) != null
        }
        return GameBuilds(
            rootDirectory = rootDirectory,
            sessions = SessionController(FakeSessionChannel(), runTask, console, scope),
            runTask = runTask,
            console = console,
            scope = scope,
            ioDispatcher = dispatcher,
            currentTimeMillis = { 0L },
        )
    }

    @AfterTest
    fun deletePlayClasspathFile() {
        playClasspathFile.delete()
    }

    @Test
    fun aFailedClasspathBuildMeansFailed() = runTest {
        classpath = null
        assertEquals(GameLaunchPreparation.Failed, createGameBuilds().prepareLaunch())
    }

    @Test
    fun aClasspathFileWithoutALaunchMeansUnavailable() = runTest {
        classpath = "kubrikoVersion=1.0.0\n${File(rootDirectory, "build/classes").path}\n"
        assertEquals(GameLaunchPreparation.Unavailable, createGameBuilds().prepareLaunch())
    }

    @Test
    fun theRunTaskGetsTheClasspathAndHotReloadProperties() = runTest {
        createGameBuilds().prepareLaunch()
        assertEquals(
            listOf(
                SessionInitScript.CLASSPATH_TASK_NAME to mapOf(
                    "kubrikoCreatorMode" to "classpath",
                    "kubrikoCreatorClasspathFile" to playClasspathFile.absolutePath,
                    "kubrikoCreatorHotReload" to "true",
                ),
            ),
            tasks,
        )
    }

    @Test
    fun onlyClassFoldersInsideTheProjectAreRead() = runTest {
        val ownClasses = File(rootDirectory, "build/classes").apply { mkdirs() }
        val otherClasses = File(testDirectory(), "classes").apply { mkdirs() }
        File(otherClasses, "Library.class").writeText("Not a class file.")
        val argumentFile = File(rootDirectory, "build/run/game.argfile").apply { parentFile.mkdirs() }
        argumentFile.writeText(JavaArgumentFile.format(listOf("-cp", File(rootDirectory, "build/run/classes").path, "game.MainKt")))
        classpath = "kubrikoVersion=1.0.0\n${ownClasses.path}\n${otherClasses.path}\nlaunch.java=/jdk/bin/java\nlaunch.mainClass=game.MainKt\n" +
            "launch.workingDir=${rootDirectory.path}\nlaunch.classpath=${ownClasses.path}\nlaunch.hotJava=/jbr/bin/java\n" +
            "launch.hotArgumentFile=${argumentFile.path}\nlaunch.hotReloadTask=hotReloadDesktopMain\nlaunch.hotLibrary=/libs/session.jar\n"
        val gameBuilds = createGameBuilds()
        val preparation = assertIs<GameLaunchPreparation.Ready>(gameBuilds.prepareLaunch(), "the folder outside the project isn't read")
        assertEquals("hotReloadDesktopMain", preparation.launch.reloadTaskName)
        File(ownClasses, "Ball.class").writeText("Not a class file.")
        assertFailsWith<IOException>("the project's own folder is read") { gameBuilds.prepareLaunch() }
    }
}
