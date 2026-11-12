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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.context.SearchHit
import com.pandulapeter.kubrikoCreator.ui.canBeShown
import com.pandulapeter.kubrikoCreator.ui.components.highlighted
import com.pandulapeter.kubrikoCreator.ui.components.matchHighlightStyle
import com.pandulapeter.kubrikoCreator.ui.components.scrollEdgeFades
import com.pandulapeter.kubrikoCreator.ui.text
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.search_hint
import kubriko.creator.generated.resources.search_place_line
import kubriko.creator.generated.resources.search_texts_failed
import kubriko.creator.generated.resources.search_texts_more
import kubriko.creator.generated.resources.search_texts_none
import kubriko.creator.generated.resources.search_texts_reading
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What a search of the project's texts found ([TextSearchState]): each line with what matched highlighted, under
 * where it is, going there when clicked ([onHitSelected]); before anything is typed, how to search and find usages.
 */
@Composable
internal fun TextResults(
    state: TextSearchState,
    onHitSelected: (SearchHit) -> Unit,
) = Column {
    when (state) {
        TextSearchState.Idle -> SearchMessage(stringResource(Res.string.search_hint))
        is TextSearchState.Searching -> SearchMessage(stringResource(Res.string.search_texts_reading, state.query))
        is TextSearchState.Failed -> SearchMessage(stringResource(Res.string.search_texts_failed, state.message))
        is TextSearchState.Found -> if (state.results.hits.isEmpty()) {
            SearchMessage(stringResource(Res.string.search_texts_none, state.query))
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().scrollEdgeFades(listState, MaterialTheme.colorScheme.surfaceContainerLow),
                state = listState,
            ) {
                items(state.results.hits) { hit -> TextHitRow(hit = hit, onClick = if (hit.place.canBeShown) ({ onHitSelected(hit) }) else null) }
                val more = state.results.total - state.results.hits.size
                if (more > 0) item { SearchMessage(stringResource(Res.string.search_texts_more, more)) }
            }
        }
    }
}

/**
 * One line found: where it is, with the icon of that kind of place, and the line with what matched highlighted; clicked,
 * it goes there, unless there is nowhere to go ([onClick] is null).
 */
@Composable
private fun TextHitRow(
    hit: SearchHit,
    onClick: (() -> Unit)?,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = onClick != null) { onClick?.invoke() }
        .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val (icon, accent) = hit.place.look
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(icon),
        contentDescription = null,
        tint = accent?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
        modifier = Modifier.weight(1f),
    ) {
        val place = hit.place.text()
        Text(
            text = hit.line?.let { stringResource(Res.string.search_place_line, place, it) } ?: place,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val style = matchHighlightStyle()
        Text(
            text = remember(hit, style) { highlighted(hit.text, hit.ranges, style) },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
