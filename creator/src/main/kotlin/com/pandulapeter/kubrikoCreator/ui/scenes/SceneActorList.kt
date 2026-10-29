/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.SceneSettingsCodeGenerator
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.scene_actor_count
import kubriko.creator.generated.resources.scene_actor_count_filtered
import kubriko.creator.generated.resources.scene_actor_search
import kubriko.creator.generated.resources.scene_settings_title
import org.jetbrains.compose.resources.stringResource

/**
 * The Actors of the open Scene in the order they are added, by type and ID, filtered by what is typed, for Scenes too
 * dense to click into. The selected one is scrolled to whenever it is selected elsewhere.
 */
@Composable
internal fun SceneActorList(
    entries: List<SceneActorEntry>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(entries, query) { matchingSceneActors(entries, query) }
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, shown) {
        val index = shown.indexOfFirst { it.id == selectedId }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.animateScrollToItem(index)
    }
    CompactTextField(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        value = query,
        onValueChange = { query = it },
        placeholder = stringResource(Res.string.scene_actor_search),
    )
    Text(
        modifier = Modifier.padding(horizontal = 12.dp),
        text = if (shown.size == entries.size) {
            stringResource(Res.string.scene_actor_count, entries.size)
        } else {
            stringResource(Res.string.scene_actor_count_filtered, shown.size, entries.size)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val settingsTitle = stringResource(Res.string.scene_settings_title)
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp),
        state = listState,
    ) {
        items(shown, key = { it.id }) { entry ->
            SceneActorRow(
                title = if (entry.typeId == SceneSettingsCodeGenerator.SETTINGS_NAME) settingsTitle else entry.typeId,
                id = entry.id,
                isSelected = entry.id == selectedId,
                onClick = { onSelect(entry.id) },
            )
        }
    }
}

@Composable
private fun SceneActorRow(
    title: String,
    id: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 4.dp),
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = id,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

/**
 * The Actors among [entries] whose type or ID contains [query], in their order.
 */
internal fun matchingSceneActors(entries: List<SceneActorEntry>, query: String): List<SceneActorEntry> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) entries else entries.filter { it.typeId.contains(trimmed, ignoreCase = true) || it.id.contains(trimmed, ignoreCase = true) }
}
