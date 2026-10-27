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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_not_now
import kubriko.creator.generated.resources.action_review_upgrade
import kubriko.creator.generated.resources.ic_warning
import kubriko.creator.generated.resources.project_upgrade_notice
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tells that the project's template files need a change this version of Kubriko Creator relies on.
 */
@Composable
internal fun ProjectUpgradeNotice(
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.tertiaryContainer,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(Res.drawable.ic_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(Res.string.project_upgrade_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        val buttonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
        TextButton(onClick = onDismiss, colors = buttonColors) {
            Text(stringResource(Res.string.action_not_now))
        }
        TextButton(onClick = onReview, colors = buttonColors) {
            Text(stringResource(Res.string.action_review_upgrade))
        }
    }
}
