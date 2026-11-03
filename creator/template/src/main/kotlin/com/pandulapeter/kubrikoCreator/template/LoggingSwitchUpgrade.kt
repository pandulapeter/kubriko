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
 * Template version 5: play sessions turn the Log Blocks on, which released games leave off. The desktop `Main.kt` sets
 * `KubrikoWiring.isLoggingEnabled` from a system property before the application starts, which only works while its
 * `main` is the template's `application` expression at the end of the file.
 */
internal object LoggingSwitchUpgrade : TemplateStep {
    override val version = 5

    override fun isDone(text: String, context: TemplateContext) = ProjectUpgrader.turnsLoggingOn(text)

    override fun change(text: String, context: TemplateContext) = ProjectUpgrader.withLoggingTurnedOn(text, context.wiringImport)
}
