/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * A line of Markdown drawn with its inline styles, its links that [links] knows as chips, and its other links opening in
 * the browser or followed by [links].
 */
@Composable
internal fun InlineText(
    text: String,
    style: TextStyle,
    codeBackground: Color,
    linkColor: Color,
    links: MarkdownLinks?,
    modifier: Modifier = Modifier,
) {
    val inline = remember(text) { InlineMarkdown.parse(text) }
    val chips = inline.spans.mapNotNull { span ->
        if (span.style != InlineMarkdown.Style.LINK || span.url == null || span.end <= span.start) null else links?.chipOf(span.url)?.let { span to it }
    }
    val base = remember(inline, codeBackground, linkColor, links) { inline.toAnnotatedString(codeBackground, linkColor, links) }
    val chipLooks = chips.map { (span, chip) -> Triple(span, chip.accent, chip.isMissing) }
    val annotated = remember(base, chipLooks) { if (chips.isEmpty()) base else base.withChips(inline.text, chips.map { it.first }) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val inlineContent = chips.mapIndexed { index, (span, chip) ->
        val label = inline.text.substring(span.start, span.end)
        val size = remember(label, style, density) { measurer.measure(label, style.copy(fontWeight = FontWeight.Medium), softWrap = false).size }
        val padding = with(density) { CHIP_PADDING.toPx() }
        chipIdOf(index) to InlineTextContent(
            placeholder = with(density) {
                Placeholder(width = (size.width + padding * 2).toSp(), height = size.height.toSp(), placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
            },
        ) { MentionChip(label, chip, style) }
    }.toMap()
    Text(
        modifier = modifier,
        text = annotated,
        style = style,
        inlineContent = inlineContent,
    )
}

@Composable
private fun MentionChip(
    label: String,
    chip: MarkdownChip,
    style: TextStyle,
) = Box(
    modifier = Modifier
        .fillMaxSize()
        .alpha(if (chip.isMissing) MISSING_ALPHA else 1f)
        .clip(RoundedCornerShape(4.dp))
        .background(chip.accent.container)
        .then(chip.onClick?.let { Modifier.clickable(onClick = it).pointerHoverIcon(PointerIcon.Hand) } ?: Modifier)
        .padding(horizontal = CHIP_PADDING),
    contentAlignment = Alignment.Center,
) {
    Text(
        text = label,
        style = style.copy(
            color = chip.accent.color,
            fontWeight = FontWeight.Medium,
            textDecoration = if (chip.isMissing) TextDecoration.LineThrough else null,
        ),
        maxLines = 1,
        softWrap = false,
    )
}

/**
 * This text with each of [chipSpans], links of [plainText], replaced by a chip's placeholder holding its label.
 */
private fun AnnotatedString.withChips(plainText: String, chipSpans: List<InlineMarkdown.Span>): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    chipSpans.forEachIndexed { index, span ->
        if (span.start < cursor) return@forEachIndexed
        append(this@withChips.subSequence(cursor, span.start))
        appendInlineContent(chipIdOf(index), plainText.substring(span.start, span.end))
        cursor = span.end
    }
    append(this@withChips.subSequence(cursor, this@withChips.length))
}

private fun InlineMarkdown.toAnnotatedString(codeBackground: Color, linkColor: Color, links: MarkdownLinks?): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        when (span.style) {
            InlineMarkdown.Style.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), span.start, span.end)
            InlineMarkdown.Style.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), span.start, span.end)
            InlineMarkdown.Style.CODE -> addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground), span.start, span.end)
            InlineMarkdown.Style.LINK -> span.url?.let { url ->
                val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                if (links == null) {
                    addLink(LinkAnnotation.Url(url, styles), span.start, span.end)
                } else {
                    addLink(LinkAnnotation.Clickable(url, styles) { links.onLinkClick(url) }, span.start, span.end)
                }
            }
        }
    }
}

private fun chipIdOf(index: Int) = "chip-$index"

private val CHIP_PADDING = 4.dp
private const val MISSING_ALPHA = 0.6f
