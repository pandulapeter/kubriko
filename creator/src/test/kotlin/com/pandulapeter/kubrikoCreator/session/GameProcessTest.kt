/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameProcessTest {
    private val root = createTempDirectory().toFile()
    private val project = File(root, "game").apply { mkdirs() }
    private val snapshots = File(root, "snapshots")
    private val process = GameProcess(project, snapshots)
    private val launch = GameLaunch(
        java = File(System.getProperty("java.home"), "bin/java"),
        mainClass = GameProcessTestGame::class.java.name,
        workingDirectory = project,
        jvmArguments = emptyList(),
        classpath = System.getProperty("java.class.path").split(File.pathSeparator).map(::File),
    )

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun aGameThatEndsOnItsOwnIsSuccessfulAndItsCopyIsDeleted() = runBlocking {
        val exit = process.run(launch, emptyMap())
        assertEquals(0, exit.exitCode)
        assertTrue("Played." in exit.output)
        assertEquals(emptyList(), snapshots.listFiles().orEmpty().toList())
    }

    @Test
    fun aGameThatFailsReportsItsExitCodeAndItsLastLines() = runBlocking {
        val exit = process.run(launch, mapOf(GameProcessTestGame.MODE_PROPERTY to "fail"))
        assertEquals(3, exit.exitCode)
        assertEquals((11..40).map { "Line $it" }, exit.output)
    }

    @Test
    fun cancellingStopsTheGame() = runBlocking {
        val game = async(Dispatchers.Default) { process.run(launch, mapOf(GameProcessTestGame.MODE_PROPERTY to "sleep")) }
        delay(1_000)
        withTimeout(10_000) { game.cancelAndJoin() }
        assertTrue(game.isCancelled)
        assertEquals(emptyList(), snapshots.listFiles().orEmpty().toList())
    }

    @Test
    fun theProjectsClassFilesAreCopiedAndEverythingElseIsUsedWhereItIs() {
        val classes = File(project, "build/classes").apply { mkdirs() }
        File(classes, "game/Ball.class").apply { parentFile.mkdirs() }.writeText("ball")
        File(classes, "files/scenes/main.json").apply { parentFile.mkdirs() }.writeText("[]")
        val ownJar = File(project, "build/libs/game.jar").apply { parentFile.mkdirs() }.apply { writeText("jar") }
        val library = File(root, "library.jar").apply { writeText("library") }
        val snapshot = File(root, "snapshot")
        val classpath = process.snapshotOf(listOf(classes, ownJar, library, File(project, "missing")), snapshot)
        assertEquals(listOf(File(snapshot, "0"), classes, File(snapshot, "1/game.jar"), library, File(project, "missing")), classpath)
        assertEquals("ball", File(snapshot, "0/game/Ball.class").readText())
        assertTrue(!File(snapshot, "0/files").exists())
        assertEquals("jar", File(snapshot, "1/game.jar").readText())
    }
}
