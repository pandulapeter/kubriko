package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.kubriko.sceneSerializer.Editable
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_close
import kubriko.tools.scene_editor.generated.resources.ic_delete
import kubriko.tools.scene_editor.generated.resources.ic_locate
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

@Composable
internal fun InstanceManagerColumn(
    registeredTypeIds: List<String>,
    selectedTypeId: String?,
    selectedUpdatableInstance: Pair<Editable<*>?, Boolean>,
    resolveTypeId: (KClass<out Editable<*>>) -> String?,
    selectTypeId: (String) -> Unit,
    deselectSelectedInstance: () -> Unit,
    locateSelectedInstance: () -> Unit,
    deleteSelectedInstance: () -> Unit,
    notifySelectedInstanceUpdate: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxHeight()
        .width(200.dp),
) {
    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        selectedUpdatableInstance.first.let { selectedInstance ->
            if (selectedInstance == null) {
                items(
                    items = registeredTypeIds,
                    key = { "typeRadioButton_${it}" },
                ) { typeId ->
                    EditorRadioButton(
                        label = typeId,
                        isSelected = typeId == selectedTypeId,
                        onSelectionChanged = { selectTypeId(typeId) },
                    )
                }
            } else {
                item(key = "selectedTypeHeader") {
                    SelectedInstanceHeader(
                        instanceTypeName = resolveTypeId(selectedInstance::class) ?: "Unknown Actor type",
                        onDeselectClicked = deselectSelectedInstance,
                        onLocateClicked = locateSelectedInstance,
                        onDeleteClicked = deleteSelectedInstance,
                    )
                }
                // TODO: Sort into categories using expandedCategories.value
                selectedInstance::class.memberProperties
                    .filterIsInstance<KMutableProperty<*>>()
                    .sortedBy { it.name }
                    .mapNotNull { property ->
                        property.toPropertyEditor(
                            actor = selectedInstance,
                            notifySelectedInstanceUpdate = notifySelectedInstanceUpdate,
                        )
                    }
                    .let { controls ->
                        if (controls.isNotEmpty()) {
                            item(key = "actorEditor") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
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
            }
        }
    }
}

@Composable
private fun SelectedInstanceHeader(
    instanceTypeName: String,
    onDeselectClicked: () -> Unit,
    onLocateClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
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
    ) {
        EditorTextTitle(
            modifier = Modifier.weight(1f),
            text = instanceTypeName,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_close,
            contentDescription = "Deselect",
            onClick = onDeselectClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_locate,
            contentDescription = "Locate",
            onClick = onLocateClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_delete,
            contentDescription = "Delete",
            onClick = onDeleteClicked,
        )
    }
    Divider()
}