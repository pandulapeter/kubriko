package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun EditorNumberInput(
    modifier: Modifier = Modifier,
    name: String,
    suffix: String = "",
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>? = null,
    enabled: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
    ) {
        EditorTextInput(
            title = name,
            value = "%.2f".format(value) + suffix,
            onValueChanged = { newValue ->
                newValue.toFloatOrNull()?.let {
                    onValueChanged(if (valueRange == null) it else min(valueRange.endInclusive, max(valueRange.start, it)))
                }
            },
            enabled = enabled,
            extraContent = extraContent,
        )
        EditorSlider(
            value = value,
            onValueChanged = onValueChanged,
            valueRange = valueRange,
        )
    }
}