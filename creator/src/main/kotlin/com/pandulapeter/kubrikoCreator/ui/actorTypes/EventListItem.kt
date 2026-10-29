/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_remove_event
import kubriko.creator.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One Event of the Event list: selected by a click, and dragged to where it should run among the others.
 *
 * @param isDropAbove Whether a dragged Event would land right before this one, which a line above it shows.
 * @param isDropBelow Whether a dragged Event would land right after this one (the last), which a line below it shows.
 */
@Composable
internal fun EventListItem(
    event: EventDefinition,
    index: Int,
    isSelected: Boolean,
    dragState: ListDragState<Int>,
    isDropAbove: Boolean,
    isDropBelow: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    flashKey: Any?,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 36.dp)
        .alpha(if (dragState.dragged == index) 0.4f else 1f)
        .then(if (isSelected) Modifier.background(Accent.EVENT.container) else Modifier)
        .background(rememberFlashColor(flashKey))
        .dropLines(isDropAbove, isDropBelow)
        .listDragSource(dragState) { index }
        .clickable { if (!dragState.isClickAfterDrag) onClick() }
        .padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(event.type.icon),
        contentDescription = null,
        tint = Accent.EVENT.color,
    )
    Column(
        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(event.type.title),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        event.detail()?.let { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    IconActionButton(
        icon = Res.drawable.ic_close,
        label = Res.string.action_remove_event,
        onClick = onRemove,
    )
}

@Composable
private fun Modifier.dropLines(isAbove: Boolean, isBelow: Boolean): Modifier {
    val color = MaterialTheme.colorScheme.primary
    return drawWithContent {
        drawContent()
        val thickness = DROP_LINE_THICKNESS.toPx()
        if (isAbove) drawRect(color, size = Size(size.width, thickness))
        if (isBelow) drawRect(color, topLeft = Offset(0f, size.height - thickness), size = Size(size.width, thickness))
    }
}

private val DROP_LINE_THICKNESS = 3.dp
