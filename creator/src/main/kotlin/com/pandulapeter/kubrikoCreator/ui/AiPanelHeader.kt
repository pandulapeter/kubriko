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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_ai_panel_more
import kubriko.creator.generated.resources.action_back_to_assistant
import kubriko.creator.generated.resources.action_use_own_ai_tool
import kubriko.creator.generated.resources.action_use_own_ai_tool_description
import kubriko.creator.generated.resources.ai_panel_assistant
import kubriko.creator.generated.resources.ai_panel_own_tool
import kubriko.creator.generated.resources.game_map_description
import kubriko.creator.generated.resources.game_map_title
import kubriko.creator.generated.resources.ic_ai
import kubriko.creator.generated.resources.ic_arrow_back
import kubriko.creator.generated.resources.ic_map
import kubriko.creator.generated.resources.ic_more
import kubriko.creator.generated.resources.ic_open_in_new
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The AI panel's header. The Assistant is the panel's own view, so its header names it and keeps the map and the way to
 * use one's own AI tool instead in its menu; those two views lead back to it.
 */
@Composable
internal fun AiPanelHeader(
    view: AiPanelView,
    onViewSelected: (AiPanelView) -> Unit,
) = AnimatedContent(
    modifier = Modifier.fillMaxWidth(),
    targetState = view,
    transitionSpec = { fadeIn(CreatorMotion.medium()) togetherWith fadeOut(CreatorMotion.short()) },
) { shownView ->
    val isAssistant = shownView == AiPanelView.ASSISTANT
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(start = if (isAssistant) 12.dp else 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isAssistant) {
            AssistantHeader(onViewSelected)
        } else {
            IconActionButton(
                icon = Res.drawable.ic_arrow_back,
                label = Res.string.action_back_to_assistant,
                onClick = { onViewSelected(AiPanelView.ASSISTANT) },
            )
            HeaderTitle(stringResource(if (shownView == AiPanelView.GAME_MAP) Res.string.game_map_title else Res.string.ai_panel_own_tool))
        }
    }
}

@Composable
private fun RowScope.AssistantHeader(
    onViewSelected: (AiPanelView) -> Unit,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(Res.drawable.ic_ai),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    HeaderTitle(stringResource(Res.string.ai_panel_assistant))
    MoreAiOptions(onViewSelected)
}

@Composable
private fun RowScope.HeaderTitle(
    text: String,
) = Text(
    modifier = Modifier.weight(1f),
    text = text,
    style = MaterialTheme.typography.titleSmall,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun MoreAiOptions(
    onViewSelected: (AiPanelView) -> Unit,
) = Box {
    var isExpanded by remember { mutableStateOf(false) }
    IconActionButton(
        icon = Res.drawable.ic_more,
        label = Res.string.action_ai_panel_more,
        onClick = { isExpanded = true },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DropdownMenu(
        modifier = Modifier.width(MENU_WIDTH),
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
    ) {
        MoreAiOption(
            icon = Res.drawable.ic_map,
            title = Res.string.game_map_title,
            description = Res.string.game_map_description,
            onClick = {
                isExpanded = false
                onViewSelected(AiPanelView.GAME_MAP)
            },
        )
        MoreAiOption(
            icon = Res.drawable.ic_open_in_new,
            title = Res.string.action_use_own_ai_tool,
            description = Res.string.action_use_own_ai_tool_description,
            onClick = {
                isExpanded = false
                onViewSelected(AiPanelView.OWN_TOOL)
            },
        )
    }
}

@Composable
private fun MoreAiOption(
    icon: DrawableResource,
    title: StringResource,
    description: StringResource,
    onClick: () -> Unit,
) = DropdownMenuItem(
    leadingIcon = { Icon(modifier = Modifier.size(16.dp), painter = painterResource(icon), contentDescription = null) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title))
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    },
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
)

private val MENU_WIDTH = 320.dp
