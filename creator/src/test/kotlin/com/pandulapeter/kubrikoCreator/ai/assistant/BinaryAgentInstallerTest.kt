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

import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BinaryAgentInstallerTest {
    private val directory = createTempDirectory().toFile()
    private val tools = File(directory, "tools")
    private val served = mutableMapOf<String, File>()
    private val downloads = mutableListOf<String>()
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")
    private val installer = BinaryAgentInstaller(tools, isWindows = isWindows, download = { url, target, _ ->
        downloads += url
        served[url]?.copyTo(target, overwrite = true) ?: throw IOException("Not found: $url")
    })

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun zip(name: String, vararg entries: Pair<String, String>) = File(directory, name).also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun agent(version: String = "1.4.5") = RegistryAgent("antigravity-acp", "Google Antigravity", version, AgentDistribution.Npx("unused"))

    private fun binary(url: String, sha256: String? = null) = AgentDistribution.Binary(url, "./agy_acp_server.par", sha256 = sha256)

    @Test
    fun anArchiveIsDownloadedOnceCheckedAndUnpackedPerVersion() {
        val archive = zip("agy.zip", "agy_acp_server.par" to "#!/bin/sh", "lib/data.txt" to "data")
        served[URL] = archive
        val program = installer.install(agent(), binary(URL, BinaryAgentInstaller.sha256Of(archive)))
        assertEquals(File(tools, "antigravity-acp/1.4.5/agy_acp_server.par").canonicalFile, program.canonicalFile)
        assertEquals("#!/bin/sh", program.readText())
        assertTrue(File(program.parentFile, "lib/data.txt").isFile)
        if (!isWindows) assertTrue(program.canExecute())
        installer.install(agent(), binary(URL, BinaryAgentInstaller.sha256Of(archive)))
        assertEquals(listOf(URL), downloads)
        assertFalse(File(tools, "antigravity-acp/1.4.5.download").exists())
    }

    @Test
    fun theProgramsAnArchiveStartsInTurnStayExecutable() {
        if (isWindows) return
        val archive = File(directory, "agy-helper.zip")
        FileSystems.newFileSystem(archive.toPath(), mapOf("create" to "true", "enablePosixFileAttributes" to "true")).use { zip ->
            mapOf("agy_acp_server.par" to "rwxr-xr-x", "localharness_external" to "rwxr-xr-x", "notes.txt" to "rw-r--r--").forEach { (name, permissions) ->
                val path = zip.getPath(name)
                Files.writeString(path, name)
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions))
            }
        }
        served[URL] = archive
        val folder = installer.install(agent(), binary(URL)).parentFile
        assertTrue(File(folder, "localharness_external").canExecute())
        assertFalse(File(folder, "notes.txt").canExecute())
    }

    @Test
    fun aNewVersionReplacesTheOldOne() {
        served[URL] = zip("agy.zip", "agy_acp_server.par" to "one")
        served[NEW_URL] = zip("agy-2.zip", "agy_acp_server.par" to "two")
        installer.install(agent(), binary(URL))
        assertEquals("two", installer.install(agent("1.5.0"), binary(NEW_URL)).readText())
        assertEquals(listOf("1.5.0"), File(tools, "antigravity-acp").list()?.toList())
    }

    @Test
    fun aDownloadThatDoesntMatchItsChecksumIsRejectedAndNothingIsKept() {
        served[URL] = zip("agy.zip", "agy_acp_server.par" to "tampered")
        assertFailsWith<IOException> { installer.install(agent(), binary(URL, "0".repeat(64))) }
        assertFalse(File(tools, "antigravity-acp/1.4.5").exists())
    }

    @Test
    fun anArchiveReachingOutsideItsFolderIsRejected() {
        served[URL] = zip("agy.zip", "../escaped.txt" to "no", "agy_acp_server.par" to "yes")
        assertFailsWith<IOException> { installer.install(agent(), binary(URL)) }
        assertFalse(File(tools, "antigravity-acp/escaped.txt").exists())
    }

    @Test
    fun anArchiveWithoutTheProgramIsRejected() {
        served[URL] = zip("agy.zip", "something_else" to "")
        assertFailsWith<IOException> { installer.install(agent(), binary(URL)) }
    }

    @Test
    fun aProgramDistributedAsItIsIsCopiedInPlace() {
        served["https://example.com/goose"] = File(directory, "goose").also { it.writeText("binary") }
        val program = installer.install(agent(), AgentDistribution.Binary("https://example.com/goose", "./goose"))
        assertEquals("binary", program.readText())
    }

    @Test
    fun tarArchivesAreUnpackedWithTheSystemsTar() {
        if (isWindows) return
        val content = File(directory, "content").also { it.mkdirs() }
        File(content, "agy_acp_server.par").writeText("from tar")
        val archive = File(directory, "agy.tar.gz")
        assertEquals(0, ProcessBuilder("tar", "-czf", archive.path, "-C", content.path, ".").start().waitFor())
        served[TAR_URL] = archive
        assertEquals("from tar", installer.install(agent(), binary(TAR_URL)).readText())
    }

    @Test
    fun aDownloadReportsItsProgressAsItGoesAndWhenItEnds() {
        val reports = mutableListOf<Pair<Long, Long?>>()
        val bytes = ByteArray(600 * 1024)
        BinaryAgentInstaller.copyReporting(bytes.inputStream(), java.io.ByteArrayOutputStream(), bytes.size.toLong()) { downloaded, total -> reports += downloaded to total }
        assertEquals(0L to bytes.size.toLong(), reports.first())
        assertEquals(bytes.size.toLong() to bytes.size.toLong(), reports.last())
        assertTrue(reports.size in 3..6, "Reported ${reports.size} times")
    }

    @Test
    fun namesAreMadeSafeForFolders() {
        assertEquals("_", BinaryAgentInstaller.safeName(".."))
        assertEquals("a_b", BinaryAgentInstaller.safeName("a/b"))
        assertEquals("1.4.5", BinaryAgentInstaller.safeName("1.4.5"))
    }

    private companion object {
        const val URL = "https://dl.google.com/agy-acp-server-1.4.5-darwin-arm64.zip"
        const val NEW_URL = "https://dl.google.com/agy-acp-server-1.5.0-darwin-arm64.zip"
        const val TAR_URL = "https://example.com/agy-acp-server.tar.gz"
    }
}
