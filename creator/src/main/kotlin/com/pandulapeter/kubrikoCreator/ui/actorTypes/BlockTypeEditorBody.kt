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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditorColumns.Folded
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_events
import kubriko.creator.generated.resources.block_palette
import kubriko.creator.generated.resources.type_editor_details
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The type editor's columns as [columns] arranges them: side by side when there is room, otherwise with the [details]
 * (and then the [palette]) as tabs beside the [events], so the selected Event's [blocks] always keep theirs. The tab
 * shown is kept per type ([typeName]).
 */
@Composable
internal fun BlockTypeEditorBody(
    columns: BlockTypeEditorColumns,
    typeName: String,
    details: @Composable (Modifier) -> Unit,
    events: @Composable (Modifier) -> Unit,
    blocks: @Composable (Modifier) -> Unit,
    palette: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier,
) {
    if (columns.folded == Folded.NONE) {
        details(Modifier.width(columns.properties).fillMaxHeight())
        VerticalDivider()
        events(Modifier.width(columns.events).fillMaxHeight())
    } else {
        TabbedColumn(
            modifier = Modifier.width(columns.events).fillMaxHeight(),
            typeName = typeName,
            tabs = if (columns.folded == Folded.DETAILS) EditorTab.entries - EditorTab.PALETTE else EditorTab.entries,
        ) { tab, tabModifier ->
            when (tab) {
                EditorTab.EVENTS -> events(tabModifier)
                EditorTab.DETAILS -> details(tabModifier)
                EditorTab.PALETTE -> palette(tabModifier)
            }
        }
    }
    VerticalDivider()
    blocks(Modifier.weight(1f).fillMaxHeight())
    if (columns.folded != Folded.DETAILS_AND_PALETTE) {
        VerticalDivider()
        palette(Modifier.width(columns.palette).fillMaxHeight())
    }
}

private enum class EditorTab(val title: StringResource) {
    EVENTS(Res.string.actor_type_events),
    DETAILS(Res.string.type_editor_details),
    PALETTE(Res.string.block_palette),
}

@Composable
private fun TabbedColumn(
    typeName: String,
    tabs: List<EditorTab>,
    modifier: Modifier = Modifier,
    content: @Composable (EditorTab, Modifier) -> Unit,
) = Column(
    modifier = modifier,
) {
    var selected by remember(typeName) { mutableStateOf(EditorTab.EVENTS) }
    val shown = selected.takeIf { it in tabs } ?: EditorTab.EVENTS
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
    ) {
        tabs.forEach { tab ->
            EditorTabButton(
                title = stringResource(tab.title),
                isSelected = tab == shown,
                onClick = { selected = tab },
            )
        }
    }
    HorizontalDivider()
    AnimatedContent(
        modifier = Modifier.fillMaxWidth().weight(1f),
        targetState = shown,
        transitionSpec = { fadeIn(CreatorMotion.medium()) togetherWith fadeOut(CreatorMotion.short()) },
    ) { tab ->
        content(tab, Modifier.fillMaxSize())
    }
}

@Composable
private fun RowScope.EditorTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) = Box(
    modifier = Modifier
        .weight(1f)
        .height(36.dp)
        .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = CreatorMotion.short(),
    )
    val indicator by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = CreatorMotion.short(),
    )
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Box(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp).height(2.dp).background(indicator, MaterialTheme.shapes.extraSmall),
    )
}
