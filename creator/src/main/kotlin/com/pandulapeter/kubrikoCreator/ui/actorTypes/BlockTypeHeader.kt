/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.ui.NewNameField
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.AccentIcon
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.action_delete_manager_type
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.action_rename_actor_type
import kubriko.creator.generated.resources.action_rename_manager_type
import kubriko.creator.generated.resources.actor_type_children
import kubriko.creator.generated.resources.actor_type_children_more
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.new_actor_type_hint
import kubriko.creator.generated.resources.new_manager_type_hint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The top of a type's editor, on one line: its name, renamed in place, and for an Actor type the type it is based on
 * and the ones based on it (a click opens one), then deleting it.
 *
 * @param onRename Renames the type, or returns why the name can't be used.
 * @param onTypeSelected Opens another type's editor.
 */
@Composable
internal fun BlockTypeHeader(
    definition: BlockTypeDefinition,
    inheritance: Inheritance,
    flashKey: Any?,
    onRename: (String) -> StringResource?,
    onParentChange: (String?) -> Unit,
    onTypeSelected: (String) -> Unit,
    onDelete: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .background(rememberFlashColor(flashKey))
        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    var isRenaming by remember(definition.name) { mutableStateOf(false) }
    AccentIcon(
        icon = definition.kind.icon,
        accent = definition.kind.accent,
        size = 32.dp,
    )
    if (isRenaming) {
        Box(
            modifier = Modifier.width(320.dp).padding(top = 4.dp),
        ) {
            NewNameField(
                placeholder = if (definition is ManagerTypeDefinition) Res.string.new_manager_type_hint else Res.string.new_actor_type_hint,
                initialName = definition.name,
                indent = 0.dp,
                confirmLabel = Res.string.action_rename,
                onCreate = { name -> if (name == definition.name) null.also { isRenaming = false } else onRename(name).also { if (it == null) isRenaming = false } },
                onCancel = { isRenaming = false },
            )
        }
    } else {
        Text(
            text = definition.name,
            style = MaterialTheme.typography.titleLarge,
        )
        IconActionButton(
            icon = Res.drawable.ic_edit,
            label = if (definition is ManagerTypeDefinition) Res.string.action_rename_manager_type else Res.string.action_rename_actor_type,
            onClick = { isRenaming = true },
        )
    }
    if (definition is ActorTypeDefinition) {
        HeaderDivider()
        ParentChoice(
            parent = definition.parent,
            choices = inheritance.parentChoices,
            onParentChange = onParentChange,
        )
        if (inheritance.children.isNotEmpty()) {
            HeaderDivider()
            ChildTypes(
                children = inheritance.children,
                onTypeSelected = onTypeSelected,
            )
        }
    }
    Spacer(Modifier.weight(1f))
    IconActionButton(
        icon = Res.drawable.ic_delete,
        label = if (definition is ManagerTypeDefinition) Res.string.action_delete_manager_type else Res.string.action_delete_actor_type,
        onClick = onDelete,
    )
}

@Composable
private fun HeaderDivider() = VerticalDivider(
    modifier = Modifier.height(20.dp).padding(horizontal = 4.dp),
)

/**
 * The Actor types based on this one as links, the first few by name and the rest in a menu.
 */
@Composable
private fun ChildTypes(
    children: List<String>,
    onTypeSelected: (String) -> Unit,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
) {
    Text(
        modifier = Modifier.padding(end = 2.dp),
        text = stringResource(Res.string.actor_type_children),
        style = MaterialTheme.typography.labelMedium,
    )
    children.take(MAXIMUM_SHOWN_CHILDREN).forEach { child ->
        TextButton(
            modifier = Modifier.heightIn(max = 32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            onClick = { onTypeSelected(child) },
        ) {
            Text(
                text = child,
                color = Accent.CAST.color,
            )
        }
    }
    val rest = children.drop(MAXIMUM_SHOWN_CHILDREN)
    if (rest.isNotEmpty()) {
        MenuButton(
            label = stringResource(Res.string.actor_type_children_more, rest.size),
            options = rest,
            optionText = { it },
            onSelected = onTypeSelected,
        )
    }
}

private const val MAXIMUM_SHOWN_CHILDREN = 3
