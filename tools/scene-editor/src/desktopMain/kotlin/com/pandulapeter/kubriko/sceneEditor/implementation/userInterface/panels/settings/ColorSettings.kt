package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorRadioButton

@Composable
internal fun ColorSettings(
    colorEditorMode: ColorEditorMode,
    onColorEditorModeChanged: (ColorEditorMode) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    ColorEditorMode.entries.forEach { mode ->
        EditorRadioButton(
            label = when (mode) {
                ColorEditorMode.HSV -> "HSV"
                ColorEditorMode.RGB -> "RGB"
            },
            isSmall = true,
            isSelected = mode == colorEditorMode,
            onSelectionChanged = { onColorEditorModeChanged(mode) },
        )
    }
}

internal enum class ColorEditorMode {
    HSV,
    RGB,
}