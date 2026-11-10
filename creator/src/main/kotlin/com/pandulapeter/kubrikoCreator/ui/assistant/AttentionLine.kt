/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.ui.actorTypes.title
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_chip_actor_type
import kubriko.creator.generated.resources.assistant_chip_event
import kubriko.creator.generated.resources.assistant_chip_game_screen
import kubriko.creator.generated.resources.assistant_chip_hint
import kubriko.creator.generated.resources.assistant_chip_manager_type
import kubriko.creator.generated.resources.assistant_chip_problem
import kubriko.creator.generated.resources.assistant_chip_scene
import kubriko.creator.generated.resources.assistant_chip_scene_screen
import kubriko.creator.generated.resources.assistant_context_none
import kubriko.creator.generated.resources.assistant_context_with
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_open_with
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What goes with the next message, filled in from what the user is looking at, as one quiet line above the composer:
 * each [chips] entry is on or off as it is by default ([AttentionChip.isOnByDefault]) until the user turns it in the
 * line's menu ([toggled]).
 */
@Composable
internal fun AttentionLine(
    chips: List<AttentionChip>,
    toggled: Set<String>,
    onToggle: (AttentionChip) -> Unit,
) {
    if (chips.isEmpty()) return
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    val included = chips.filter { it.isSentWith(toggled) }.map { it.label() }
    Box {
        WithTooltip(stringResource(Res.string.assistant_chip_hint)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(anchor.modifier)
                    .clickable(onClick = anchor::open)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(Res.drawable.ic_open_with),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (included.isEmpty()) stringResource(Res.string.assistant_context_none) else stringResource(Res.string.assistant_context_with, included.joinToString(SEPARATOR)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(Res.drawable.ic_chevron),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RightClickMenu(
            state = menu,
            actions = chips.map { chip -> ContextAction(label = chip.label(), isChecked = chip.isSentWith(toggled)) { onToggle(chip) } },
        )
    }
}

@Composable
private fun AttentionChip.label() = when (this) {
    is AttentionChip.Scene -> stringResource(Res.string.assistant_chip_scene, name)
    is AttentionChip.BlockType -> stringResource(if (isManager) Res.string.assistant_chip_manager_type else Res.string.assistant_chip_actor_type, name)
    is AttentionChip.Problem -> stringResource(Res.string.assistant_chip_problem)
    is AttentionChip.Event -> stringResource(Res.string.assistant_chip_event, stringResource(type.title))
    AttentionChip.GameScreen -> stringResource(Res.string.assistant_chip_game_screen)
    is AttentionChip.SceneScreen -> stringResource(Res.string.assistant_chip_scene_screen)
}

private const val SEPARATOR = " · "
