/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade

/**
 * The project's build files that came from its template, as commands see them: the upgrade they need (as last checked in
 * the background, so that validating reads no files, and told when a command brought them up to date), and the targets they
 * build the game for.
 */
public interface ProjectBuildFiles {

    /**
     * What the project's template files need, as last checked.
     */
    public fun current(): TemplateUpgrade

    /**
     * Tells the rest of the editor that the project's template files were brought up to date, or that its build files
     * changed otherwise (a target was turned on or off).
     */
    public fun onUpgraded()

    /**
     * The targets the game is built for, or null if they can't be changed here.
     */
    public val targets: GameTargets? get() = null

    public companion object {
        /**
         * A project whose template files need nothing, with no one to tell, and no targets to change.
         */
        public val UpToDate: ProjectBuildFiles = object : ProjectBuildFiles {
            override fun current(): TemplateUpgrade = TemplateUpgrade.UpToDate

            override fun onUpgraded() = Unit
        }
    }
}
