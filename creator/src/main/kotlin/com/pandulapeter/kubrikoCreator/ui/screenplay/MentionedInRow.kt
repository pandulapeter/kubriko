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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_page
import kubriko.creator.generated.resources.mentioned_in
import kubriko.creator.generated.resources.mentioned_in_more
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The pages that mention the file at [path] (from the project's root), each opening on a click, so that the design is
 * one click away from what an editor shows; nothing when no page does, or outside a workspace.
 */
@Composable
internal fun MentionedInRow(
    path: String?,
    modifier: Modifier = Modifier,
) {
    val mentionedIn = LocalMentionedIn.current ?: return
    val pages = path?.let(mentionedIn.index::pagesMentioning).orEmpty()
    if (pages.isEmpty()) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(Res.drawable.ic_page),
            contentDescription = null,
            tint = Accent.SCREENPLAY.color,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp, end = 2.dp),
            text = stringResource(Res.string.mentioned_in),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        pages.take(MAXIMUM_SHOWN_PAGES).forEach { page ->
            TextButton(
                modifier = Modifier.heightIn(max = 28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                onClick = { mentionedIn.onPageSelected(page) },
            ) {
                Text(text = page, color = Accent.SCREENPLAY.color, style = MaterialTheme.typography.labelLarge)
            }
        }
        val rest = pages.drop(MAXIMUM_SHOWN_PAGES)
        if (rest.isNotEmpty()) {
            MenuButton(
                label = stringResource(Res.string.mentioned_in_more, rest.size),
                options = rest,
                optionText = { it },
                onSelected = mentionedIn.onPageSelected,
            )
        }
    }
}

private const val MAXIMUM_SHOWN_PAGES = 4
