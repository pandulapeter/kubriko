/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import org.jetbrains.compose.resources.painterResource

/**
 * The Actor type being dragged from the sidebar, following the pointer.
 */
@Composable
internal fun ActorTypeDragPreview(state: ActorTypeDragState) {
    val dragged = state.dragged ?: return
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        Row(
            modifier = Modifier
                .offset { (state.pointer - origin + Offset(12f, 12f)).round() }
                .shadow(6.dp, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(Res.drawable.ic_actor_type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = dragged,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
