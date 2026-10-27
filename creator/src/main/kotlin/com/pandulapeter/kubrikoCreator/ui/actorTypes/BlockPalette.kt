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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_palette
import kubriko.creator.generated.resources.block_palette_hint
import kubriko.creator.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Blocks the selected Event can use; clicking one adds it to the end of the Event.
 */
@Composable
internal fun BlockPalette(
    eventType: EventType?,
    onBlockKindSelected: (BlockKind) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_add,
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
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        BlockKind.entries.filter { it.isAvailableIn(eventType) }.forEach { kind ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clickable { onBlockKindSelected(kind) }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(kind.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(kind.title),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
