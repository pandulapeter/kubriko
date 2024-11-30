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