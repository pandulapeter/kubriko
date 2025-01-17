/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSwitch

@Composable
internal fun DebugMenuSettings(
    isDebutMenuEnabled: Boolean,
    onIsDebutMenuEnabledChanged: (Boolean) -> Unit,
) = EditorSwitch(
    modifier = Modifier,
    text = "Debug Menu",
    isChecked = isDebutMenuEnabled,
    onCheckedChanged = onIsDebutMenuEnabledChanged,
)