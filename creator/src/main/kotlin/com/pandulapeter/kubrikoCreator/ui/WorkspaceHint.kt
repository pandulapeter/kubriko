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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
        Row(
            modifier = Modifier.padding(top = 12.dp).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HintCard(Res.drawable.ic_actor_type, Accent.CAST, Res.string.workspace_hint_cast_title, Res.string.workspace_hint_cast)
            HintCard(Res.drawable.ic_manager_type, Accent.CREW, Res.string.workspace_hint_crew_title, Res.string.workspace_hint_crew)
            HintCard(Res.drawable.ic_scene, Accent.SCENE, Res.string.workspace_hint_scenes_title, Res.string.workspace_hint_scenes)
        }
        PlayHint()
    }
}

@Composable
private fun HintCard(
    icon: DrawableResource,
    accent: Accent,
    title: StringResource,
    text: StringResource,
) = Column(
    modifier = Modifier
        .width(216.dp)
        .fillMaxHeight()
        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    AccentIcon(
        icon = icon,
        accent = accent,
        size = 44.dp,
    )
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = accent.color,
    )
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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
