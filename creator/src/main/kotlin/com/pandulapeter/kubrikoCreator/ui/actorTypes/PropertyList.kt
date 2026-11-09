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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.BufferedTextField
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.MenuIconButton
import com.pandulapeter.kubrikoCreator.ui.components.NumberField
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.confirmingOnEnter
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_property
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.actor_type_inherited_property
import kubriko.creator.generated.resources.actor_type_properties
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_property
import kubriko.creator.generated.resources.property_initial_value
import kubriko.creator.generated.resources.property_name
import kubriko.creator.generated.resources.property_toggle_initially_on
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Properties of an Actor type: the values each of its Actors keeps, with the value they start with, as items of the
 * lazy list they are shown in, so that only the rows on screen are composed.
 *
 * @param propertyKeys The keys of [properties] (from [ListItemKeys]), so that a row keeps what is typed into it when
 * the rows above it change.
 * @param onChanged Called with a Property's current name and what it should become.
 */
internal fun LazyListScope.propertyListItems(
    properties: List<PropertyDefinition>,
    propertyKeys: List<Long>,
    emptyHint: StringResource,
    onAdded: (PropertyDefinition) -> Unit,
    onChanged: (name: String, PropertyDefinition) -> Unit,
    onRemoved: (name: String) -> Unit,
    inheritedProperties: List<PropertyDefinition> = emptyList(),
) {
    item {
        PanelHeader(
            icon = Res.drawable.ic_property,
            accent = Accent.PROPERTIES,
            title = stringResource(Res.string.actor_type_properties),
        ) {
            MenuIconButton(
                icon = Res.drawable.ic_add,
                label = Res.string.action_add_property,
                options = PropertyKind.entries,
                optionText = { stringResource(it.title) },
                optionIcon = { it.icon },
                onSelected = { kind -> onAdded(kind.create(PropertyNaming.freeName(kind.defaultName, (properties + inheritedProperties).map { it.name }))) },
            )
        }
    }
    itemsIndexed(inheritedProperties) { index, property ->
        Text(
            modifier = Modifier.propertyItemPadding(isFirst = index == 0),
            text = stringResource(Res.string.actor_type_inherited_property, property.name, stringResource(PropertyKind.of(property).title)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (properties.isEmpty() && inheritedProperties.isEmpty()) {
        item {
            Text(
                modifier = Modifier.propertyItemPadding(isFirst = true),
                text = stringResource(emptyHint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val names = properties.map { it.name }
    itemsIndexed(properties, key = { index, _ -> propertyKeys[index] }) { index, property ->
        PropertyRow(
            modifier = Modifier.propertyItemPadding(isFirst = index == 0 && inheritedProperties.isEmpty()),
            property = property,
            otherNames = names - property.name,
            onChange = { onChanged(property.name, it) },
            onRemove = { onRemoved(property.name) },
        )
    }
    item {
        Spacer(modifier = Modifier.height(PROPERTY_LIST_VERTICAL_PADDING))
    }
}

private fun Modifier.propertyItemPadding(isFirst: Boolean) = padding(
    start = 12.dp,
    end = 12.dp,
    top = if (isFirst) PROPERTY_LIST_VERTICAL_PADDING else PROPERTY_SPACING,
)

private val PROPERTY_LIST_VERTICAL_PADDING = 4.dp
private val PROPERTY_SPACING = 10.dp

@Composable
private fun PropertyRow(
    property: PropertyDefinition,
    otherNames: List<String>,
    onChange: (PropertyDefinition) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hover = rememberHoverRevealState()
    val menu = rememberRightClickMenuState()
    Column(
        modifier = modifier.hoverReveal(hover).rightClickMenu(menu),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(PropertyKind.of(property).icon),
                contentDescription = stringResource(PropertyKind.of(property).title),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            PropertyNameField(
                modifier = Modifier.weight(1f),
                name = property.name,
                otherNames = otherNames,
                onRename = { onChange(property.withName(it)) },
            )
            Box {
                IconActionButton(modifier = Modifier.revealedWhen(hover.isActive), icon = Res.drawable.ic_close, label = Res.string.action_remove, onClick = onRemove)
                RightClickMenu(menu, listOf(ContextAction(stringResource(Res.string.action_remove), Res.drawable.ic_close, isDestructive = true, onClick = onRemove)))
            }
        }
        Row(
            modifier = Modifier.padding(start = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (property) {
                is PropertyDefinition.Number -> NumberField(
                    modifier = Modifier.fillMaxWidth(),
                    label = Res.string.property_initial_value,
                    value = property.initialValue,
                    onValueChange = { onChange(property.copy(initialValue = it)) },
                )
                is PropertyDefinition.Text -> BufferedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = property.initialValue,
                    onValueChange = { onChange(property.copy(initialValue = it)) },
                    label = stringResource(Res.string.property_initial_value),
                )
                is PropertyDefinition.Toggle -> LabeledSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.property_toggle_initially_on),
                    checked = property.initialValue,
                    onCheckedChange = { onChange(property.copy(initialValue = it)) },
                )
            }
        }
    }
}

/**
 * Keeps whatever is being typed, and renames the Property once its name is done: on Enter, when the field loses the
 * focus, or when it leaves the screen. Renaming rewrites every Scene holding the type's Actors, so it never happens per
 * keystroke, and each name typed renames it once, though leaving the screen also loses the focus. Escape, or leaving the
 * field with a name the Property can't have, brings its name back.
 */
@Composable
private fun PropertyNameField(
    name: String,
    otherNames: List<String>,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(name) { mutableStateOf(name) }
    var renamedTo by remember(name) { mutableStateOf<String?>(null) }
    val finish by rememberUpdatedState {
        val newName = renamedPropertyName(text, name, otherNames)
        when {
            newName == null -> text = name
            newName != renamedTo -> {
                renamedTo = newName
                onRename(newName)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { finish() }
    }
    CompactTextField(
        modifier = modifier
            .onFocusChanged { if (!it.isFocused) finish() }
            .confirmingOnEnter(onConfirm = { finish() }, onCancel = { text = name }),
        value = text,
        onValueChange = { text = it },
        label = stringResource(Res.string.property_name),
        isError = PropertyNaming.validate(text, otherNames) != null,
    )
}

/**
 * What a Property called [name] is renamed to once [text] is done being typed: [text], if it differs and is a name the
 * Property can have beside [otherNames], or null.
 */
internal fun renamedPropertyName(text: String, name: String, otherNames: List<String>): String? =
    text.takeIf { it != name && PropertyNaming.validate(it, otherNames) == null }
