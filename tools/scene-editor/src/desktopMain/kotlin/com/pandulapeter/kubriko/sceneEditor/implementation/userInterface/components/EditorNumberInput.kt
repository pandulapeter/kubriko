package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun EditorNumberInput(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier,
    ) {
        EditorTextInput(
            title = title,
            value = "%.2f".format(value),
            onValueChanged = { newValue ->
                newValue.toFloatOrNull()?.let {
                    onValueChanged(if (valueRange == null) it else min(valueRange.endInclusive, max(valueRange.start, it)))
                }
            },
            enabled = enabled,
        )
        EditorSlider(
            value = value,
            onValueChanged = onValueChanged,
            valueRange = valueRange,
        )
    }
}