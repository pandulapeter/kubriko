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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.action_done
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A dialog of several [pages], listed on its left under its [title] with the chosen one shown on its right, as a desktop
 * app's settings are: everything one click away rather than down one long scroll. As large as [maxWidth] and [maxHeight]
 * allow within the window, and smaller with it. Changes made on a page apply at once, so Done only closes it.
 *
 * @param initialPageKey The key of the page it opens on, or null for the first one; until that page is there (its content
 *   still being read), the first one shows.
 */
@Composable
public fun MasterDetailDialog(
    title: String,
    pages: List<DialogPage>,
    onDismiss: () -> Unit,
    maxWidth: Dp = 820.dp,
    maxHeight: Dp = 580.dp,
    initialPageKey: String? = null,
): Unit = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
) {
    var selectedKey by rememberSaveable { mutableStateOf(initialPageKey ?: pages.firstOrNull()?.key) }
    val selected = pages.firstOrNull { it.key == selectedKey } ?: pages.firstOrNull()
    Surface(
        modifier = Modifier
            .padding(32.dp)
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .fillMaxSize()
            .dialogEntrance(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
    ) {
        Row {
            PageList(
                modifier = Modifier.width(PAGE_LIST_WIDTH).fillMaxHeight(),
                title = title,
                pages = pages,
                selected = selected,
                onSelected = { selectedKey = it.key },
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                AnimatedContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    targetState = selected?.key,
                    transitionSpec = {
                        (fadeIn(CreatorMotion.medium()) + slideInVertically(CreatorMotion.medium()) { it / 40 }) togetherWith fadeOut(CreatorMotion.short())
                    },
                ) { key ->
                    pages.firstOrNull { it.key == key }?.let { PageContent(it) }
                }
                HorizontalDivider()
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    FilledTonalButton(onClick = onDismiss) { Text(stringResource(Res.string.action_done)) }
                }
            }
        }
    }
}

@Composable
private fun PageList(
    title: String,
    pages: List<DialogPage>,
    selected: DialogPage?,
    onSelected: (DialogPage) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 12.dp),
) {
    Text(
        modifier = Modifier.padding(start = PAGE_LIST_GUTTER, end = PAGE_LIST_GUTTER, top = 8.dp, bottom = 12.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
    pages.forEach { page ->
        DialogPageListItem(
            page = page,
            isSelected = page.key == selected?.key,
            onClick = { onSelected(page) },
        )
    }
}

@Composable
private fun PageContent(
    page: DialogPage,
) = Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DIALOG_PAGE_GUTTER, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge,
        )
        page.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    page.content()
}

@Composable
private fun DialogPageListItem(
    page: DialogPage,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = CreatorMotion.short(),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = PAGE_LIST_GUTTER, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(page.icon),
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = page.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val PAGE_LIST_WIDTH = 210.dp

private val PAGE_LIST_GUTTER = 20.dp

/**
 * The space between a [MasterDetailDialog] page's edges and its content, which a row reaching to those edges
 * ([fullBleed]) pads its own content by.
 */
public val DIALOG_PAGE_GUTTER: Dp = 28.dp
