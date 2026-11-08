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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.page_format_bold
import kubriko.creator.generated.resources.page_format_heading
import kubriko.creator.generated.resources.page_format_image
import kubriko.creator.generated.resources.page_format_italic
import kubriko.creator.generated.resources.page_format_link
import kubriko.creator.generated.resources.page_format_list
import kubriko.creator.generated.resources.page_format_mention
import kubriko.creator.generated.resources.page_image_choose
import kubriko.creator.generated.resources.page_image_from_file
import kubriko.creator.generated.resources.page_image_sprite
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Markdown of one chunk of a page being typed into, focused as it opens, with a small toolbar and the usual shortcuts
 * (Cmd/Ctrl+B, I, and K) writing plain Markdown ([PageFormatting]). Escape, or leaving it, ends typing ([onDone]).
 *
 * Typing `@` (or the toolbar's Mention) offers what [links] says the page can mention, below the field: the arrow keys
 * move through them, Enter or Tab writes the one lit as a link, and Escape closes them. The toolbar's Image, and pasting
 * an image or image files, put images on the page through [images].
 */
@Composable
internal fun PageChunkField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDone: () -> Unit,
    links: PageLinksState? = null,
    images: PageImageChoices? = null,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(6.dp))
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    val currentValue by rememberUpdatedState(value)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var hasBeenFocused by remember { mutableStateOf(false) }
    var dismissedAt by remember { mutableStateOf<Int?>(null) }
    val suggestions = links?.let { remember(value, it.candidates, dismissedAt) { mentionSuggestionsOf(value, it.candidates, dismissedAt) } }
    var highlighted by remember(suggestions?.query) { mutableStateOf(0) }
    fun choose(candidate: MentionCandidate) {
        if (links != null && suggestions != null) onValueChange(withMention(currentValue, suggestions.query, candidate, links.pagePath))
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    PageFormattingBar(isMentionShown = links != null, images = images) { format -> onValueChange(format(currentValue)) }
    MentionSuggestionsAnchor(
        suggestions = suggestions,
        highlighted = highlighted,
        onChoose = ::choose,
    ) {
        CompactTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focus ->
                    if (focus.isFocused) hasBeenFocused = true else if (hasBeenFocused) onDone()
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (suggestions != null) {
                        when (event.key) {
                            Key.DirectionDown -> highlighted = (highlighted + 1) % suggestions.matches.size
                            Key.DirectionUp -> highlighted = (highlighted - 1 + suggestions.matches.size) % suggestions.matches.size
                            Key.Enter, Key.NumPadEnter, Key.Tab -> choose(suggestions.matches[highlighted.coerceIn(suggestions.matches.indices)])
                            Key.Escape -> dismissedAt = suggestions.query.start
                            else -> return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (event.key == Key.Escape) {
                        onDone()
                        return@onPreviewKeyEvent true
                    }
                    if (images != null && (event.isMetaPressed || event.isCtrlPressed) && event.key == Key.V && images.paste(pastedImagesOf(), scope)) {
                        return@onPreviewKeyEvent true
                    }
                    val format = if (event.isMetaPressed || event.isCtrlPressed) shortcuts[event.key] else null
                    format?.let { onValueChange(it(currentValue)) } != null
                },
            value = value,
            onValueChange = { changed ->
                // Closing the suggestions lasts only as long as the mention they were for.
                if (dismissedAt != null && mentionQueryOf(changed)?.start != dismissedAt) dismissedAt = null
                onValueChange(changed)
            },
            isSingleLine = false,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun PageFormattingBar(
    isMentionShown: Boolean,
    images: PageImageChoices?,
    onFormat: ((TextFieldValue) -> TextFieldValue) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(2.dp),
) {
    (if (isMentionShown) formats + mention else formats).forEach { (label, format) ->
        TextButton(
            modifier = Modifier.focusProperties { canFocus = false },
            onClick = { onFormat(format) },
        ) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
    if (images != null) PageImageButton(images)
}

@Composable
private fun PageImageButton(images: PageImageChoices) = Box {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    val chooseTitle = stringResource(Res.string.page_image_choose)
    TextButton(
        modifier = anchor.modifier.focusProperties { canFocus = false },
        onClick = {
            images.onStart()
            anchor.open()
        },
    ) {
        Text(stringResource(Res.string.page_format_image), style = MaterialTheme.typography.labelMedium)
    }
    RightClickMenu(
        state = menu,
        actions = listOfNotNull(
            ContextAction(label = stringResource(Res.string.page_image_from_file)) { pickImageFile(chooseTitle)?.let { images.onChoose(PageImageSource.ImageFile(it)) } },
            images.sprites.takeIf { it.isNotEmpty() }?.let { sprites ->
                ContextAction(
                    label = stringResource(Res.string.page_image_sprite),
                    children = sprites.map { sprite -> ContextAction(label = sprite.substringAfterLast('/')) { images.onChoose(PageImageSource.Sprite(sprite)) } },
                )
            },
        ),
    )
}

private val formats: List<Pair<StringResource, (TextFieldValue) -> TextFieldValue>> = listOf(
    Res.string.page_format_heading to { value -> PageFormatting.withLinePrefix(value, "## ") },
    Res.string.page_format_bold to { value -> PageFormatting.wrapped(value, "**") },
    Res.string.page_format_italic to { value -> PageFormatting.wrapped(value, "*") },
    Res.string.page_format_list to { value -> PageFormatting.withLinePrefix(value, "- ") },
    Res.string.page_format_link to PageFormatting::linked,
)

private val mention: Pair<StringResource, (TextFieldValue) -> TextFieldValue> = Res.string.page_format_mention to ::withMentionStarted

private val shortcuts: Map<Key, (TextFieldValue) -> TextFieldValue> = mapOf(
    Key.B to { value -> PageFormatting.wrapped(value, "**") },
    Key.I to { value -> PageFormatting.wrapped(value, "*") },
    Key.K to PageFormatting::linked,
)
