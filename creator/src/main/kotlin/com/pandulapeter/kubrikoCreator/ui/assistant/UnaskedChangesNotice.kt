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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_unasked_changes
import org.jetbrains.compose.resources.stringResource

/**
 * The note a conversation shows once its agent has been seen changing files without asking: Undo can't take that back, so
 * it suggests version control.
 */
@Composable
internal fun UnaskedChangesNotice(
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    text = stringResource(Res.string.assistant_unasked_changes, LocalAgent.current.name),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
