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
 * The state of the New project screen. The package follows the name until the user edits it.
 */
internal data class NewProjectForm(
    val name: String = "",
    val packageName: String = "",
    val parentDirectory: File,
    val isPackageNameEdited: Boolean = false,
) {
    val nameError get() = if (name.isEmpty()) null else ProjectNaming.validateName(name)
    val packageNameError get() = if (packageName.isEmpty()) null else ProjectNaming.validatePackageName(packageName)

    /**
     * What to create, or null while the form is incomplete or invalid.
     */
    val request
        get() = if (ProjectNaming.validateName(name) == null && ProjectNaming.validatePackageName(packageName) == null) {
            NewProjectRequest(name = name.trim(), packageName = packageName, parentDirectory = parentDirectory)
        } else {
            null
        }

    fun withName(name: String) = copy(
        name = name,
        packageName = if (isPackageNameEdited) packageName else ProjectNaming.defaultPackageName(name),
    )

    fun withPackageName(packageName: String) = copy(
        packageName = packageName,
        isPackageNameEdited = true,
    )
}
