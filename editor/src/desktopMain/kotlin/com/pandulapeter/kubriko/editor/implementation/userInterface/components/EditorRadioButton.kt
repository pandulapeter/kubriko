package com.pandulapeter.kubriko.editor.implementation.userInterface.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorRadioButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
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
        modifier = Modifier.size(16.dp),
        selected = isSelected,
        onClick = onSelectionChanged,
    )
    EditorTextTitle(
        modifier = Modifier.wrapContentHeight(),
        text = label,
    )
}