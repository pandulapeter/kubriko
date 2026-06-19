/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceBrowserColumn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSurface
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorTextInput
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.clear_filter
import kubriko.tools.scene_editor.generated.resources.filter_hint
import kubriko.tools.scene_editor.generated.resources.ic_close
import kubriko.tools.scene_editor.generated.resources.ic_visible_only_off
import kubriko.tools.scene_editor.generated.resources.ic_visible_only_on
import kubriko.tools.scene_editor.generated.resources.named_instance
import kubriko.tools.scene_editor.generated.resources.toggle_visible_only
import kubriko.tools.scene_editor.generated.resources.unique_prefix
import kubriko.tools.scene_editor.generated.resources.unknown_actor_type
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

@Composable
internal fun InstanceBrowserColumn(
    modifier: Modifier = Modifier,
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
    shouldShowVisibleOnly: Boolean,
    allInstances: List<Editable<*>>,
    visibleInstances: List<Editable<*>>,
    selectedUpdatableInstance: Pair<Editable<*>?, Boolean>,
    onShouldShowVisibleOnlyToggled: () -> Unit,
    selectInstance: (Editable<*>) -> Unit,
    resolveTypeId: (KClass<out Editable<*>>) -> String?,
) = EditorSurface(
    modifier = modifier,
) {
    Column {
        HeaderRow(
            filterText = filterText,
            onFilterTextChanged = onFilterTextChanged,
            shouldShowVisibleOnly = shouldShowVisibleOnly,
            onShouldShowVisibleOnlyToggled = onShouldShowVisibleOnlyToggled,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(if (shouldShowVisibleOnly) visibleInstances else allInstances) { instance ->
                EditorText(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            color = if (instance == selectedUpdatableInstance.first) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        ).clickable { selectInstance(instance) }.padding(
                            horizontal = 8.dp,
                            vertical = 2.dp,
                        ),
                    color = if (instance == selectedUpdatableInstance.first) contentColorFor(MaterialTheme.colorScheme.primaryContainer) else LocalContentColor.current,
                    text = instance.getName(resolveTypeId(instance::class)),
                )
            }
        }
    }
}

@Composable
private fun Editable<*>.getName(typeId: String?): String {
    val type = typeId ?: stringResource(Res.string.unknown_actor_type)
    val prefixedType = if (this is Unique) stringResource(Res.string.unique_prefix, type) else type
    val id = (this as? Identifiable)?.name
    return if (id == null) prefixedType else stringResource(Res.string.named_instance, prefixedType, id)
}

@Composable
private fun HeaderRow(
    filterText: String,
    onFilterTextChanged: (String) -> Unit,
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
    ) {
        EditorTextInput(
            modifier = Modifier.weight(1f),
            value = filterText,
            hint = stringResource(Res.string.filter_hint),
            onValueChanged = onFilterTextChanged,
        )
        if (filterText.isNotEmpty()) {
            EditorIcon(
                drawableResource = Res.drawable.ic_close,
                contentDescription = stringResource(Res.string.clear_filter),
                onClick = { onFilterTextChanged("") },
            )
        }
        EditorIcon(
            drawableResource = if (shouldShowVisibleOnly) Res.drawable.ic_visible_only_on else Res.drawable.ic_visible_only_off,
            contentDescription = stringResource(Res.string.toggle_visible_only),
            onClick = onShouldShowVisibleOnlyToggled,
        )
    }
    HorizontalDivider()
}