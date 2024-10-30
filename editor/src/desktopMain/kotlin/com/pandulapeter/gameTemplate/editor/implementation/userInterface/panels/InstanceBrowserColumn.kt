package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorText
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_visible_only_off
import game.editor.generated.resources.ic_visible_only_on

@Composable
internal fun InstanceBrowserColumn(
    allGameObjects: List<AvailableInEditor<*>>,
    visibleGameObjects: List<AvailableInEditor<*>>,
) = Row(
    modifier = Modifier
        .fillMaxHeight()
        .width(150.dp),
) {
    Column(
        modifier = Modifier.weight(1f),
    ) {
        val shouldShowVisibleOnly = EditorController.shouldShowVisibleOnly.collectAsState()
        HeaderRow(
            shouldShowVisibleOnly = shouldShowVisibleOnly.value,
            onShouldShowVisibleOnlyToggled = EditorController::onShouldShowVisibleOnlyToggled,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(if (shouldShowVisibleOnly.value) visibleGameObjects else allGameObjects) { gameObject ->
                EditorText(
                    modifier = Modifier.fillMaxWidth().clickable { EditorController.selectGameObject(gameObject) }.padding(
                        horizontal = 8.dp,
                        vertical = 2.dp,
                    ),
                    text = Engine.get().instanceManager.getTypeId(gameObject::class),
                    isBold = gameObject.isSelectedInEditor,
                )
            }
        }
    }
    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
}

@Composable
private fun HeaderRow(
    shouldShowVisibleOnly: Boolean,
    onShouldShowVisibleOnlyToggled: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 2.dp,
            )
            .padding(
                start = 8.dp,
                end = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        EditorIcon(
            drawableResource = if (shouldShowVisibleOnly) Res.drawable.ic_visible_only_on else Res.drawable.ic_visible_only_off,
            contentDescription = "Toggle visible only",
            onClick = onShouldShowVisibleOnlyToggled,
        )
    }
    Divider()
}