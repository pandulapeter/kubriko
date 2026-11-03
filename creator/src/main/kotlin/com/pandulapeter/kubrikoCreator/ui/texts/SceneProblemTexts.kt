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

import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_entry_point_missing
import kubriko.creator.generated.resources.error_entry_point_multiple
import kubriko.creator.generated.resources.error_entry_point_not_loaded
import kubriko.creator.generated.resources.error_scene_view_failed
import kubriko.creator.generated.resources.scene_build_failed
import kubriko.creator.generated.resources.scene_unsupported
import kubriko.creator.generated.resources.scene_unsupported_library
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [SceneProblem].
 */
internal fun SceneProblem.toResource(): StringResource = when (this) {
    SceneProblem.ENTRY_POINT_MISSING -> Res.string.error_entry_point_missing
    SceneProblem.ENTRY_POINT_MULTIPLE -> Res.string.error_entry_point_multiple
    SceneProblem.ENTRY_POINT_NOT_LOADED -> Res.string.error_entry_point_not_loaded
    SceneProblem.VIEW_FAILED -> Res.string.error_scene_view_failed
    SceneProblem.BUILD_FAILED -> Res.string.scene_build_failed
    SceneProblem.UNSUPPORTED -> Res.string.scene_unsupported
    SceneProblem.UNSUPPORTED_LIBRARY -> Res.string.scene_unsupported_library
}
