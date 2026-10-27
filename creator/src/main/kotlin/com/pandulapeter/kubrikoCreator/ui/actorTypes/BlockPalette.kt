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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_palette
import kubriko.creator.generated.resources.block_palette_drag_hint
import kubriko.creator.generated.resources.block_palette_hint
import kubriko.creator.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Blocks the selected Event can use; clicking one adds it to the end of the Event.
 */
@Composable
internal fun BlockPalette(
    eventType: EventType?,
    dragState: BlockDragState,
    onBlockKindSelected: (BlockKind) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    var category by rememberSaveable { mutableStateOf(BlockCategory.MOVEMENT) }
    PanelHeader(
        icon = Res.drawable.ic_add,
        title = stringResource(Res.string.block_palette),
    )
    if (eventType == null) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = stringResource(Res.string.block_palette_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return@Column
    }
    CategoryTabs(
        selected = category,
        onSelected = { category = it },
    )
    Text(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
        text = stringResource(category.title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        category.kinds.filter { it.isAvailableIn(eventType) }.forEach { kind ->
            PaletteEntry(
                kind = kind,
                dragState = dragState,
                onClick = { if (!dragState.isClickAfterDrag) onBlockKindSelected(kind) },
            )
        }
    }
    Text(
        modifier = Modifier.padding(12.dp),
        text = stringResource(Res.string.block_palette_drag_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CategoryTabs(
    selected: BlockCategory,
    onSelected: (BlockCategory) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    BlockCategory.entries.forEach { category ->
        val isSelected = category == selected
        IconActionButton(
            modifier = Modifier.clip(MaterialTheme.shapes.small).background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
            icon = category.icon,
            label = category.title,
            onClick = { onSelected(category) },
            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A Block of the palette: a click adds it at the end of the selected Event, a drag puts it where it is dropped.
 */
@Composable
private fun PaletteEntry(
    kind: BlockKind,
    dragState: BlockDragState,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 32.dp)
        .blockDragSource(dragState) { DraggedBlock.New(kind) }
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(kind.icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
    )
    Text(
        text = stringResource(kind.title),
        style = MaterialTheme.typography.bodyMedium,
    )
}
