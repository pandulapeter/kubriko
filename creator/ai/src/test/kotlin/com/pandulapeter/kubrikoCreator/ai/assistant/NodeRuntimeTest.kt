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

import org.junit.Assume.assumeFalse
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeRuntimeTest {
    private val directory = createTempDirectory().toFile()
    private val releases = TestNodeReleases(directory)
    private val downloads get() = releases.downloads
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun runtime(sums: (File) -> String, platform: String? = "darwin-aarch64") = releases.runtime(sums, platform)

    @Test
    fun theNewestLtsReleaseForThisPlatformIsDownloadedCheckedAndFoundAgain() {
        assumeFalse(isWindows)
        val runtime = runtime(releases::validSums)
        assertNull(runtime.installedNpx())
        val npx = runtime.install()
        assertEquals(listOf("https://nodejs.org/dist/v24.11.1/node-v24.11.1-darwin-arm64.tar.gz"), downloads)
        assertEquals("npx", npx.name)
        assertTrue(File(npx.parentFile, "node").canExecute())
        assertEquals(npx.canonicalFile, runtime.installedNpx()?.canonicalFile)
    }

    @Test
    fun aDownloadNotMatchingNodesChecksumIsRejected() {
        assumeFalse(isWindows)
        assertFailsWith<IOException> { runtime({ "0000  node-v24.11.1-darwin-arm64.tar.gz" }).install() }
        assertNull(runtime({ "" }).installedNpx())
    }

    @Test
    fun aPlatformNodeHasNoBuildForIsSaidSo() {
        assertFailsWith<IOException> { runtime({ "" }, platform = null).install() }
    }

    @Test
    fun nodesNamesAndListsAreRead() {
        assertEquals("darwin-arm64", NodeRuntime.nodePlatformOf("darwin-aarch64"))
        assertEquals("win-x64", NodeRuntime.nodePlatformOf("windows-x86_64"))
        assertNull(NodeRuntime.nodePlatformOf(null))
        assertEquals("v24.11.1", NodeRuntime.latestLts(TestNodeReleases.INDEX))
        assertNull(NodeRuntime.latestLts("nope"))
        assertEquals("abc", NodeRuntime.checksumOf("abc  node.zip\ndef  other.zip", "node.zip"))
    }
}
