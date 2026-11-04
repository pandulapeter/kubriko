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

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_write_description_with_ai
import kubriko.creator.generated.resources.ic_ai
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A small AI button, for the end of a description, which has the assistant write the description (or improve the one there
 * is) with [prompt]: shown only while AI features are on and the assistant takes prompts ([LocalDescriptionWriter]), and
 * disabled while it is in the middle of a turn.
 */
@Composable
internal fun WriteDescriptionButton(
    prompt: String,
    modifier: Modifier = Modifier,
) {
    val writer = LocalDescriptionWriter.current
    if (writer == null || !LocalIsAiEnabled.current) return
    val label = stringResource(Res.string.action_write_description_with_ai)
    WithTooltip(
        label = label,
        modifier = modifier,
    ) {
        IconButton(
            modifier = Modifier.size(20.dp),
            enabled = !writer.isBusy,
            onClick = { writer.write(prompt) },
        ) {
            val tint = MaterialTheme.colorScheme.primary
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(Res.drawable.ic_ai),
                contentDescription = label,
                tint = if (writer.isBusy) tint.copy(alpha = 0.38f) else tint,
            )
        }
    }
}
