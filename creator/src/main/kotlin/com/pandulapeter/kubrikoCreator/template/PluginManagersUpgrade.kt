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

/**
 * Template version 4: `Game.kt` stops creating the plugins' Managers, which `KubrikoWiring` creates when the game uses
 * them, so that the project only depends on the plugins it uses. Managers created in more than one line, or configured,
 * are left to the user.
 */
internal object PluginManagersUpgrade : TemplateStep {
    override val version = 4

    override fun isDone(text: String, context: TemplateContext) = ProjectUpgrader.createsNoPluginManagers(text)

    override fun change(text: String, context: TemplateContext) = ProjectUpgrader.withoutPluginManagers(text)
}
