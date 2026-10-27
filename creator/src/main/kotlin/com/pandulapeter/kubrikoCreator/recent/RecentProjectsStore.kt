/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.recent

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Remembers the projects opened recently, newest first, in [file]. Projects whose folder is gone are left out (and
 * forgotten), and a file that can't be read counts as an empty list rather than an error.
 */
internal class RecentProjectsStore(
    private val file: File,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val serializer = ListSerializer(RecentProject.serializer())
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): List<RecentProject> {
        val stored = read()
        val available = stored.filter { it.directory.isDirectory }
        if (available.size != stored.size) write(available)
        return available
    }

    fun add(directory: File, name: String): List<RecentProject> {
        val path = directory.absoluteFile.normalize().path
        val updated = (listOf(RecentProject(path = path, name = name, lastOpened = currentTimeMillis())) + load().filter { it.path != path })
            .take(MAXIMUM_COUNT)
        write(updated)
        return updated
    }

    fun remove(path: String): List<RecentProject> = load().filter { it.path != path }.also(::write)

    private fun read() = try {
        if (file.isFile) json.decodeFromString(serializer, file.readText()).sortedByDescending { it.lastOpened } else emptyList()
    } catch (_: IOException) {
        emptyList()
    } catch (_: SerializationException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun write(projects: List<RecentProject>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, projects))
        } catch (_: IOException) {
            // Forgetting the list is not worth interrupting anyone for.
        }
    }

    companion object {
        const val MAXIMUM_COUNT = 10

        /**
         * The store in Kubriko Creator's settings folder, where each platform keeps application settings.
         */
        fun default() = RecentProjectsStore(File(settingsDirectory(), "recent-projects.json"))

        internal fun settingsDirectory(
            operatingSystem: String = System.getProperty("os.name").orEmpty().lowercase(),
            home: String = System.getProperty("user.home"),
            appData: String? = System.getenv("APPDATA"),
            xdgConfigHome: String? = System.getenv("XDG_CONFIG_HOME"),
        ) = when {
            "mac" in operatingSystem -> File(home, "Library/Application Support/Kubriko Creator")
            "windows" in operatingSystem -> File(appData ?: "$home/AppData/Roaming", "Kubriko Creator")
            else -> File(xdgConfigHome ?: "$home/.config", "kubriko-creator")
        }
    }
}
