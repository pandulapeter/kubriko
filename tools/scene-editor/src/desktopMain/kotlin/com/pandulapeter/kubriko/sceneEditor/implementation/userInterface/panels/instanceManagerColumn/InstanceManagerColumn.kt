/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextTitle
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors.createBodyPropertyEditor
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.AngleEditorMode
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_close
import kubriko.tools.scene_editor.generated.resources.ic_delete
import kubriko.tools.scene_editor.generated.resources.ic_locate
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

@Composable
internal fun InstanceManagerColumn(
    modifier: Modifier = Modifier,
    registeredTypeIds: List<String>,
    selectedTypeId: String?,
    selectedUpdatableInstance: Pair<Editable<*>?, Boolean>,
    resolveTypeId: (KClass<out Editable<*>>) -> String?,
    selectTypeId: (String) -> Unit,
    deselectSelectedInstance: () -> Unit,
    locateSelectedInstance: () -> Unit,
    deleteSelectedInstance: () -> Unit,
    notifySelectedInstanceUpdate: () -> Unit,
    colorEditorMode: ColorEditorMode,
    angleEditorMode: AngleEditorMode,
) = EditorSurface(
    modifier = modifier,
) {
    selectedUpdatableInstance.first.let { selectedInstance ->
        Column {
            if (selectedInstance != null) {
                SelectedInstanceHeader(
                    instanceTypeName = resolveTypeId(selectedInstance::class) ?: "Unknown Actor type",
                    onDeselectClicked = deselectSelectedInstance,
                    onLocateClicked = locateSelectedInstance,
                    onDeleteClicked = deleteSelectedInstance,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                    // TODO: Sort into categories using expandedCategories.value
                    selectedInstance::class.memberProperties
                        .filterIsInstance<KMutableProperty<*>>()
                        .sortedBy { it.name }
                        .mapNotNull { property ->
                            property.toPropertyEditor(
                                actor = selectedInstance,
                                notifySelectedInstanceUpdate = notifySelectedInstanceUpdate,
                                colorEditorMode = colorEditorMode,
                                angleEditorMode = angleEditorMode,
                            )
                        }
                        .let { controls ->
                            val allControls = controls.toMutableList().apply {
                                add(
                                    index = 0,
                                    element = createBodyPropertyEditor(
                                        getActor = { selectedInstance },
                                        notifySelectedInstanceUpdate = notifySelectedInstanceUpdate,
                                        angleEditorMode = angleEditorMode,
                                    )
                                )
                            }
                            item(key = "actorEditor") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    allControls.forEachIndexed { index, lambda ->
                                        lambda()
                                        if (index != allControls.lastIndex) {
                                            HorizontalDivider()
                                        }
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
            drawableResource = Res.drawable.ic_locate,
            contentDescription = "Locate",
            onClick = onLocateClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_delete,
            contentDescription = "Delete",
            onClick = onDeleteClicked,
        )
        EditorIcon(
            drawableResource = Res.drawable.ic_close,
            contentDescription = "Deselect",
            onClick = onDeselectClicked,
        )
    }
    HorizontalDivider()
}