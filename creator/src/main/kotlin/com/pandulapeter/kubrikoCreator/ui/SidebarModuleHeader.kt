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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_module
import org.jetbrains.compose.resources.painterResource

/**
 * The band above the groups of the Gradle module in [path] of a project of several, indented by its [depth] among the
 * modules, named by [moduleFolderName], opened and closed by a click, with [subfolderActions] laid over the end of its
 * name while it is pointed at and holds anything that opens.
 */
@Composable
internal fun SidebarModuleHeader(
    path: String,
    depth: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    subfolderActions: SubfolderActions?,
    modifier: Modifier = Modifier,
) {
    val hover = rememberHoverRevealState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onToggle)
            .hoverReveal(hover)
            .padding(start = 12.dp + SIDEBAR_INDENT * depth, end = SIDEBAR_ROW_END),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(Res.drawable.ic_module),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SidebarRowTitle(
            modifier = Modifier.weight(1f),
            text = moduleFolderName(path),
            style = MaterialTheme.typography.titleSmall,
            isPointedAt = hover.isActive,
            subfolderActions = subfolderActions,
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        SidebarChevron(
            modifier = Modifier.padding(end = 8.dp).size(16.dp),
            isExpanded = isExpanded,
        )
    }
}
