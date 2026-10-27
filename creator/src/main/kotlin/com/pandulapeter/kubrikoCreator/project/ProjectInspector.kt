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

/**
 * Asks Gradle about a project's structure instead of guessing from its layout.
 */
internal object ProjectInspector {

    fun inspect(connection: GradleProjectConnection): CreatorProject {
        val root = connection.model<GradleProject>()
        return CreatorProject(
            rootDirectory = connection.rootDirectory,
            name = root.name,
            desktopModulePath = findDesktopModulePath(ModuleNode.from(root)),
        )
    }

    /**
     * The first module, in Gradle's order, with a Compose desktop `run` task.
     */
    internal fun findDesktopModulePath(root: ModuleNode) = root.withDescendants().firstOrNull { "run" in it.taskNames }?.path
        ?: throw ProjectInspectionException(Res.string.error_no_desktop_module)
}
