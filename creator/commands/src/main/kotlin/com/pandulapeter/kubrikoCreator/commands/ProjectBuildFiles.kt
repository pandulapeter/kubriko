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

import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade

/**
 * The template upgrade the project needs, as commands see it: as last checked in the background, so that validating reads no
 * files, and told when a command brought the template files up to date.
 */
public interface ProjectUpgrades {

    /**
     * What the project's template files need, as last checked.
     */
    public fun current(): TemplateUpgrade

    /**
     * Tells the rest of the editor that the project's template files were brought up to date.
     */
    public fun onUpgraded()

    public companion object {
        /**
         * A project whose template files need nothing, with no one to tell.
         */
        public val UpToDate: ProjectUpgrades = object : ProjectUpgrades {
            override fun current(): TemplateUpgrade = TemplateUpgrade.UpToDate

            override fun onUpgraded() = Unit
        }
    }
}
