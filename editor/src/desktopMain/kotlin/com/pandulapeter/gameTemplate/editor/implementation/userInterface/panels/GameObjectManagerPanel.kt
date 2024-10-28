package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ColorfulTraitEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.RotatableTraitEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.ScalableTraitEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.UniqueTraitEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.VisibleTraitEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_delete
import game.editor.generated.resources.ic_locate

@Composable
internal fun GameObjectManagerPanel(
    data: Pair<GameObject<*>?, Boolean>,
    selectedGameObjectTypeId: String?
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .width(200.dp),
    ) {
        val registeredTypeIds = Engine.get().gameObjectManager.registeredTypeIds.collectAsState()
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        val isColorfulExpanded = remember { mutableStateOf(false) }
        val isRotatableExpanded = remember { mutableStateOf(false) }
        val isScalableExpanded = remember { mutableStateOf(false) }
        val isVisibleExpanded = remember { mutableStateOf(false) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            data.first.let { gameObject ->
                if (gameObject == null) {
                    items(
                        items = registeredTypeIds.value,
                        key = { "typeRadioButton_${it}" },
                    ) { gameObjectType ->
                        EditorRadioButton(
                            label = gameObjectType,
                            isSelected = gameObjectType == selectedGameObjectTypeId,
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
                                    vertical = 2.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EditorTextTitle(
                                modifier = Modifier.weight(1f),
                                text = gameObject::class.java.simpleName,
                            )
                            EditorIcon(
                                drawableResource = Res.drawable.ic_locate,
                                contentDescription = "Locate",
                                onClick = EditorController::locateGameObject,
                            )
                            EditorIcon(
                                drawableResource = Res.drawable.ic_delete,
                                contentDescription = "Delete",
                                onClick = EditorController::deleteSelectedGameObject,
                            )
                        }
                        Divider()
                    }
                    if (gameObject is Colorful) {
                        item(key = "traitColorful") {
                            ColorfulTraitEditor(
                                data = gameObject to data.second,
                                isExpanded = isColorfulExpanded.value,
                                onExpandedChanged = { isColorfulExpanded.value = !isColorfulExpanded.value }
                            )
                        }
                    }
                    if (gameObject is Rotatable) {
                        item(key = "traitRotatable") {
                            RotatableTraitEditor(
                                data = gameObject to data.second,
                                isExpanded = isRotatableExpanded.value,
                                onExpandedChanged = { isRotatableExpanded.value = !isRotatableExpanded.value }
                            )
                        }
                    }
                    if (gameObject is Scalable) {
                        item(key = "traitScalable") {
                            ScalableTraitEditor(
                                data = gameObject to data.second,
                                isExpanded = isScalableExpanded.value,
                                onExpandedChanged = { isScalableExpanded.value = !isScalableExpanded.value }
                            )
                        }
                    }
                    if (gameObject is Unique) {
                        item(key = "traitUnique") {
                            UniqueTraitEditor()
                        }
                    }
                    if (gameObject is Visible) {
                        item(key = "traitVisible") {
                            VisibleTraitEditor(
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
}