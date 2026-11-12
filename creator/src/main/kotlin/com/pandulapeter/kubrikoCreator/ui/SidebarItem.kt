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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RowActionsButton
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import com.pandulapeter.kubrikoCreator.ui.components.flashBackground
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import com.pandulapeter.kubrikoCreator.ui.gameMap.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_more_options
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * One row of the sidebar: an icon and a name, indented by its [depth] in the folders it is in, or while [isTopLevel] (a
 * document beside the groups rather than in one), lined up with the group headers. The icon, and the row while it is
 * selected, take the [accent] of what the row is, and its name highlights what the sidebar's filter matches in it
 * ([LocalSidebarFilter]). Its [actions] are offered by a ⋮ button that shows while it is
 * pointed at, and by its right-click menu.
 */
@Composable
internal fun SidebarItem(
    icon: DrawableResource,
    text: String,
    onClick: (() -> Unit)?,
    isSelected: Boolean = false,
    detail: String? = null,
    flashKey: Any? = null,
    depth: Int = 0,
    accent: Accent? = null,
    actions: List<ContextAction> = emptyList(),
    isTopLevel: Boolean = false,
    modifier: Modifier = Modifier,
    hints: List<HealthFinding> = emptyList(),
) {
    val menu = rememberRightClickMenuState()
    val hover = rememberHoverRevealState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .then(if (isSelected) Modifier.background(accent?.container ?: MaterialTheme.colorScheme.secondaryContainer) else Modifier)
            .flashBackground(flashKey)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .rightClickMenu(menu)
            .hoverReveal(hover)
            .semantics { selected = isSelected }
            .padding(start = (if (isTopLevel) SIDEBAR_HEADER_START else SIDEBAR_ROW_START) + SIDEBAR_INDENT * depth, end = if (actions.isEmpty()) 12.dp else SIDEBAR_ROW_END),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (isSelected && accent == null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        Box(
            modifier = if (isTopLevel) Modifier.size(SIDEBAR_HEADER_ICON) else Modifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent?.color ?: color.copy(alpha = 0.7f),
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = rememberFilterHighlighted(text),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (hints.isNotEmpty()) HintMarker(hints)
        if (actions.isNotEmpty()) {
            RowActionsButton(
                modifier = Modifier.revealedWhen(hover.isActive || menu.isOpen),
                menu = menu,
                actions = actions,
                label = Res.string.action_more_options,
            )
        }
    }
}

/**
 * How much further a row is indented for every folder it is in.
 */
internal val SIDEBAR_INDENT = 14.dp

/**
 * Where a group header's icon tile starts, and with it a row at the headers' level.
 */
internal val SIDEBAR_HEADER_START = 12.dp

/**
 * The size of a group header's icon tile, the room a row at the headers' level gives its icon, so its name lines up
 * with theirs.
 */
internal val SIDEBAR_HEADER_ICON = 22.dp

/**
 * Where a row's icon starts outside every folder.
 */
internal val SIDEBAR_ROW_START = 28.dp

/**
 * How far a row's last button (its ⋮) is from the sidebar's edge, the same as a group header's, so it is in the same
 * place on every row.
 */
internal val SIDEBAR_ROW_END = 4.dp

/**
 * A quiet dot saying that something about the row is worth a look ([hints]), which its tooltip says.
 */
@Composable
private fun HintMarker(hints: List<HealthFinding>) = WithTooltip(label = hints.map { it.text() }.joinToString("\n")) {
    Box(Modifier.padding(4.dp).size(6.dp).background(Accent.SCREENPLAY.color, CircleShape))
}
