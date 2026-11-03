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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.session.WebGameServer
import com.pandulapeter.kubrikoCreator.testing.blockingTest
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebPlayerTest {
    private val moduleDirectory = testDirectory()
    private val distribution = File(moduleDirectory, WebPlayer.DISTRIBUTION_PATH)
    private val console = ConsoleLog()
    private val builds = Channel<Unit>(Channel.UNLIMITED)
    private var isBuildSuccessful = true
    private val opened = mutableListOf<URI>()
    private var canOpenBrowser = true
    private val http = HttpClient.newHttpClient()
    private val playerScope = CoroutineScope(Dispatchers.IO + Job())

    @AfterTest
    fun tearDown() {
        playerScope.cancel()
    }

    private val statuses get() = console.entries.value.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    private fun player() = WebPlayer(
        runTask = { taskName, _ ->
            assertEquals(WebPlayer.DISTRIBUTION_TASK_NAME, taskName)
            builds.send(Unit)
            isBuildSuccessful
        },
        moduleDirectory = { moduleDirectory },
        openBrowser = { address ->
            opened += address
            canOpenBrowser
        },
        console = console,
        scope = playerScope,
        ioDispatcher = Dispatchers.IO,
    )

    private fun page(address: URI) = http.send(HttpRequest.newBuilder(address).build(), HttpResponse.BodyHandlers.ofString())

    @Test
    fun theGameIsBuiltServedAndOpenedInTheBrowserUntilItIsStopped() = blockingTest {
        File(distribution, "index.html").apply { parentFile.mkdirs() }.writeText("<body></body>")
        val isUp = CompletableDeferred<Unit>()
        val playing = launch { player().play(firstScene = "levels/1-1") { isUp.complete(Unit) } }
        withTimeout(10_000) { isUp.await() }
        val address = opened.single()
        assertContains(address.toString(), "kubriko.creator.scene=levels%2F1-1")
        assertEquals(200, page(address).statusCode())
        assertEquals(listOf(WorkspaceStatus.WEB_GAME_SERVED), statuses)
        playing.cancelAndJoin()
        assertFailsWith<IOException> { page(address) }
    }

    @Test
    fun aChangeWhilePlayingBuildsAgainAndReloadsThePage() = blockingTest {
        File(distribution, "index.html").apply { parentFile.mkdirs() }.writeText("<body></body>")
        val player = player()
        player.onCodeChanged()
        val isUp = CompletableDeferred<Unit>()
        val playing = launch { player.play(firstScene = null) { isUp.complete(Unit) } }
        withTimeout(10_000) { isUp.await() }
        builds.receive()
        assertTrue(builds.tryReceive().isFailure, "A change before playing built the game again.")
        player.onCodeChanged()
        withTimeout(10_000) { builds.receive() }
        withTimeout(10_000) { while (WorkspaceStatus.WEB_GAME_RELOADED !in statuses) delay(10) }
        playing.cancelAndJoin()
    }

    @Test
    fun aFailedBuildOrAMissingPageServesNothing() = blockingTest {
        isBuildSuccessful = false
        assertFalse(player().play(firstScene = null) { error("Up without a build") })
        isBuildSuccessful = true
        assertFalse(player().play(firstScene = null) { error("Up without a page") })
        assertEquals(listOf(WorkspaceStatus.WEB_GAME_MISSING), statuses)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun aBrowserThatCantBeOpenedLeavesTheAddressInTheConsole() = blockingTest {
        File(distribution, "index.html").apply { parentFile.mkdirs() }.writeText("<body></body>")
        canOpenBrowser = false
        val isUp = CompletableDeferred<Unit>()
        val playing = launch { player().play(firstScene = null) { isUp.complete(Unit) } }
        withTimeout(10_000) { isUp.await() }
        withTimeout(10_000) { while (WorkspaceStatus.BROWSER_NOT_OPENED !in statuses) delay(10) }
        playing.cancelAndJoin()
    }

    @Test
    fun theServedPageListensForReloads() = blockingTest {
        File(distribution, "index.html").apply { parentFile.mkdirs() }.writeText("<body></body>")
        val isUp = CompletableDeferred<Unit>()
        val playing = launch { player().play(firstScene = null) { isUp.complete(Unit) } }
        withTimeout(10_000) { isUp.await() }
        assertContains(page(opened.single()).body(), WebGameServer.EVENTS_PATH)
        playing.cancelAndJoin()
    }
}
