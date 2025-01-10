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
internal fun ColorSlider(
    modifier: Modifier = Modifier,
    title: String,
    red: Float,
    green: Float,
    blue: Float,
    onValueChanged: (Float, Float, Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
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
        value = red,
        onValueChanged = { onValueChanged(it, green, blue) },
        valueRange = valueRange,
    )
    ShaderSlider(
        modifier = Modifier.weight(1f),
        value = green,
        onValueChanged = { onValueChanged(red, it, blue) },
        valueRange = valueRange,
    )
    ShaderSlider(
        modifier = Modifier.weight(1f),
        value = blue,
        onValueChanged = { onValueChanged(red, green, it) },
        valueRange = valueRange,
    )
}