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

import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.template.UpgradedFile.WhenMissing

/**
 * Every template file an upgrade may change, in the order its changes are shown, each with its steps in version order.
 */
internal object TemplateSteps {
    val FILES = listOf(
        UpgradedFile({ it.gamePath }, listOf(SceneWiringUpgrade, PluginManagersUpgrade, RunningInstancesUpgrade.GameStep), WhenMissing.BY_HAND),
        UpgradedFile({ it.entryPointPath }, listOf(SceneEditorManagersUpgrade, RunningInstancesUpgrade.EntryPointStep), WhenMissing.BY_HAND),
        UpgradedFile({ it.mainPath }, listOf(LoggingSwitchUpgrade, FirstSceneOverrideUpgrade, GameIdentityUpgrade.MainStep), WhenMissing.BY_HAND),
        UpgradedFile({ GameIdentity.BUILD_FILE }, listOf(GameIdentityUpgrade.BuildFileStep), WhenMissing.BY_HAND),
        UpgradedFile({ it.pagePath }, listOf(GameIdentityUpgrade.PageStep), WhenMissing.SKIP),
        UpgradedFile({ GameIdentity.GRADLE_PROPERTIES_FILE }, listOf(GradleCachesUpgrade, GameIdentityUpgrade.PropertiesStep), WhenMissing.CREATE_EMPTY),
        UpgradedFile({ it.sceneLoaderPath }, emptyList(), WhenMissing.SKIP, deletion = SceneWiringUpgrade.SceneLoaderDeletion),
    )
}
