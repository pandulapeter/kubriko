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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.AccentIcon
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_manager_type
import kubriko.creator.generated.resources.ic_play
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.workspace_hint_cast
import kubriko.creator.generated.resources.workspace_hint_cast_title
import kubriko.creator.generated.resources.workspace_hint_crew
import kubriko.creator.generated.resources.workspace_hint_crew_title
import kubriko.creator.generated.resources.workspace_hint_play
import kubriko.creator.generated.resources.workspace_hint_scenes
import kubriko.creator.generated.resources.workspace_hint_scenes_title
import kubriko.creator.generated.resources.workspace_hint_subtitle
import kubriko.creator.generated.resources.workspace_hint_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What the center shows while nothing is open: the film the project is, told as its cast, its crew, and its scenes, each
 * in the color it has in the sidebar.
 */
@Composable
internal fun WorkspaceHint(
    modifier: Modifier,
) = Box(
    modifier = modifier.padding(32.dp),
    contentAlignment = Alignment.Center,
) {
    Column(
        modifier = Modifier.widthIn(max = 720.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.workspace_hint_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            modifier = Modifier.widthIn(max = 480.dp),
            text = stringResource(Res.string.workspace_hint_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        HintCards(Modifier.padding(top = 12.dp))
        PlayHint()
    }
}

/**
 * The cast, the crew, and the Scenes, side by side and sharing the width equally while each keeps room for its text,
 * and otherwise stacked as rows with the icon beside the text, so no card is ever squeezed narrower than the others.
 */
@Composable
private fun HintCards(
    modifier: Modifier = Modifier,
) = BoxWithConstraints(modifier) {
    val isSideBySide = maxWidth >= MINIMUM_CARD_WIDTH * HINTS.size + CARD_SPACING * (HINTS.size - 1)
    if (isSideBySide) {
        Row(
            modifier = Modifier.widthIn(max = MAXIMUM_CARD_WIDTH * HINTS.size + CARD_SPACING * (HINTS.size - 1)).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
        ) {
            HINTS.forEach { hint ->
                HintCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    hint = hint,
                    isStacked = false,
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.widthIn(max = STACKED_WIDTH),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HINTS.forEach { hint ->
                HintCard(
                    modifier = Modifier.fillMaxWidth(),
                    hint = hint,
                    isStacked = true,
                )
            }
        }
    }
}

@Composable
private fun HintCard(
    hint: Hint,
    isStacked: Boolean,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Text(
            text = stringResource(hint.title),
            style = MaterialTheme.typography.titleMedium,
            color = hint.accent.color,
        )
        Text(
            text = stringResource(hint.text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val cardModifier = modifier
        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
        .padding(if (isStacked) 12.dp else 16.dp)
    if (isStacked) {
        Row(
            modifier = cardModifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(icon = hint.icon, accent = hint.accent, size = 36.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
        }
    } else {
        Column(
            modifier = cardModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccentIcon(icon = hint.icon, accent = hint.accent, size = 44.dp)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

private class Hint(val icon: DrawableResource, val accent: Accent, val title: StringResource, val text: StringResource)

private val HINTS = listOf(
    Hint(Res.drawable.ic_actor_type, Accent.CAST, Res.string.workspace_hint_cast_title, Res.string.workspace_hint_cast),
    Hint(Res.drawable.ic_manager_type, Accent.CREW, Res.string.workspace_hint_crew_title, Res.string.workspace_hint_crew),
    Hint(Res.drawable.ic_scene, Accent.SCENE, Res.string.workspace_hint_scenes_title, Res.string.workspace_hint_scenes),
)

private val MINIMUM_CARD_WIDTH = 176.dp
private val MAXIMUM_CARD_WIDTH = 216.dp
private val CARD_SPACING = 12.dp
private val STACKED_WIDTH = 480.dp

@Composable
private fun PlayHint() = Row(
    modifier = Modifier.padding(top = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(Res.drawable.ic_play),
        contentDescription = null,
        tint = Accent.ACTION.color,
    )
    Text(
        text = stringResource(Res.string.workspace_hint_play),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
