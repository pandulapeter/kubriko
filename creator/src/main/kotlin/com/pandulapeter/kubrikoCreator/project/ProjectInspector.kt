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

import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_no_desktop_module
import org.gradle.tooling.model.GradleProject
import java.io.File

/**
 * What Kubriko Creator needs to know about a project's build. Opening a project only reads its files ([isGradleProject],
 * [nameOf]); the desktop module is asked from Gradle in the background ([desktopModulePathOf]), since that configures
 * every task of the build, and only builds need it.
 */
internal object ProjectInspector {

    private val buildFileNames = listOf("settings.gradle.kts", "settings.gradle", "build.gradle.kts", "build.gradle")
    private val rootProjectNamePattern = Regex("""rootProject\.name\s*=\s*["']([^"']+)["']""")

    fun isGradleProject(directory: File) = buildFileNames.any { File(directory, it).isFile }

    /**
     * The root project's name as the settings file sets it, or the folder's name, as Gradle defaults to.
     */
    fun nameOf(directory: File) = listOf("settings.gradle.kts", "settings.gradle")
        .firstNotNullOfOrNull { name -> File(directory, name).takeIf { it.isFile }?.readText()?.let { rootProjectNamePattern.find(it)?.groupValues?.get(1) } }
        ?: directory.name

    /**
     * The Gradle path of the first module, in Gradle's order, with a Compose desktop `run` task.
     */
    fun desktopModulePathOf(connection: GradleProjectConnection) = findDesktopModulePath(ModuleNode.from(connection.model(GradleProject::class.java)))

    internal fun findDesktopModulePath(root: ModuleNode) = root.withDescendants().firstOrNull { "run" in it.taskNames }?.path
        ?: throw ProjectInspectionException(Res.string.error_no_desktop_module)
}
