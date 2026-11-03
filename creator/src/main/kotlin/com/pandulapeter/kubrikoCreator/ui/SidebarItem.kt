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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.flashBackground
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.ui.components.rightClickMenu
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * One row of the sidebar: an icon and a name, indented by its [depth] in the folders it is in. The icon, and the row
 * while it is selected, take the [accent] of what the row is.
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
    contextActions: List<ContextAction> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val menu = rememberRightClickMenuState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .then(if (isSelected) Modifier.background(accent?.container ?: MaterialTheme.colorScheme.secondaryContainer) else Modifier)
            .flashBackground(flashKey)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .rightClickMenu(menu)
            .semantics { selected = isSelected }
            .padding(start = 28.dp + SIDEBAR_INDENT * depth, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (isSelected && accent == null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = accent?.color ?: color.copy(alpha = 0.7f),
        )
        Text(
            text = text,
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
        RightClickMenu(menu, contextActions)
    }
}

/**
 * How much further a row is indented for every folder it is in.
 */
internal val SIDEBAR_INDENT = 14.dp
