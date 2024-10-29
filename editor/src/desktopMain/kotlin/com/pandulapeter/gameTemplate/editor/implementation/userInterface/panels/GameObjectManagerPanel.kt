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
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.GenericTraitEditor
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_delete
import game.editor.generated.resources.ic_locate
import kotlin.reflect.KClass

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
        val registeredTypeIds = Engine.get().gameObjectManager.registeredTypeIdsForEditor.collectAsState()
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        val expandedTraitTypes = remember { mutableStateOf(emptySet<KClass<out Trait<*>>>()) }
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
                    gameObject.allTraits
                        .forEach { entry ->
                            (entry.value::class.annotations
                                .firstOrNull { it.annotationClass == VisibleInEditor::class } as? VisibleInEditor)
                                ?.let { editableTrait ->
                                    val isExpanded = expandedTraitTypes.value.contains(entry.key)
                                    val onExpandedChanged = {
                                        expandedTraitTypes.value = if (isExpanded) {
                                            expandedTraitTypes.value.filterNot { it == entry.key }
                                        } else {
                                            (expandedTraitTypes.value + entry.key)
                                        }.toSet()
                                    }
                                    item(key = "traitEditor_${editableTrait.typeId}") {
                                        GenericTraitEditor(
                                            data = entry.value to data.second,
                                            visibleInEditor = editableTrait,
                                            isExpanded = isExpanded,
                                            onExpandedChanged = onExpandedChanged
                                        )
                                    }
                                }
                        }
                }
            }
        }
    }
}