/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The title row of a panel or a section: an icon, a title, and [actions] at the end. With an [accent], the icon sits on
 * a tile of its color, for the headers of what has a color of its own (the cast, the crew, the Scenes).
 */
@Composable
internal fun PanelHeader(
    icon: DrawableResource,
    title: String,
    modifier: Modifier = Modifier,
    accent: Accent? = null,
    actions: @Composable RowScope.() -> Unit = {},
) = Row(
    modifier = modifier.fillMaxWidth().heightIn(min = 36.dp).padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (accent == null) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        AccentIcon(
            icon = icon,
            accent = accent,
        )
    }
    Text(
        modifier = Modifier.weight(1f),
        text = title,
        style = MaterialTheme.typography.titleSmall,
    )
    actions()
}
