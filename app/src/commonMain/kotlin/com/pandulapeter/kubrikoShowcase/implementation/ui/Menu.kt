/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntryType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


internal fun LazyListScope.menu(
    allShowcaseEntries: List<ShowcaseEntry>,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = allShowcaseEntries
    .filter { if (BuildConfig.ARE_TEST_EXAMPLES_ENABLED) true else it.type != ShowcaseEntryType.TEST }
    .groupBy { it.type }
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

@Composable
internal fun MenuItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: StringResource,
    subtitle: StringResource,
    onSelected: () -> Unit,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .selectable(
            selected = isSelected,
            onClick = onSelected,
        )
        .background(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        )
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues())
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    val contentColor = if (isSelected) contentColorFor(MaterialTheme.colorScheme.primaryContainer) else LocalContentColor.current
    Text(
        modifier = Modifier.fillMaxWidth(),
        color = contentColor,
        style = MaterialTheme.typography.labelLarge,
        text = stringResource(title),
    )
    Text(
        modifier = Modifier.fillMaxWidth(),
        color = contentColor.copy(alpha = 0.75f),
        style = MaterialTheme.typography.labelSmall,
        text = stringResource(subtitle),
    )
}

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