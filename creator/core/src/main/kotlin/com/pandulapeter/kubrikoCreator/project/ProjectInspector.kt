/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import java.io.File

/**
 * What Kubriko Creator needs to know about a project's build to open it, read from its files only ([isGradleProject],
 * [nameOf]). The desktop module is asked from Gradle later (`DesktopModules`, in the `gradle` package).
 */
public object ProjectInspector {

    private val buildFileNames = listOf("settings.gradle.kts", "settings.gradle", "build.gradle.kts", "build.gradle")
    private val rootProjectNamePattern = Regex("""rootProject\.name\s*=\s*["']([^"']+)["']""")

    public fun isGradleProject(directory: File): Boolean = buildFileNames.any { File(directory, it).isFile }

    /**
     * The root project's name as the settings file sets it, or the folder's name, as Gradle defaults to.
     */
    public fun nameOf(directory: File): String = listOf("settings.gradle.kts", "settings.gradle")
        .firstNotNullOfOrNull { name -> File(directory, name).takeIf { it.isFile }?.readText()?.let { rootProjectNamePattern.find(it)?.groupValues?.get(1) } }
        ?: directory.name
}
