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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.hoverReveal
import com.pandulapeter.kubrikoCreator.ui.components.rememberHoverRevealState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_module
import org.jetbrains.compose.resources.painterResource

/**
 * The band above the groups of one Gradle module of a project of several, named by its Gradle path ([title]), opened
 * and closed by a click, with [subfolderActions] before its chevron while it holds anything that opens.
 */
@Composable
internal fun SidebarModuleHeader(
    title: String,
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
            .padding(start = 12.dp, end = SIDEBAR_ROW_END),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(Res.drawable.ic_module),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subfolderActions != null) {
            ExpandCollapseButtons(
                isShown = hover.isActive,
                actions = subfolderActions,
            )
        }
        SidebarChevron(
            modifier = Modifier.padding(end = 8.dp).size(16.dp),
            isExpanded = isExpanded,
        )
    }
}

/**
 * The name a module of the project in [SidebarModule.path] goes by: its folder written as a Gradle path (`:app:desktop`),
 * which it is unless its build sets another (or it is a build of its own, such as `buildSrc`), or for the root project,
 * the project's own name.
 */
internal fun moduleTitle(path: String, projectName: String) = if (path.isEmpty()) projectName else ":" + path.replace('/', ':')
