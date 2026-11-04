/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.screenplay.PageChunks
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.MarkdownText
import kotlinx.coroutines.delay
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.page_add_paragraph
import kubriko.creator.generated.resources.page_empty
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The page [name] of the screenplay, shown rendered: clicking a chunk of it edits that chunk's Markdown in place
 * ([PageChunkField]), saved through [onTextChange] a moment after typing pauses and when the chunk is left.
 *
 * @param text The page as it is on disk.
 */
@Composable
internal fun PageEditor(
    name: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember(name) { PageEditorState(text, onTextChange) }
    state.onTextChange = onTextChange
    LaunchedEffect(text) { state.onPageChanged(text) }
    // What was typed in the last moment before the editor goes (another page or type is shown) is saved too.
    DisposableEffect(state) { onDispose { state.finish() } }
    val session = state.session
    LaunchedEffect(session?.draft?.text) {
        if (session != null) {
            delay(SAVE_DELAY_IN_MILLISECONDS)
            state.save()
        }
    }
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = PAGE_WIDTH).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PageTitle(name)
            if (session == null) {
                val chunks = remember(state.text) { PageChunks.of(state.text) }
                if (chunks.isEmpty()) {
                    Text(stringResource(Res.string.page_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                chunks.forEachIndexed { index, chunk -> RenderedChunk(chunk.text) { state.edit(index) } }
            } else {
                remember(session.frame) { PageChunks.of(session.frame.before) }.forEachIndexed { index, chunk -> RenderedChunk(chunk.text) { state.edit(state.indexOf(index, isAfter = false)) } }
                PageChunkField(
                    value = session.draft,
                    onValueChange = state::onDraftChange,
                    onDone = state::finish,
                )
                remember(session.frame) { PageChunks.of(session.frame.after) }.forEachIndexed { index, chunk -> RenderedChunk(chunk.text) { state.edit(state.indexOf(index, isAfter = true)) } }
            }
            TextButton(onClick = state::append) { Text(stringResource(Res.string.page_add_paragraph)) }
        }
    }
}

@Composable
private fun PageTitle(name: String) = Row(
    modifier = Modifier.padding(bottom = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(18.dp),
        painter = painterResource(Res.drawable.ic_page),
        contentDescription = null,
        tint = Accent.SCREENPLAY.color,
    )
    Text(text = name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RenderedChunk(
    text: String,
    onClick: () -> Unit,
) = MarkdownText(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    text = text,
)

private const val SAVE_DELAY_IN_MILLISECONDS = 600L
private val PAGE_WIDTH = 760.dp
