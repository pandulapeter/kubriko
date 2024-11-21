package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorSlider(
    modifier: Modifier = Modifier,
    title: String,
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
) = Column(
    modifier = modifier,
) {
    EditorTextLabel(
        text = "$title: ${"%.2f".format(value)}$suffix",
    )
    Slider(
        modifier = Modifier.height(24.dp),
        value = value,
        onValueChange = onValueChanged,
        valueRange = valueRange,
        enabled = enabled,
    )
}