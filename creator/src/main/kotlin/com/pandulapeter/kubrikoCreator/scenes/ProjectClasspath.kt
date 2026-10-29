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

import com.pandulapeter.kubrikoCreator.session.GameLaunch
import java.io.File

/**
 * What the init script's `kubrikoCreatorClasspath` task writes: the project's Kubriko version, the runtime classpath of
 * its desktop module (its own compiled classes first), and how to play the game from it ([launch], null if it can't be).
 */
internal data class ProjectClasspath(
    val kubrikoVersion: String,
    val files: List<File>,
    val launch: GameLaunch? = null,
) {
    companion object {
        private const val VERSION_PREFIX = "kubrikoVersion="

        fun parse(text: String): ProjectClasspath? {
            val lines = text.lines().filter { it.isNotBlank() }
            val version = lines.firstOrNull()?.takeIf { it.startsWith(VERSION_PREFIX) }?.removePrefix(VERSION_PREFIX) ?: return null
            val (launchLines, files) = lines.drop(1).partition { it.startsWith(GameLaunch.PREFIX) }
            return ProjectClasspath(kubrikoVersion = version, files = files.map(::File), launch = GameLaunch.parse(launchLines))
        }
    }
}
