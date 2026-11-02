/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KubrikoApiTest {
    private val home = createTempDirectory().toFile()
    private val cache = File(home, "cache")
    private val downloads = mutableListOf<String>()

    @AfterTest
    fun tearDown() {
        home.deleteRecursively()
    }

    private fun sourcesJar(file: File, vararg sources: Pair<String, String>) = file.also { jar ->
        jar.parentFile.mkdirs()
        ZipOutputStream(jar.outputStream()).use { zip ->
            sources.forEach { (path, text) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
    }

    @Test
    fun theOverviewKeepsHowToUseKubrikoAndLeavesOutHowTheRepositoryIsBuilt() {
        val overview = KubrikoApi.overviewOf("# CLAUDE.md\n## Build, run, test\ngradlew\n## Core architecture\nManagers\n## Tools\ndebug\n## Common patterns\nActor\n## Performance\nfast")
        assertEquals("## Core architecture\nManagers\n\n## Common patterns\nActor", overview)
        assertTrue(KubrikoApi.overview.contains("ActorManager"))
    }

    @Test
    fun aHeadingQuotedInsideALineIsNotTakenForTheHeading() {
        val overview = KubrikoApi.overviewOf("Keep \"## Core architecture\" and \"## Tools\" as headings.\n## Core architecture\nSee `## Tools`.\n## Tools\ndebug")
        assertEquals("## Core architecture\nSee `## Tools`.", overview)
    }

    @Test
    fun theOverviewOfTheRootClaudeMdHasBothSections() {
        val claudeMd = File("../CLAUDE.md")
        assertTrue(claudeMd.isFile, "Run from the creator folder: ${claudeMd.absolutePath}")
        val overview = KubrikoApi.overviewOf(claudeMd.readText())
        assertTrue(overview.startsWith("## Core architecture"))
        assertContains(overview, "ActorManager")
        assertContains(overview, "## Common patterns")
        assertContains(overview, "class MyManager")
        assertFalse("## Tools" in overview)
        assertFalse("## Conventions" in overview)
        assertFalse("## Performance" in overview)
    }

    @Test
    fun aClassIsFoundInMavenLocalsSourcesJarEngineFirst() {
        sourcesJar(
            File(home, ".m2/repository/io/github/pandulapeter/kubriko/engine/1.0.0/engine-1.0.0-sources.jar"),
            "commonMain/com/pandulapeter/kubriko/actor/body/BoxBody.kt" to "/** A box. */\nclass BoxBody(\n    initialPosition: SceneOffset,\n)",
            "commonMain/com/pandulapeter/kubriko/actor/body/PointBody.kt" to "open class PointBody",
        )
        val sources = KubrikoSources("1.0.0", cache, home) { url, _ ->
            downloads += url
            throw IOException("Offline.")
        }
        val found = sources.find("BoxBody")!!
        assertEquals(KubrikoArtifact.ENGINE, found.artifact)
        assertContains(found.text, "/** A box. */")
        assertEquals("open class PointBody", sources.find("PointBody")?.text)
        assertNull(sources.find("Missing"))
        assertFalse(downloads.any { "/engine/" in it })
    }

    @Test
    fun aSourcesJarNotDownloadedYetIsFetchedFromMavenCentralOnce() {
        val served = sourcesJar(File(home, "served.jar"), "commonMain/Physics.kt" to "interface RigidBody")
        val sources = KubrikoSources("1.0.0", cache, home) { url, target ->
            downloads += url
            if ("plugin-physics" in url) served.copyTo(target, overwrite = true) else throw IOException("Not found.")
        }
        assertEquals(KubrikoArtifact.PHYSICS, sources.find("RigidBody")?.artifact)
        assertTrue("https://repo1.maven.org/maven2/io/github/pandulapeter/kubriko/plugin-physics/1.0.0/plugin-physics-1.0.0-sources.jar" in downloads)
        val count = downloads.size
        sources.find("RigidBody")
        assertEquals(count + KubrikoArtifact.entries.indexOf(KubrikoArtifact.PHYSICS), downloads.size)
    }
}
