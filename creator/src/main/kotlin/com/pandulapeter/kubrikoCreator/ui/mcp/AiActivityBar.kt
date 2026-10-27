/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.mcp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import kotlinx.coroutines.delay
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ai_activity_follow
import kubriko.creator.generated.resources.ai_activity_step
import kubriko.creator.generated.resources.ai_activity_step_with_target
import kubriko.creator.generated.resources.ai_target_block
import kubriko.creator.generated.resources.ai_target_event
import kubriko.creator.generated.resources.ai_target_game
import kubriko.creator.generated.resources.ai_target_scene
import kubriko.creator.generated.resources.ai_target_version_control
import kubriko.creator.generated.resources.ic_ai
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What the AI is doing, step by step: the step it is on (pulsing while it runs) after the one before, numbered, with
 * the switch that makes the editor follow it. It shows up when an AI client starts working and hides a few seconds
 * after it stops.
 */
@Composable
internal fun AiActivityBar(
    steps: List<AiStep>,
    isFollowing: Boolean,
    onFollowingChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val current = steps.lastOrNull()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(current?.id, current?.isRunning) {
        if (current == null) return@LaunchedEffect
        isVisible = true
        if (!current.isRunning) {
            delay(VISIBLE_AFTER_LAST_STEP)
            isVisible = false
        }
    }
    AnimatedVisibility(isVisible && current != null) {
        if (current == null) return@AnimatedVisibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingIcon(isPulsing = current.isRunning)
            Text(
                text = stringResource(Res.string.ai_activity_step, (current.id + 1).toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            steps.getOrNull(steps.lastIndex - 1)?.let { previous ->
                StepText(
                    modifier = Modifier.alpha(0.55f),
                    step = previous,
                    isCurrent = false,
                )
                Text(
                    text = "→",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            StepText(
                modifier = Modifier.weight(1f),
                step = current,
                isCurrent = true,
            )
            FilterChip(
                selected = isFollowing,
                onClick = { onFollowingChange(!isFollowing) },
                label = { Text(stringResource(Res.string.ai_activity_follow)) },
            )
        }
    }
}

@Composable
private fun PulsingIcon(
    isPulsing: Boolean,
) {
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(PULSE_DURATION), RepeatMode.Reverse),
    )
    Icon(
        modifier = Modifier.size(16.dp).alpha(if (isPulsing) pulse else 1f),
        painter = painterResource(Res.drawable.ic_ai),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun StepText(
    step: AiStep,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(step.title)
    val text = step.target?.description()?.let { stringResource(Res.string.ai_activity_step_with_target, title, it) } ?: title
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
        color = if (step.rejection != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * What a step works on, in words: `Paddle, Event 2, Block 1`, counting from one.
 */
@Composable
internal fun CommandTarget.description(): String = when (this) {
    is CommandTarget.BlockType -> when {
        eventIndex == null -> name
        blockIndex == null -> stringResource(Res.string.ai_target_event, name, (eventIndex + 1).toString())
        else -> stringResource(Res.string.ai_target_block, name, (eventIndex + 1).toString(), (blockIndex + 1).toString())
    }
    is CommandTarget.Scene -> stringResource(Res.string.ai_target_scene, name)
    CommandTarget.VersionControl -> stringResource(Res.string.ai_target_version_control)
    CommandTarget.Game -> stringResource(Res.string.ai_target_game)
}

private const val VISIBLE_AFTER_LAST_STEP = 6_000L
private const val PULSE_DURATION = 600
