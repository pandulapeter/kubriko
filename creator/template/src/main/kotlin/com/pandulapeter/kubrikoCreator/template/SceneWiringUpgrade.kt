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
 * Template version 3: the generated `GeneratedScenes` loads the first Scene, so that Blocks can load others. `Game.kt` hands
 * `KubrikoWiring.createManagers` the Scene its `SceneLoader` loaded instead (creating the generated Managers there if
 * version 2 didn't yet), which only works while it has one `Kubriko.newInstance` with a one-line `SceneLoader` in it, and
 * `SceneLoader.kt` is deleted ([SceneLoaderDeletion]).
 */
internal object SceneWiringUpgrade : TemplateStep {
    override val version = 3

    override fun isDone(text: String, context: TemplateContext) = ProjectUpgrader.loadsScenesThroughWiring(text)

    override fun change(text: String, context: TemplateContext) = ProjectUpgrader.withScenesLoadedByWiring(text, context.wiringImport)

    /**
     * `SceneLoader.kt` deleted once `Game.kt` no longer uses it, if it is still the template's: one changed by hand is left alone.
     */
    object SceneLoaderDeletion : TemplateDeletion {
        override fun requiredPath(context: TemplateContext) = context.gamePath

        override fun isTemplates(text: String, context: TemplateContext) = text.trimEnd() == ProjectUpgrader.templateSceneLoader(context.packageName).trimEnd()
    }
}
