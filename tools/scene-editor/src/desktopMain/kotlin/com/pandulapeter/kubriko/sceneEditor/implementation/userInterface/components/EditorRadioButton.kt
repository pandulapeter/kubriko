package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorRadioButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    isSmall: Boolean = false,
    onSelectionChanged: () -> Unit,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .clickable(onClick = onSelectionChanged).padding(
            horizontal = 8.dp,
            vertical = 2.dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    RadioButton(
        modifier = Modifier.scale(if (isSmall) 0.5f else 0.75f).size(if (isSmall) 12.dp else 16.dp),
        selected = isSelected,
        onClick = onSelectionChanged,
    )
    if (isSmall) {
        EditorText(
            text = label,
        )
    } else {
        EditorTextTitle(
            text = label,
        )
    }
}