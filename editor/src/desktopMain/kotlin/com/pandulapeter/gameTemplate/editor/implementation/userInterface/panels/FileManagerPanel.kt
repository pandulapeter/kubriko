package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorIcon
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_new
import game.editor.generated.resources.ic_open
import game.editor.generated.resources.ic_save


@Composable
internal fun FileManagerPanel(
    onNewIconClicked: () -> Unit,
    onOpenIconClicked: () -> Unit,
    onSaveIconClicked: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = 8.dp,
            vertical = 4.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EditorIcon(
            drawableResource = Res.drawable.ic_new,
            contentDescription = "New",
            onClick = onNewIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_open,
            contentDescription = "Open",
            onClick = onOpenIconClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_save,
            contentDescription = "Save",
            onClick = onSaveIconClicked,
        )
    }
    Divider()
}