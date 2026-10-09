/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.isAvailable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


internal fun LazyListScope.menu(
    allShowcaseEntries: List<ShowcaseEntry>,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = allShowcaseEntries
    .groupedForMenu()
    .let { groups ->
        groups.forEach { (type, entries) ->
            item(type.name) {
                MenuCategoryLabel(
                    title = type.titleStringResource,
                    icon = type.iconDrawableResource,
                )
            }
            items(
                items = entries,
                key = { it.name }
            ) { showcaseEntry ->
                MenuItem(
                    isSelected = selectedShowcaseEntry == showcaseEntry,
                    title = showcaseEntry.titleStringResource,
                    subtitle = showcaseEntry.subtitleStringResource,
                    onSelected = { onShowcaseEntrySelected(showcaseEntry) },
                )
            }
        }
    }

/**
 * The index of [showcaseEntry]'s row in a list that has exactly one item before [menu] (the Welcome row), or 0 for
 * the Welcome row itself and for an entry the menu does not list.
 */
internal fun List<ShowcaseEntry>.menuItemIndex(showcaseEntry: ShowcaseEntry?): Int {
    if (showcaseEntry == null) return 0
    var index = 1
    groupedForMenu().forEach { (_, entries) ->
        index++ // The category label
        val position = entries.indexOf(showcaseEntry)
        if (position >= 0) return index + position
        index += entries.size
    }
    return 0
}

private fun List<ShowcaseEntry>.groupedForMenu() = filter { it.isAvailable }.groupBy { it.type }

@Composable
private fun MenuCategoryLabel(
    modifier: Modifier = Modifier,
    title: StringResource,
    icon: DrawableResource,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        )
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues()),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(icon),
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = stringResource(title),
    )
    Text(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        text = stringResource(title),
    )
}