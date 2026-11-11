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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.model.PlanEntry
import com.agentclientprotocol.model.PlanEntryStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.SmallerModel
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_plan
import kubriko.creator.generated.resources.assistant_plan_approve
import kubriko.creator.generated.resources.assistant_plan_approve_hint
import kubriko.creator.generated.resources.assistant_plan_approve_with
import kubriko.creator.generated.resources.assistant_plan_approved
import kubriko.creator.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The agent's plan for its turn, as a checklist, with an Approve button while [onApprove] isn't null: the agent laid it
 * out and waits for the user to approve it, or to say what to change, before it starts. [onApprove] takes the message
 * that approves it, and [smallerModel] when the user approved it with that one, which a second button offers.
 */
@Composable
internal fun PlanChecklist(
    entries: List<PlanEntry>,
    smallerModel: SmallerModel?,
    onApprove: ((String, SmallerModel?) -> Unit)?,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        text = stringResource(Res.string.assistant_plan),
        style = MaterialTheme.typography.titleSmall,
    )
    entries.forEach { entry ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (entry.status) {
                PlanEntryStatus.COMPLETED -> Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                PlanEntryStatus.IN_PROGRESS -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                else -> Text(
                    modifier = Modifier.size(14.dp),
                    text = "○",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = if (entry.status == PlanEntryStatus.COMPLETED) TextDecoration.LineThrough else null,
                color = if (entry.status == PlanEntryStatus.COMPLETED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    AnimatedVisibility(
        visible = onApprove != null,
        enter = fadeIn(CreatorMotion.medium()) + expandVertically(CreatorMotion.medium()),
        exit = fadeOut(CreatorMotion.short()) + shrinkVertically(CreatorMotion.short()),
    ) {
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val approval = stringResource(Res.string.assistant_plan_approved)
                FilledTonalButton(onClick = { onApprove?.invoke(approval, null) }) {
                    Text(stringResource(Res.string.assistant_plan_approve))
                }
                smallerModel?.let { model ->
                    TextButton(onClick = { onApprove?.invoke(approval, model) }) {
                        Text(stringResource(Res.string.assistant_plan_approve_with, model.name))
                    }
                }
            }
            Text(
                text = stringResource(Res.string.assistant_plan_approve_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
