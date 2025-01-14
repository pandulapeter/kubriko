package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.TextInput

@Composable
internal fun EditorTextInput(
    modifier: Modifier = Modifier,
    title: String? = null,
    hint: String? = null,
    value: String,
    onValueChanged: (String) -> Unit,
    enabled: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null,
) = Column(
    modifier = modifier,
) {
    if (!title.isNullOrBlank()) {
        EditorTextLabel(
            text = title,
        )
    }
    Row(
        modifier = Modifier.defaultMinSize(minHeight = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            TextInput(
                value = value,
                enabled = enabled,
                onValueChanged = onValueChanged,
            )
            if (!hint.isNullOrBlank() && value.isEmpty()) {
                EditorText(
                    modifier = Modifier.alpha(0.5f),
                    text = hint,
                )
            }
        }
        extraContent?.invoke()
    }
}