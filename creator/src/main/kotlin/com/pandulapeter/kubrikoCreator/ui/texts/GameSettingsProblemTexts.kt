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

import com.pandulapeter.kubrikoCreator.project.GameSettingsProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_game_settings_first_scene_invalid
import kubriko.creator.generated.resources.error_game_settings_frame_rate_invalid
import kubriko.creator.generated.resources.error_game_settings_viewport_size_invalid
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [GameSettingsProblem].
 */
internal fun GameSettingsProblem.toResource(): StringResource = when (this) {
    GameSettingsProblem.FIRST_SCENE_INVALID -> Res.string.error_game_settings_first_scene_invalid
    GameSettingsProblem.VIEWPORT_SIZE_INVALID -> Res.string.error_game_settings_viewport_size_invalid
    GameSettingsProblem.FRAME_RATE_INVALID -> Res.string.error_game_settings_frame_rate_invalid
}
