/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(ExperimentalComposeUiApi::class)

package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_check
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource

/**
 * The menu of [actions] that opens right where the element with [rightClickMenu] of [state] was right-clicked (or under the
 * button that opened it): a [Popover] holding compact rows, which scrolls when they are too many, its scrolling reaching
 * the popover's edges. An action with children opens their menu beside it when pointed at or clicked, and choosing in it
 * closes every menu. Every menu of Kubriko Creator is one.
 */
@Composable
public fun RightClickMenu(
    state: RightClickMenuState,
    actions: List<ContextAction>,
): Unit = MenuLevel(state = state, actions = actions, onChosen = state::close)

/**
 * One menu of [actions] open while [state] is, with the submenu of the entry that has one open beside it; [onChosen]
 * closes every menu above it too.
 */
@Composable
private fun MenuLevel(
    state: RightClickMenuState,
    actions: List<ContextAction>,
    onChosen: () -> Unit,
) {
    if (actions.isEmpty()) return
    val hasIcons = actions.any { it.icon != null || it.isChecked != null }
    val submenu = rememberRightClickMenuState()
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val entryCoordinates = remember { HashMap<Int, LayoutCoordinates>() }
    LaunchedEffect(state.isOpen) {
        if (!state.isOpen) {
            expandedIndex = null
            submenu.close()
        }
    }
    fun expand(index: Int?) {
        if (index == expandedIndex) return
        expandedIndex = index
        val coordinates = index?.let(entryCoordinates::get)
        if (coordinates == null) submenu.close() else submenu.openBeside(coordinates)
    }
    Popover(
        state = state,
        modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = MENU_MINIMUM_WIDTH, max = MENU_MAXIMUM_WIDTH),
    ) {
        Column(
            modifier = Modifier.heightIn(max = MENU_MAXIMUM_HEIGHT).verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
        ) {
            actions.forEachIndexed { index, action ->
                if (action.isSeparated) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                val hasChildren = action.children.isNotEmpty()
                MenuEntry(
                    modifier = Modifier
                        .onGloballyPositioned { entryCoordinates[index] = it }
                        .onPointerEvent(PointerEventType.Enter) { expand(index.takeIf { hasChildren }) },
                    action = action,
                    hasIcons = hasIcons,
                    isExpanded = expandedIndex == index,
                    onClick = {
                        if (hasChildren) {
                            expand(index)
                        } else {
                            onChosen()
                            action.onClick()
                        }
                    },
                )
            }
        }
    }
    expandedIndex?.let { index ->
        MenuLevel(
            state = submenu,
            actions = actions.getOrNull(index)?.children.orEmpty(),
            onChosen = {
                submenu.close()
                onChosen()
            },
        )
    }
}

@Composable
private fun MenuEntry(
    action: ContextAction,
    hasIcons: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .background(if (isExpanded) MaterialTheme.colorScheme.onSurface.copy(alpha = EXPANDED_ENTRY_ALPHA) else Color.Transparent)
        .heightIn(min = MENU_ENTRY_HEIGHT)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val color = when {
        action.isDestructive -> MaterialTheme.colorScheme.error
        action.isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val icon = when (action.isChecked) {
        true -> Res.drawable.ic_check
        false -> null
        null -> action.icon
    }
    if (hasIcons) {
        Box(Modifier.size(16.dp)) {
            icon?.let { Icon(modifier = Modifier.size(16.dp), painter = painterResource(it), contentDescription = null, tint = color) }
        }
    }
    Column(
        // An entry with a submenu puts its arrow at the menu's edge, where a submenu opens.
        modifier = Modifier.weight(1f, fill = action.children.isNotEmpty()),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        action.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (action.isCurrent) {
        Icon(modifier = Modifier.size(14.dp), painter = painterResource(Res.drawable.ic_check), contentDescription = null, tint = color)
    }
    if (action.children.isNotEmpty()) {
        Icon(modifier = Modifier.size(14.dp), painter = painterResource(Res.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val MENU_MINIMUM_WIDTH = 140.dp
private val MENU_MAXIMUM_WIDTH = 300.dp
private val MENU_MAXIMUM_HEIGHT = 360.dp
private val MENU_ENTRY_HEIGHT = 30.dp
private const val EXPANDED_ENTRY_ALPHA = 0.08f
