package com.pandulapeter.gameTemplate.editor.implementation.userInterface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun ClickableText(
    text: String,
    onClick: () -> Unit
) = Text(
    style = MaterialTheme.typography.subtitle1,
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
    text = text
)

@Composable
internal fun PropertyEditorSection(
    title: String,
    controls: @Composable () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
) {
    Text(
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.subtitle1,
        text = title,
    )
    controls()
    Divider()
}

@Composable
internal fun SliderWithTitle(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
) {
    PropertyTitle(
        text = "$title: ${"%.2f".format(value)}",
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
    )
}

@Composable
internal fun PropertyTitle(
    text: String,
) = Text(
    style = MaterialTheme.typography.caption,
    text = text,
)