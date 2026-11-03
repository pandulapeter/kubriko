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
public data class NewProjectRequest(
    public val name: String,
    public val packageName: String,
    public val parentDirectory: File,
    public val template: ProjectTemplate = ProjectTemplate.STARTER,
    public val isUnderVersionControl: Boolean = true,
) {
    internal val projectId get() = ProjectNaming.projectId(name)

    /**
     * The folder the project is created in.
     */
    public val directory: File get() = File(parentDirectory, projectId)
}
