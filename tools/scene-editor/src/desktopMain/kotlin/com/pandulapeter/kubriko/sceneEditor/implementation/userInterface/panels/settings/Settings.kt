package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.KubrikoTheme
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText

@Composable
internal fun Settings(
    modifier: Modifier = Modifier,
    colorEditorMode: ColorEditorMode,
    onColorEditorModeChanged: (ColorEditorMode) -> Unit,
    angleEditorMode: AngleEditorMode,
    onAngleEditorModeChanged: (AngleEditorMode) -> Unit,
) = KubrikoTheme {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorText(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "Color controls",
            )
            ColorSettings(
                colorEditorMode = colorEditorMode,
                onColorEditorModeChanged = onColorEditorModeChanged,
            )
            EditorText(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "Angle controls",
            )
            AngleSettings(
                angleEditorMode = angleEditorMode,
                onAngleEditorModeChanged = onAngleEditorModeChanged,
            )
        }
    }
}