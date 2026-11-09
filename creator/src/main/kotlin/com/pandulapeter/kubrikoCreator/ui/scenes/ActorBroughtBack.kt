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

import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActors

/**
 * The ID of the one Actor an undo or redo ([step]) brought back into the Scene called [sceneName] (an undone removal,
 * or a placement made again), which the Scene editor selects as it selects a placed one, or null if it brought back none,
 * or several.
 */
internal fun actorBroughtBackBy(step: Invocation<*>, sceneName: String): String? = when (val input = step.input) {
    is RestoreActor.Input -> input.actorId.takeIf { input.scene == sceneName }
    is RestoreActors.Input -> input.actors.singleOrNull()?.actorId.takeIf { input.scene == sceneName }
    is PlaceActor.Input -> input.actorId.takeIf { input.scene == sceneName }
    else -> null
}
