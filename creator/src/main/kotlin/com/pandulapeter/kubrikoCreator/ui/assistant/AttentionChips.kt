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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_chip_actor_type
import kubriko.creator.generated.resources.assistant_chip_hint
import kubriko.creator.generated.resources.assistant_chip_manager_type
import kubriko.creator.generated.resources.assistant_chip_problem
import kubriko.creator.generated.resources.assistant_chip_scene
import org.jetbrains.compose.resources.stringResource

/**
 * What goes with the next message, filled in from what the user is looking at: each chip on until the user turns it off.
 */
@Composable
internal fun AttentionChips(
    chips: List<AttentionChip>,
    leftOut: Set<String>,
    onToggle: (AttentionChip) -> Unit,
) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { chip ->
            WithTooltip(stringResource(Res.string.assistant_chip_hint)) {
                FilterChip(
                    selected = chip.key !in leftOut,
                    onClick = { onToggle(chip) },
                    label = { Text(text = chip.label(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

@Composable
private fun AttentionChip.label() = when (this) {
    is AttentionChip.Scene -> stringResource(Res.string.assistant_chip_scene, name)
    is AttentionChip.BlockType -> stringResource(if (isManager) Res.string.assistant_chip_manager_type else Res.string.assistant_chip_actor_type, name)
    is AttentionChip.Problem -> stringResource(Res.string.assistant_chip_problem)
}
