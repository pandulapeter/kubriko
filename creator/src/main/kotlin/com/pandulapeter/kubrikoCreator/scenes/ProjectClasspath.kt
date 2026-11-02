/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.foundation.CreatorLibraries
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.HotReloadLaunch
import java.io.File

/**
 * What the init script's `kubrikoCreatorClasspath` task writes: the project's Kubriko version, the runtime classpath of
 * its desktop module (its own compiled classes first), and how to play the game from it ([launch], null if it can't be),
 * with Compose Hot Reload ([hotReloadLaunch], only written for a play session, and null if it can't be), and the versions
 * of the libraries it resolved ([libraries], empty in a file written by an older Kubriko Creator). Such a file parses as
 * before and passes the [SharedLibraries] check, and an older Creator reads the `library.` lines as files that don't exist,
 * which every user of the classpath skips.
 */
internal data class ProjectClasspath(
    val kubrikoVersion: String,
    val files: List<File>,
    val launch: GameLaunch? = null,
    val hotReloadLaunch: HotReloadLaunch? = null,
    val libraries: Map<String, String> = emptyMap(),
) {
    companion object {
        private const val VERSION_PREFIX = "kubrikoVersion="
        private const val LIBRARY_PREFIX = "library."

        fun parse(text: String): ProjectClasspath? {
            val lines = text.lines().filter { it.isNotBlank() }
            val version = lines.firstOrNull()?.takeIf { it.startsWith(VERSION_PREFIX) }?.removePrefix(VERSION_PREFIX) ?: return null
            val (libraryLines, otherLines) = lines.drop(1).partition { it.startsWith(LIBRARY_PREFIX) }
            val (launchLines, files) = otherLines.partition { it.startsWith(GameLaunch.PREFIX) }
            return ProjectClasspath(
                kubrikoVersion = version,
                files = files.map(::File),
                launch = GameLaunch.parse(launchLines),
                hotReloadLaunch = HotReloadLaunch.parse(launchLines),
                libraries = CreatorLibraries.parseLines(libraryLines.map { it.removePrefix(LIBRARY_PREFIX) }),
            )
        }
    }
}
