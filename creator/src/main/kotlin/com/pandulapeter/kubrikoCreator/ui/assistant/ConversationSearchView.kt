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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationMatch
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.highlighted
import com.pandulapeter.kubrikoCreator.ui.components.matchHighlightStyle
import com.pandulapeter.kubrikoCreator.ui.components.scrollEdgeFades
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_conversation_search
import kubriko.creator.generated.resources.conversation_search_agent
import kubriko.creator.generated.resources.conversation_search_failed
import kubriko.creator.generated.resources.conversation_search_field
import kubriko.creator.generated.resources.conversation_search_from
import kubriko.creator.generated.resources.conversation_search_hint
import kubriko.creator.generated.resources.conversation_search_none
import kubriko.creator.generated.resources.conversation_search_other_agent
import kubriko.creator.generated.resources.conversation_search_you
import kubriko.creator.generated.resources.ic_close
import org.jetbrains.compose.resources.stringResource

/**
 * The search of the project's saved conversations, shown in place of the conversation: a field, and every message
 * holding what is typed ([search]), each under its conversation's title with the words around it, which [onChosen]
 * goes to. Escape, or the close button, goes back to the conversation ([onClose]).
 */
@Composable
internal fun ConversationSearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    search: suspend (String) -> List<ConversationMatch>,
    onChosen: (ConversationMatch) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    ConversationSearchField(query = query, onQueryChange = onQueryChange, onClose = onClose)
    val trimmed = query.trim()
    val found by produceState<ConversationSearchResult?>(null, trimmed) {
        if (trimmed.isEmpty()) {
            value = null
            return@produceState
        }
        delay(TYPING_PAUSE_IN_MILLISECONDS)
        value = try {
            ConversationSearchResult(trimmed, search(trimmed), failure = null)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ConversationSearchResult(trimmed, emptyList(), failure = exception.message ?: exception.toString())
        }
    }
    val result = found?.takeIf { it.query == trimmed }
    when {
        trimmed.isEmpty() -> SearchLine(stringResource(Res.string.conversation_search_hint))
        result == null -> Unit
        result.failure != null -> SearchLine(stringResource(Res.string.conversation_search_failed, result.failure))
        result.matches.isEmpty() -> SearchLine(stringResource(Res.string.conversation_search_none, trimmed))
        else -> {
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().scrollEdgeFades(listState, MaterialTheme.colorScheme.surfaceContainerLow),
                state = listState,
            ) {
                items(result.matches) { match -> ConversationMatchRow(match = match, onClick = { onChosen(match) }) }
            }
        }
    }
}

/**
 * What a search of the conversations found for [query], or why it found nothing.
 */
private class ConversationSearchResult(
    val query: String,
    val matches: List<ConversationMatch>,
    val failure: String?,
)

@Composable
private fun ConversationSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CompactTextField(
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event -> (event.type == KeyEventType.KeyDown && event.key == Key.Escape).also { if (it) onClose() } },
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(Res.string.conversation_search_field),
    )
    IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_close_conversation_search, onClick = onClose)
}

/**
 * One message found: the title of its conversation, who said it (and whether another agent holds the conversation),
 * and the words around what matched, highlighted.
 */
@Composable
private fun ConversationMatchRow(
    match: ConversationMatch,
    onClick: () -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    val speaker = stringResource(if (match.isFromUser) Res.string.conversation_search_you else Res.string.conversation_search_agent)
    val title = conversationTitle(match.conversation.title)
    Text(
        text = stringResource(if (match.isHeldByAnotherAgent) Res.string.conversation_search_other_agent else Res.string.conversation_search_from, title, speaker),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    val style = matchHighlightStyle()
    Text(
        text = remember(match, style) { highlighted(match.snippet.text, match.snippet.ranges, style) },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SearchLine(
    text: String,
) = Text(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

/**
 * How long typing has to pause before the conversations are searched.
 */
private const val TYPING_PAUSE_IN_MILLISECONDS = 200L
