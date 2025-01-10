package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

@Composable
internal fun Settings(
    modifier: Modifier = Modifier,
    colorEditorMode: ColorEditorMode,
    onColorEditorModeChanged: (ColorEditorMode) -> Unit,
    angleEditorMode: AngleEditorMode,
    onAngleEditorModeChanged: (AngleEditorMode) -> Unit,
    isDebutMenuEnabled: Boolean,
    onIsDebutMenuEnabledChanged: (Boolean) -> Unit,
) = KubrikoTheme {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 8.dp),
        ) {
            EditorText(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "Color controls",
            )
            Spacer(modifier = Modifier.height(8.dp))
            ColorSettings(
                colorEditorMode = colorEditorMode,
                onColorEditorModeChanged = onColorEditorModeChanged,
            )
            Spacer(modifier = Modifier.height(8.dp))
            EditorText(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "Angle controls",
            )
            Spacer(modifier = Modifier.height(8.dp))
            AngleSettings(
                angleEditorMode = angleEditorMode,
                onAngleEditorModeChanged = onAngleEditorModeChanged,
            )
            Spacer(modifier = Modifier.height(4.dp))
            DebugMenuSettings(
                isDebutMenuEnabled = isDebutMenuEnabled,
                onIsDebutMenuEnabledChanged = onIsDebutMenuEnabledChanged,
            )
        }
    }
}