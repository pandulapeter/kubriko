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
public data class NewProjectForm(
    public val name: String = "",
    public val packageName: String = "",
    public val parentDirectory: File,
    public val isPackageNameEdited: Boolean = false,
    public val isUnderVersionControl: Boolean = true,
) {
    /**
     * Why [name] can't be used, or null while it is empty or valid.
     */
    public val nameError: TemplateProblem? get() = if (name.isEmpty()) null else ProjectNaming.validateName(name)

    /**
     * Why [packageName] can't be used, or null while it is empty or valid.
     */
    public val packageNameError: TemplateProblem? get() = if (packageName.isEmpty()) null else ProjectNaming.validatePackageName(packageName)

    /**
     * What to create, or null while the form is incomplete or invalid.
     */
    public val request: NewProjectRequest?
        get() = if (ProjectNaming.validateName(name) == null && ProjectNaming.validatePackageName(packageName) == null) {
            NewProjectRequest(name = name.trim(), packageName = packageName, parentDirectory = parentDirectory, isUnderVersionControl = isUnderVersionControl)
        } else {
            null
        }

    /**
     * The form with [name], and the package derived from it unless the user edited the package.
     */
    public fun withName(name: String): NewProjectForm = copy(
        name = name,
        packageName = if (isPackageNameEdited) packageName else ProjectNaming.defaultPackageName(name),
    )

    /**
     * The form with [packageName], which no longer follows the name.
     */
    public fun withPackageName(packageName: String): NewProjectForm = copy(
        packageName = packageName,
        isPackageNameEdited = true,
    )
}
