/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack

/**
 * The ID of the Actor the Scene editor of the Scene called [sceneName] selects after an undo or redo brought [broughtBack]
 * back, as it selects a placed one: the one Actor brought back into it, unless that is its Scene settings, which are
 * never selected. Null when the step brought back several, or none into this Scene.
 */
internal fun restoredSelectionOf(broughtBack: ActorsBroughtBack, sceneName: String): String? = broughtBack.actors.singleOrNull()
    ?.takeIf { broughtBack.scene == sceneName && it.typeId != SceneSettingsCodeGenerator.SETTINGS_NAME }
    ?.id
