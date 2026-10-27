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

import org.gradle.tooling.model.GradleProject

/**
 * The parts of a Gradle module that matter to Kubriko Creator, detached from the Tooling API's model.
 */
internal data class ModuleNode(
    val path: String,
    val taskNames: Set<String>,
    val children: List<ModuleNode> = emptyList(),
) {
    fun withDescendants(): Sequence<ModuleNode> = sequence {
        yield(this@ModuleNode)
        children.forEach { yieldAll(it.withDescendants()) }
    }

    companion object {

        fun from(project: GradleProject): ModuleNode = ModuleNode(
            path = project.path,
            taskNames = project.tasks.mapTo(HashSet()) { it.name },
            children = project.children.map(::from),
        )
    }
}
