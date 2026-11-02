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

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.settingsDirectory
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Remembers the projects opened recently, newest first, in [file]. Projects whose folder is gone are kept (the folder may
 * be on a drive that isn't mounted), and a file that can't be read counts as an empty list rather than an error (one that
 * can't be decoded is kept as `recent-projects.json.bak` by the next change). A list that can't be written is forgotten, not
 * worth interrupting anyone for. Reading, changing, and writing the file happen one call at a time, whichever thread calls.
 */
internal class RecentProjectsStore(
    file: File,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val store = JsonFileStore(file, ListSerializer(RecentProject.serializer()))

    private val lock = Any()

    fun load(): List<RecentProject> = synchronized(lock) { read() }

    fun add(directory: File, name: String): List<RecentProject> = synchronized(lock) {
        val path = directory.absoluteFile.normalize().path
        (listOf(RecentProject(path = path, name = name, lastOpened = currentTimeMillis())) + read().filter { it.path != path })
            .take(MAXIMUM_COUNT)
            .also(store::write)
    }

    fun remove(path: String): List<RecentProject> = synchronized(lock) { read().filter { it.path != path }.also(store::write) }

    private fun read() = store.read().valueOrNull()?.sortedByDescending { it.lastOpened }.orEmpty()

    companion object {
        const val MAXIMUM_COUNT = 10

        /**
         * The store in Kubriko Creator's settings folder, where each platform keeps application settings.
         */
        fun default() = RecentProjectsStore(File(settingsDirectory(), "recent-projects.json"))
    }
}
