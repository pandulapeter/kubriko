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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * [text], written in Markdown, drawn as the blocks [MarkdownParser] reads from it.
 */
@Composable
internal fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    val blocks = remember(text) { MarkdownParser.parse(text) }
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val linkColor = MaterialTheme.colorScheme.primary
    val body = MaterialTheme.typography.bodyMedium
    blocks.forEach { block ->
        when (block) {
            is MarkdownBlock.Paragraph -> InlineText(block.text, body, codeBackground, linkColor)
            is MarkdownBlock.Heading -> InlineText(
                text = block.text,
                style = if (block.level <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                codeBackground = codeBackground,
                linkColor = linkColor,
            )
            is MarkdownBlock.ListItem -> Row(
                modifier = Modifier.padding(start = (block.depth * 16).dp),
            ) {
                Text(
                    modifier = Modifier.width(20.dp),
                    text = block.number?.let { "$it." } ?: BULLET,
                    style = body,
                )
                InlineText(block.text, body, codeBackground, linkColor)
            }
            is MarkdownBlock.Quote -> Row(
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                InlineText(
                    modifier = Modifier.padding(start = 8.dp),
                    text = block.text,
                    style = body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    codeBackground = codeBackground,
                    linkColor = linkColor,
                )
            }
            is MarkdownBlock.Code -> Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(codeBackground, RoundedCornerShape(6.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                text = block.text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                softWrap = false,
            )
            MarkdownBlock.Rule -> HorizontalDivider()
        }
    }
}

@Composable
private fun InlineText(
    text: String,
    style: TextStyle,
    codeBackground: Color,
    linkColor: Color,
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier,
    text = remember(text, codeBackground, linkColor) { InlineMarkdown.parse(text).toAnnotatedString(codeBackground, linkColor) },
    style = style,
)

private fun InlineMarkdown.toAnnotatedString(codeBackground: Color, linkColor: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        when (span.style) {
            InlineMarkdown.Style.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), span.start, span.end)
            InlineMarkdown.Style.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), span.start, span.end)
            InlineMarkdown.Style.CODE -> addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground), span.start, span.end)
            InlineMarkdown.Style.LINK -> span.url?.let { url ->
                addLink(
                    LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))),
                    span.start,
                    span.end,
                )
            }
        }
    }
}

private const val BULLET = "•"
