/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import androidx.compose.ui.input.key.Key
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.interaction_mode_rotate
import kubriko.tools.scene_editor.generated.resources.interaction_mode_scale
import kubriko.tools.scene_editor.generated.resources.interaction_mode_translate
import org.jetbrains.compose.resources.StringResource

internal enum class SceneEditorInteractionMode(
    val label: StringResource,
    val shortcut: Key,
) {
    Translate(label = Res.string.interaction_mode_translate, shortcut = Key.T),
    Scale(label = Res.string.interaction_mode_scale, shortcut = Key.S),
    Rotate(label = Res.string.interaction_mode_rotate, shortcut = Key.R),
}
