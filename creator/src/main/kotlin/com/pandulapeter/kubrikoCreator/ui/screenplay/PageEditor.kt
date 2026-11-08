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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.screenplay.PageChunks
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.MarkdownText
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
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
 * @param links What its links point at, drawing its mentions as chips.
 * @param sprites The sprites the page can show, by their paths from `composeResources`.
 * @param onAddImage Puts an image on the page, at the end of the section given (or of the page), or null if this page can't
 *   take images. Images dropped onto it, pasted, or chosen in the toolbar go at the end of the section of the chunk being
 *   typed into, which is saved first.
 * @param identity What tells the page apart from the one shown before ([MarkdownIdentity]), so that a page moved meanwhile
 *   keeps what is being typed.
 */
@Composable
internal fun PageEditor(
    name: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    links: PageLinksState? = null,
    identity: Any = name,
    sprites: List<String> = emptyList(),
    onAddImage: ((PageImageSource, section: String?) -> Unit)? = null,
) {
    val state = remember(identity) { PageEditorState(text, onTextChange) }
    val currentOnAddImage by rememberUpdatedState(onAddImage)
    val images = rememberPageImageChoices(state, sprites, isShown = onAddImage != null) { source, section -> currentOnAddImage?.invoke(source, section) }
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
        modifier = modifier
            .fillMaxSize()
            .then(if (images == null) Modifier else Modifier.pageImageDropTarget { files -> images.onStart(); files.forEach { images.onChoose(PageImageSource.ImageFile(it)) } })
            .verticalScroll(rememberScrollState()),
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
                chunks.forEachIndexed { index, chunk -> RenderedChunk(chunk.text, links) { state.edit(index) } }
            } else {
                remember(session.frame) { PageChunks.of(session.frame.before) }.forEachIndexed { index, chunk -> RenderedChunk(chunk.text, links) { state.edit(state.indexOf(index, isAfter = false)) } }
                PageChunkField(
                    value = session.draft,
                    onValueChange = state::onDraftChange,
                    onDone = state::finish,
                    links = links,
                    images = images,
                )
                remember(session.frame) { PageChunks.of(session.frame.after) }.forEachIndexed { index, chunk -> RenderedChunk(chunk.text, links) { state.edit(state.indexOf(index, isAfter = true)) } }
            }
            TextButton(onClick = state::append) { Text(stringResource(Res.string.page_add_paragraph)) }
        }
    }
    links?.pendingAddress?.let { address ->
        AddressConfirmation(
            address = address,
            onOpen = {
                links.pendingAddress = null
                runCatching { openInBrowser(address) }
            },
            onDismiss = { links.pendingAddress = null },
        )
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
    links: PageLinksState?,
    onClick: () -> Unit,
) = MarkdownText(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    text = text,
    links = links,
)

/**
 * What the toolbar's Image menu and pasting offer for the page [state] edits, or null when the page can't take images
 * ([isShown]); an image goes at the end of the section being typed into as it is chosen, after what was typed is saved.
 */
@Composable
private fun rememberPageImageChoices(
    state: PageEditorState,
    sprites: List<String>,
    isShown: Boolean,
    onAddImage: (PageImageSource, section: String?) -> Unit,
): PageImageChoices? {
    val section = remember(state) { arrayOfNulls<String>(1) }
    return if (!isShown) {
        null
    } else {
        remember(state, sprites) {
            PageImageChoices(
                sprites = sprites,
                onStart = {
                    section[0] = state.currentSection()
                    // Saved first, so that the image is added to the page as typed, rather than ending the typing unsaved.
                    state.save()
                },
                onChoose = { source -> onAddImage(source, section[0]) },
            )
        }
    }
}

private const val SAVE_DELAY_IN_MILLISECONDS = 600L
private val PAGE_WIDTH = 760.dp
