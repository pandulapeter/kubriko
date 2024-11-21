package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        modifier = Modifier.defaultMinSize(minWidth = 48.dp),
        style = MaterialTheme.typography.labelSmall,
        text = title,
    )
    Slider(
        modifier = Modifier.weight(1f).height(24.dp),
        value = red,
        onValueChange = { onValueChanged(it, green, blue) },
        valueRange = valueRange,
    )
    Slider(
        modifier = Modifier.weight(1f).height(24.dp),
        value = green,
        onValueChange = { onValueChanged(red, it, blue) },
        valueRange = valueRange,
    )
    Slider(
        modifier = Modifier.weight(1f).height(24.dp),
        value = blue,
        onValueChange = { onValueChanged(red, green, it) },
        valueRange = valueRange,
    )
}