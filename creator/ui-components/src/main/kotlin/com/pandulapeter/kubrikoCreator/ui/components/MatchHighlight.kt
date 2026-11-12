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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.pandulapeter.kubrikoCreator.foundation.NameMatching

/**
 * [text] with the characters [ranges] name (those a query matched, [NameMatching]) in [style], each range clamped to
 * the text.
 */
public fun highlighted(text: String, ranges: List<IntRange>, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(text)
    ranges.forEach { range ->
        val start = range.first.coerceAtLeast(0)
        val end = (range.last + 1).coerceAtMost(text.length)
        if (start < end) addStyle(style, start, end)
    }
}

/**
 * [text] with what of it matches [query] ([NameMatching]) highlighted, or as it is when it doesn't match.
 */
public fun highlightedMatch(text: String, query: String, style: SpanStyle): AnnotatedString =
    highlighted(text, NameMatching.match(text, query)?.ranges.orEmpty(), style)

/**
 * How what a query matched is highlighted: in the theme's primary color, a little bolder.
 */
@Composable
@ReadOnlyComposable
public fun matchHighlightStyle(): SpanStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
