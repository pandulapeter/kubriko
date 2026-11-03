/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import org.gradle.tooling.model.GradleProject

/**
 * Finds a project's desktop module. It is asked from Gradle in the background, since that configures every task of the
 * build, and only builds need it.
 */
internal object DesktopModules {

    /**
     * The Gradle path of the first module, in Gradle's order, with a Compose desktop `run` task.
     */
    suspend fun pathOf(connection: GradleProjectConnection) = find(ModuleNode.from(connection.model(GradleProject::class.java)))

    internal fun find(root: ModuleNode) = root.withDescendants().firstOrNull { "run" in it.taskNames }?.path
        ?: throw ProjectInspectionException(ProjectProblem.NO_DESKTOP_MODULE)
}
