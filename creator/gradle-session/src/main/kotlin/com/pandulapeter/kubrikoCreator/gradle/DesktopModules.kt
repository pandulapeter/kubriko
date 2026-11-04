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

import org.gradle.tooling.model.GradleProject

/**
 * Finds a project's desktop modules. They are asked from Gradle in the background, since that configures every task of the
 * build, and only builds need them.
 */
public object DesktopModules {

    /**
     * The Gradle paths of the modules with a Compose desktop `run` task, in Gradle's order (depth first, children by name),
     * which is empty for a project without one.
     */
    public suspend fun pathsOf(connection: GradleProjectConnection): List<String> = pathsIn(ModuleNode.from(connection.model(GradleProject::class.java)))

    internal fun pathsIn(root: ModuleNode): List<String> = root.withDescendants().filter { "run" in it.taskNames }.map { it.path }.toList()
}
