package com.pandulapeter.gameTemplate.editor.implementation.userInterface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject

@Composable
internal fun GameObjectTypeRadioButton(
    gameObjectType: Class<out GameObject>,
    selectedGameObjectType: Class<out GameObject>,
    onSelected: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onSelected),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    RadioButton(
        selected = gameObjectType == selectedGameObjectType,
        onClick = onSelected,
    )
    Text(
        text = gameObjectType.simpleName,
    )
}

@Composable
internal fun ClickableText(
    text: String,
    onClick: () -> Unit
) = OutlinedButton(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    onClick = onClick,
) {
    Text(
        text = text
    )
}

@Composable
internal fun PropertyEditorSection(
    title: String,
    controls: @Composable () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    val isExpanded = remember { mutableStateOf(false) }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded.value = !isExpanded.value }
            .padding(8.dp),
        style = MaterialTheme.typography.subtitle1,
        text = title,
    )
    if (isExpanded.value) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            controls()
        }
    }
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