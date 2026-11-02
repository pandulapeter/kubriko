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

import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator

/**
 * Template version 2: `GameEntryPoint.kt` hands the generated Manager types to the Scene editor, as `Game.kt` hands them
 * to Kubriko (which [SceneWiringUpgrade] makes). An entry point that creates custom Managers of its own, or doesn't create
 * its `SerializationManager` in one line, is left to the user.
 */
internal object SceneEditorManagersUpgrade : TemplateStep {
    override val version = 2

    override fun isDone(text: String, context: TemplateContext) = ManagerTypeCodeGenerator.WIRING_NAME in text

    override fun change(text: String, context: TemplateContext) = ProjectUpgrader.withManagersHandedToTheSceneEditor(text, context.wiringImport)
}
