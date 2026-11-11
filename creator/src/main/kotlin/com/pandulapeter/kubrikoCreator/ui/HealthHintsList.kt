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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.ui.gameMap.text
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.game_map_worth_a_look
import kubriko.creator.generated.resources.health_hints_fewer
import kubriko.creator.generated.resources.health_hints_more
import org.jetbrains.compose.resources.stringResource

/**
 * What about [subject] is worth a look ([LocalHealthHints]), shown quietly in its editor, the first few at once; nothing
 * when there is nothing to say.
 */
@Composable
internal fun HealthHintsList(
    subject: RelationSubject,
    modifier: Modifier = Modifier,
) {
    val findings = LocalHealthHints.current.ofEditor(subject)
    if (findings.isEmpty()) return
    var isExpanded by remember(subject) { mutableStateOf(false) }
    Column(
        modifier = modifier.animateContentSize(CreatorMotion.medium()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(Res.string.game_map_worth_a_look),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        (if (isExpanded) findings else findings.take(MAXIMUM_COLLAPSED_HINTS)).forEach { finding ->
            Text(
                text = finding.text(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (findings.size > MAXIMUM_COLLAPSED_HINTS) {
            TextButton(
                modifier = Modifier.heightIn(max = 28.dp).offset(x = -BUTTON_PADDING),
                contentPadding = PaddingValues(horizontal = BUTTON_PADDING, vertical = 2.dp),
                onClick = { isExpanded = !isExpanded },
            ) {
                Text(
                    text = if (isExpanded) stringResource(Res.string.health_hints_fewer) else stringResource(Res.string.health_hints_more, findings.size - MAXIMUM_COLLAPSED_HINTS),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private const val MAXIMUM_COLLAPSED_HINTS = 3

/**
 * The button's own padding, which it is moved back by, so its label lines up with the hints.
 */
private val BUTTON_PADDING = 8.dp
