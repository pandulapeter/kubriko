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
    public val template: ProjectTemplate = ProjectTemplate.STARTER,
    public val pitch: String? = null,
    public val targets: Set<GameTarget>? = null,
) {
    /**
     * The targets the project is built for, as chosen or as the templates have them: the desktop and the web.
     */
    public val shownTargets: Set<GameTarget> get() = targets ?: TEMPLATE_TARGETS

    /**
     * Whether the project is created from an idea ([pitch]): from the Blank template, its writers' room starting from it.
     */
    public val isFromIdea: Boolean get() = pitch != null

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
        get() = if (ProjectNaming.validateName(name) == null && ProjectNaming.validatePackageName(packageName) == null && pitch?.isBlank() != true) {
            NewProjectRequest(
                name = name.trim(),
                packageName = packageName,
                parentDirectory = parentDirectory,
                template = if (isFromIdea) ProjectTemplate.BLANK else template,
                isUnderVersionControl = isUnderVersionControl,
                targets = targets?.minus(GameTarget.DESKTOP),
                pitch = pitch?.trim(),
            )
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

    /**
     * The form with [target] turned on or off, which the desktop never is.
     */
    public fun withTarget(target: GameTarget, isOn: Boolean): NewProjectForm =
        if (target == GameTarget.DESKTOP) this else copy(targets = if (isOn) shownTargets + target else shownTargets - target)

    public companion object {
        private val TEMPLATE_TARGETS = setOf(GameTarget.DESKTOP, GameTarget.WEB)

        /**
         * The form [request] was made from, so a creation that failed comes back to the form as it was filled in.
         */
        public fun of(request: NewProjectRequest): NewProjectForm = NewProjectForm(
            name = request.name,
            packageName = request.packageName,
            parentDirectory = request.parentDirectory,
            isPackageNameEdited = request.packageName != ProjectNaming.defaultPackageName(request.name),
            isUnderVersionControl = request.isUnderVersionControl,
            template = request.template,
            pitch = request.pitch,
            targets = request.targets?.plus(GameTarget.DESKTOP),
        )
    }
}
