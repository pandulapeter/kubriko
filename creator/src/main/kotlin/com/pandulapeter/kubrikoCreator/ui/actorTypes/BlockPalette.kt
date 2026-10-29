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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.accentEdge
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_palette
import kubriko.creator.generated.resources.block_palette_drag_hint
import kubriko.creator.generated.resources.block_palette_hint
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_block
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Blocks the selected Event of a type of [typeKind] can use; clicking one adds it to the end of the Event.
 */
@Composable
internal fun BlockPalette(
    eventType: EventType?,
    typeKind: BlockTypeKind,
    dragState: BlockDragState,
    onBlockKindSelected: (BlockKind) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    var requestedCategory by rememberSaveable { mutableStateOf(BlockCategory.MOVEMENT) }
    PanelHeader(
        icon = Res.drawable.ic_block,
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
    val categories = BlockCategory.availableIn(eventType, typeKind)
    val category = requestedCategory.takeIf { it in categories } ?: categories.first()
    CategoryTabs(
        categories = categories,
        selected = category,
        onSelected = { requestedCategory = it },
    )
    Text(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
        text = stringResource(category.title),
        style = MaterialTheme.typography.labelLarge,
        color = category.accent.color,
    )
    val kinds = remember(category, eventType, typeKind) { category.kinds.filter { it.isAvailableIn(eventType, typeKind) } }
    LazyColumn(
        modifier = Modifier.weight(1f, fill = false),
    ) {
        items(kinds) { kind ->
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
    categories: List<BlockCategory>,
    selected: BlockCategory,
    onSelected: (BlockCategory) -> Unit,
) = FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
    horizontalArrangement = Arrangement.Start,
) {
    categories.forEach { category ->
        val isSelected = category == selected
        IconActionButton(
            modifier = Modifier.clip(MaterialTheme.shapes.small).background(if (isSelected) category.accent.container else Color.Transparent),
            icon = category.icon,
            label = category.title,
            onClick = { onSelected(category) },
            tint = if (isSelected) category.accent.color else category.accent.color.copy(alpha = 0.6f),
        )
    }
}

/**
 * A Block of the palette, a chip in its category's color like the card it becomes: a click adds it at the end of the
 * selected Event, a drag puts it where it is dropped.
 */
@Composable
private fun PaletteEntry(
    kind: BlockKind,
    dragState: BlockDragState,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(MaterialTheme.shapes.small)
        .background(kind.category.accent.container)
        .accentEdge(kind.category.accent.color)
        .heightIn(min = 32.dp)
        .listDragSource(dragState) { DraggedBlock.New(kind) }
        .clickable(onClick = onClick)
        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(kind.icon),
        contentDescription = null,
        tint = kind.category.accent.color,
    )
    Text(
        text = stringResource(kind.title),
        style = MaterialTheme.typography.bodyMedium,
    )
}
