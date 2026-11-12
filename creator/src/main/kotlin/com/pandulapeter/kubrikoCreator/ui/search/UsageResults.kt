/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.ui.canBeShown
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.scrollEdgeFades
import com.pandulapeter.kubrikoCreator.ui.placeText
import com.pandulapeter.kubrikoCreator.ui.relationText
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_block
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.ic_scene
import kubriko.creator.generated.resources.ic_settings
import kubriko.creator.generated.resources.ic_sprite
import kubriko.creator.generated.resources.search_usages_failed
import kubriko.creator.generated.resources.search_usages_none
import kubriko.creator.generated.resources.search_usages_reading
import kubriko.creator.generated.resources.search_usages_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What refers to [subject], in the project's order, each row going to its place when clicked ([onUsageSelected]).
 */
@Composable
internal fun UsageResults(
    subject: RelationSubject,
    usages: UsagesState,
    onUsageSelected: (Usage) -> Unit,
) = Column {
    val name = subject.shownName
    Text(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        text = stringResource(Res.string.search_usages_title, name),
        style = MaterialTheme.typography.titleSmall,
    )
    when (usages) {
        is UsagesState.Reading -> SearchMessage(stringResource(Res.string.search_usages_reading, name))
        is UsagesState.Failed -> SearchMessage(stringResource(Res.string.search_usages_failed, usages.message))
        is UsagesState.Read -> if (usages.usages.isEmpty()) {
            SearchMessage(stringResource(Res.string.search_usages_none, name))
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().scrollEdgeFades(listState, MaterialTheme.colorScheme.surfaceContainerLow),
                state = listState,
            ) {
                items(usages.usages) { usage -> UsageResultRow(usage = usage, onClick = if (usage.canBeShown) ({ onUsageSelected(usage) }) else null) }
            }
        }
    }
}

/**
 * One usage: where it is, with the icon of that kind of place, and how it refers to what it is the usage of; clicked,
 * it goes there, unless there is nowhere to go ([onClick] is null).
 */
@Composable
private fun UsageResultRow(
    usage: Usage,
    onClick: (() -> Unit)?,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = onClick != null) { onClick?.invoke() }
        .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val (icon, accent) = usage.place.look
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(icon),
        contentDescription = null,
        tint = accent?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = usage.placeText(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = usage.relationText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The icon and accent of the kind of place [this] is.
 */
private val UsagePlace.look: Pair<DrawableResource, Accent?>
    get() = when (this) {
        is UsagePlace.InModel -> when (place) {
            is RelationPlace.Scene -> Res.drawable.ic_scene to Accent.SCENE
            is RelationPlace.Type, is RelationPlace.Text, is RelationPlace.KotlinUi -> Res.drawable.ic_block to Accent.EVENT
        }
        is UsagePlace.KotlinFile -> Res.drawable.ic_code to Accent.CODE
        is UsagePlace.Markdown -> Res.drawable.ic_page to Accent.SCREENPLAY
        is UsagePlace.ResourceFile -> Res.drawable.ic_sprite to Accent.CODE
        UsagePlace.GameSettings -> Res.drawable.ic_settings to null
    }

/**
 * What [this] is called where people read it: a Property with its type (`Game.score`), a file by its path.
 */
internal val RelationSubject.shownName: String
    get() = when (this) {
        is RelationSubject.Type -> name
        is RelationSubject.Property -> "$owner.$name"
        is RelationSubject.Scene -> name
        is RelationSubject.Page -> name
        is RelationSubject.Timer -> name
        is RelationSubject.Resource -> path
        is RelationSubject.Image -> path
        is RelationSubject.Folder -> path
    }
