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

import com.pandulapeter.kubrikoCreator.session.ClassShape
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_game_reload_failed
import kubriko.creator.generated.resources.status_game_reloaded
import kubriko.creator.generated.resources.status_game_restarting
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameReloaderTest {
    private val ball = ClassShape(superName = "java/lang/Object", interfaces = emptyList(), instanceFields = setOf("color:J"), stateHash = null)
    private val ballWithLabel = ball.copy(instanceFields = ball.instanceFields + "label:Ljava/lang/String;")
    private val folders = listOf(File("/game/build/classes"))
    private val console = ConsoleLog()
    private var builtShapes = mapOf("game/Ball" to ball)
    private var buildResult: () -> List<File>? = { folders }
    private var isGameRunning = true
    private val reloads = mutableListOf<String>()
    private var reloadResult: suspend () -> Boolean = { true }
    private var builds = 0
    private var restarts = 0

    private fun TestScope.createReloader() = GameReloader(
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        buildClasses = {
            builds++
            buildResult()
        },
        readShapes = { assertEquals(folders, it); builtShapes },
        isGameRunning = { isGameRunning },
        reload = {
            reloads += it
            reloadResult()
        },
        restartGame = { restarts++ },
        console = console,
        currentTimeMillis = { 0L },
    )

    private val statuses get() = console.entries.value.filterIsInstance<ConsoleEntry.Status>()

    @Test
    fun aChangeToCodeOnlyIsReloadedIntoTheRunningGame() = runTest {
        val reloader = createReloader()
        reloader.onGameStarting("hotReloadDesktopMain", mapOf("game/Ball" to ball))
        reloader.onCodeChanged()
        assertEquals(listOf("hotReloadDesktopMain"), reloads)
        assertEquals(0, restarts)
        assertEquals(ConsoleEntry.Status(Res.string.status_game_reloaded, arguments = listOf("0.0")), statuses.last())
    }

    @Test
    fun aClassWhoseShapeChangedRestartsTheGameNamingItsType() = runTest {
        val reloader = createReloader()
        reloader.onGameStarting("hotReloadDesktopMain", mapOf("game/Ball" to ball, "game/Ball\$State" to ball))
        builtShapes = mapOf("game/Ball" to ballWithLabel, "game/Ball\$State" to ballWithLabel, "game/Star" to ballWithLabel)
        reloader.onCodeChanged()
        assertTrue(reloads.isEmpty())
        assertEquals(1, restarts)
        assertEquals(ConsoleEntry.Status(Res.string.status_game_restarting, arguments = listOf("Ball")), statuses.last())
    }

    @Test
    fun theShapesOfReloadedClassesAreWhatTheNextChangeIsComparedWith() = runTest {
        val reloader = createReloader()
        reloader.onGameStarting("hotReloadDesktopMain", emptyMap())
        builtShapes = mapOf("game/Star" to ball)
        reloader.onCodeChanged()
        builtShapes = mapOf("game/Star" to ballWithLabel)
        reloader.onCodeChanged()
        assertEquals(1, reloads.size)
        assertEquals(1, restarts, "Star was new to the game with the first reload, and changed shape with the second")
    }

    @Test
    fun nothingIsBuiltForAGameStartedWithoutHotReloadOrForNoGame() = runTest {
        val reloader = createReloader()
        reloader.onCodeChanged()
        reloader.onGameStarting(null, emptyMap())
        reloader.onCodeChanged()
        reloader.onGameStarting("hotReloadDesktopMain", emptyMap())
        isGameRunning = false
        reloader.onCodeChanged()
        assertEquals(0, builds)
    }

    @Test
    fun aFailedBuildLeavesTheGameAsItIs() = runTest {
        val reloader = createReloader()
        reloader.onGameStarting("hotReloadDesktopMain", emptyMap())
        buildResult = { null }
        reloader.onCodeChanged()
        assertEquals(1, builds)
        assertTrue(reloads.isEmpty())
        assertEquals(0, restarts)
    }

    @Test
    fun aFailedReloadRestartsTheGame() = runTest {
        val reloader = createReloader()
        reloader.onGameStarting("hotReloadDesktopMain", emptyMap())
        reloadResult = { false }
        reloader.onCodeChanged()
        assertEquals(1, restarts)
        assertEquals(ConsoleEntry.Status(Res.string.status_game_reload_failed, isError = true), statuses.last())
    }

    @Test
    fun changesMadeWhileOneIsAppliedAreAppliedTogetherAfterIt() = runTest {
        val reloader = createReloader()
        reloader.onGameStarting("hotReloadDesktopMain", emptyMap())
        val firstReload = CompletableDeferred<Boolean>()
        reloadResult = { firstReload.await() }
        reloader.onCodeChanged()
        reloadResult = { true }
        reloader.onCodeChanged()
        reloader.onCodeChanged()
        assertEquals(1, builds)
        firstReload.complete(true)
        assertEquals(2, builds)
        assertEquals(2, reloads.size)
    }
}
