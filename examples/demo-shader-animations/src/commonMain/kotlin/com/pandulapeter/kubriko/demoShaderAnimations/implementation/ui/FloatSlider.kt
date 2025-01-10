package com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.ShaderSlider

@Composable
internal fun FloatSlider(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChanged: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        modifier = Modifier.defaultMinSize(minWidth = 42.dp),
        style = MaterialTheme.typography.labelSmall,
        text = title,
    )
    ShaderSlider(
        modifier = Modifier.weight(1f),
        value = value,
        onValueChanged = onValueChanged,
        valueRange = valueRange,
    )
}