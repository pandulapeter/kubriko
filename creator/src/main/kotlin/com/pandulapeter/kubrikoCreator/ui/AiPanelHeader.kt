/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_show_game_map
import kubriko.creator.generated.resources.ai_panel_assistant
import kubriko.creator.generated.resources.ai_panel_own_tool
import kubriko.creator.generated.resources.ic_map
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The AI panel's header: the assistant or the user's own AI tool, and, less prominent, what the AI is told about the game.
 */
@Composable
internal fun AiPanelHeader(
    view: AiPanelView,
    onViewSelected: (AiPanelView) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    ViewChip(AiPanelView.ASSISTANT, Res.string.ai_panel_assistant, view, onViewSelected)
    ViewChip(AiPanelView.OWN_TOOL, Res.string.ai_panel_own_tool, view, onViewSelected)
    Spacer(Modifier.weight(1f))
    IconActionButton(
        icon = Res.drawable.ic_map,
        label = Res.string.action_show_game_map,
        onClick = { onViewSelected(if (view == AiPanelView.GAME_MAP) AiPanelView.ASSISTANT else AiPanelView.GAME_MAP) },
        tint = if (view == AiPanelView.GAME_MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ViewChip(
    view: AiPanelView,
    label: StringResource,
    selected: AiPanelView,
    onViewSelected: (AiPanelView) -> Unit,
) = FilterChip(
    selected = view == selected,
    onClick = { onViewSelected(view) },
    label = { Text(stringResource(label)) },
)
