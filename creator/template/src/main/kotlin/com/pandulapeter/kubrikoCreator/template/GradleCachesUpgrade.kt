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
 * Template version 6: Gradle's configuration cache and build cache are on in `gradle.properties`, so that Kubriko Creator's
 * builds skip configuring the project when nothing changed. A property set already, to any value, is kept, and a missing
 * file is created, so this is never left to the user.
 */
internal object GradleCachesUpgrade : TemplateStep {
    override val version = 6

    override fun isDone(text: String, context: TemplateContext) = ProjectUpgrader.setsCaches(text)

    override fun change(text: String, context: TemplateContext) = ProjectUpgrader.withCaches(text)
}
