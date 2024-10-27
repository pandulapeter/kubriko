package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ColorfulPropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.RotatablePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ScalablePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.VisiblePropertyEditors
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_delete
import game.editor.generated.resources.ic_locate
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun GameObjectManagerPanel(
    data: Pair<GameObject?, Boolean>,
    selectedGameObjectType: Class<out GameObject>
) {
    val isColorfulExpanded = remember { mutableStateOf(false) }
    val isRotatableExpanded = remember { mutableStateOf(false) }
    val isScalableExpanded = remember { mutableStateOf(false) }
    val isVisibleExpanded = remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .width(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        data.first.let { gameObject ->
            if (gameObject == null) {
                items(
                    items = EditorController.supportedGameObjectTypes.keys.toList(),
                    key = { "typeRadioButton_${it.name}" },
                ) { gameObjectType ->
                    EditorRadioButton(
                        label = gameObjectType.simpleName,
                        isSelected = gameObjectType == selectedGameObjectType,
                        onSelectionChanged = { EditorController.selectGameObjectType(gameObjectType) },
                    )
                }
            } else {
                item(key = "selectedTypeTitle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditorTextTitle(
                            modifier = Modifier.weight(1f),
                            text = gameObject::class.java.simpleName,
                        )
                        Icon(
                            modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = EditorController::locateGameObject).padding(4.dp),
                            painter = painterResource(Res.drawable.ic_locate),
                            contentDescription = "Locate"
                        )
                        Icon(
                            modifier = Modifier.size(24.dp).clip(CircleShape).clickable(onClick = EditorController::deleteGameObject).padding(4.dp),
                            painter = painterResource(Res.drawable.ic_delete),
                            contentDescription = "Delete"
                        )
                    }
                    Divider()
                }
                if (gameObject is Colorful) {
                    item(key = "propertyColorful") {
                        ColorfulPropertyEditors(
                            data = gameObject to data.second,
                            isExpanded = isColorfulExpanded.value,
                            onExpandedChanged = { isColorfulExpanded.value = !isColorfulExpanded.value }
                        )
                    }
                }
                if (gameObject is Rotatable) {
                    item(key = "propertyRotatable") {
                        RotatablePropertyEditors(
                            data = gameObject to data.second,
                            isExpanded = isRotatableExpanded.value,
                            onExpandedChanged = { isRotatableExpanded.value = !isRotatableExpanded.value }
                        )
                    }
                }
                if (gameObject is Scalable) {
                    item(key = "propertyScalable") {
                        ScalablePropertyEditors(
                            data = gameObject to data.second,
                            isExpanded = isScalableExpanded.value,
                            onExpandedChanged = { isScalableExpanded.value = !isScalableExpanded.value }
                        )
                    }
                }
                if (gameObject is Visible) {
                    item(key = "propertyVisible") {
                        VisiblePropertyEditors(
                            data = gameObject to data.second,
                            isExpanded = isVisibleExpanded.value,
                            onExpandedChanged = { isVisibleExpanded.value = !isVisibleExpanded.value }
                        )
                    }
                }
            }
        }
    }
}