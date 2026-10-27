/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import java.io.File

/**
 * What the user asked for when creating a project.
 *
 * @param name The project's display name, such as "My Game".
 * @param packageName The package of the project's code.
 * @param parentDirectory The folder the project's own folder is created in.
 * @param isUnderVersionControl Whether the project starts as a git repository with everything committed.
 */
internal data class NewProjectRequest(
    val name: String,
    val packageName: String,
    val parentDirectory: File,
    val template: ProjectTemplate = ProjectTemplate.STARTER,
    val isUnderVersionControl: Boolean = true,
) {
    val projectId get() = ProjectNaming.projectId(name)
    val directory get() = File(parentDirectory, projectId)
}
