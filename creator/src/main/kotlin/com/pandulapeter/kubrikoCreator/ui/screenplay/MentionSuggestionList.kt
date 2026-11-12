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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.highlightedMatch
import com.pandulapeter.kubrikoCreator.ui.components.matchHighlightStyle

/**
 * The things a mention being typed may name, what of them [query] matches highlighted and the [highlighted] one lit,
 * each chosen by a click. Its rows never take the
 * focus, so the text keeps it.
 */
@Composable
internal fun MentionSuggestionList(
    candidates: List<MentionCandidate>,
    query: String,
    highlighted: Int,
    onChoose: (MentionCandidate) -> Unit,
) = Column(
    modifier = Modifier.width(SUGGESTIONS_WIDTH).padding(vertical = 4.dp),
) {
    val highlightStyle = matchHighlightStyle()
    candidates.forEachIndexed { index, candidate ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (index == highlighted) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
                .focusProperties { canFocus = false }
                .clickable { onChoose(candidate) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background((accentOf(candidate.mention) ?: Accent.CODE).color, CircleShape))
            Text(text = highlightedMatch(candidate.title, query, highlightStyle), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            candidate.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val SUGGESTIONS_WIDTH = 360.dp
