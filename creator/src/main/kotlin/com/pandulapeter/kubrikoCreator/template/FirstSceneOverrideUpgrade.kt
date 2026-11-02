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
 * Template version 7: Kubriko Creator plays the game from any Scene. The desktop `Main.kt` also sets
 * `KubrikoWiring.firstSceneOverride` from a system property, right after it turns the Log Blocks on.
 */
internal object FirstSceneOverrideUpgrade : TemplateStep {
    override val version = 7

    override fun isDone(text: String, context: TemplateContext) = ProjectUpgrader.overridesFirstScene(text)

    override fun change(text: String, context: TemplateContext) = ProjectUpgrader.withFirstSceneOverride(text, context.wiringImport)
}
