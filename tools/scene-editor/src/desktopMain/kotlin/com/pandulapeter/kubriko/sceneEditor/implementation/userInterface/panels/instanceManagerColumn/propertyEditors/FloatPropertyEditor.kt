package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput

@Composable
internal fun FloatPropertyEditor(
    modifier: Modifier = Modifier,
    name: String,
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
    shouldUseHorizontalLayout: Boolean = false,
) = Column(
    modifier = modifier,
) {
    EditorNumberInput(
        name = name,
        suffix = suffix,
        value = value,
        onValueChanged = onValueChanged,
        valueRange = valueRange,
        enabled = enabled,
        shouldUseHorizontalLayout = shouldUseHorizontalLayout,
    )
}