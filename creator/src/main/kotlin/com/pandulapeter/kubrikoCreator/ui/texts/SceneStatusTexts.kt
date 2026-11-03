/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.scenes.SceneStatus
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_scene_newer_libraries
import kubriko.creator.generated.resources.status_scene_opened
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [SceneStatus].
 */
internal fun SceneStatus.toResource(): StringResource = when (this) {
    SceneStatus.NEWER_LIBRARIES -> Res.string.status_scene_newer_libraries
    SceneStatus.OPENED -> Res.string.status_scene_opened
}
