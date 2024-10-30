package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels

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
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.toEditorControl
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.editor.Editable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import game.editor.generated.resources.Res
import game.editor.generated.resources.ic_close
import game.editor.generated.resources.ic_delete
import game.editor.generated.resources.ic_locate
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Composable
internal fun GameObjectManagerPanel(
    data: Pair<AvailableInEditor<*>?, Boolean>,
    selectedGameObjectTypeId: String?
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .width(200.dp),
    ) {
        val registeredTypeIds = Engine.get().gameObjectManager.registeredTypeIdsForEditor.collectAsState()
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        val expandedCategories = EditorController.expandedCategories.collectAsState()
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
                                text = Engine.get().gameObjectManager.getTypeId(gameObject::class),
                            )
                            EditorIcon(
                                drawableResource = Res.drawable.ic_close,
                                contentDescription = "Unselect",
                                onClick = EditorController::unselectGameObject,
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
                    // TODO: Sort into categories using expandedCategories.value
                    gameObject::class.memberProperties
                        .filterIsInstance<KMutableProperty<*>>()
                        .sortedBy { it.name }
                        .mapNotNull { property -> property.toEditorControl(gameObject) }
                        .let { controls ->
                            if (controls.isNotEmpty()) {
                                item(key = "gameObjectEditor") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp,
                                            ),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        controls.forEach {
                                            it.invoke()
                                            Divider()
                                        }
                                    }
                                }
                            }
                        }

//                    gameObject.allTraits
//                        .forEach { entry ->
//                            (entry.value::class.annotations
//                                .firstOrNull { it.annotationClass == VisibleInEditor::class } as? VisibleInEditor)
//                                ?.let { editableTrait ->
//                                    val isExpanded = expandedCategories.value.contains(entry.key)
//                                    val onExpandedChanged = {
//                                        expandedCategories.value = if (isExpanded) {
//                                            expandedCategories.value.filterNot { it == entry.key }
//                                        } else {
//                                            (expandedCategories.value + entry.key)
//                                        }.toSet()
//                                    }
//                                    item(key = "traitEditor_${editableTrait.typeId}") {
//                                        GenericTraitEditor(
//                                            data = entry.value to data.second,
//                                            visibleInEditor = editableTrait,
//                                            isExpanded = isExpanded,
//                                            onExpandedChanged = onExpandedChanged
//                                        )
//                                    }
//                                }
//                        }
                }
            }
        }
    }
}