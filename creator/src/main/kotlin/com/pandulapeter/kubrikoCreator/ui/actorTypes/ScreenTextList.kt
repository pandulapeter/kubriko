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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_text
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_text
import kubriko.creator.generated.resources.manager_type_text
import kubriko.creator.generated.resources.manager_type_text_size
import kubriko.creator.generated.resources.manager_type_texts
import kubriko.creator.generated.resources.manager_type_texts_empty
import org.jetbrains.compose.resources.stringResource

/**
 * The texts a Manager type shows on top of the game, each with its words (which may show Properties), place, size,
 * and color.
 */
@Composable
internal fun ScreenTextList(
    texts: List<ScreenTextDefinition>,
    onAdded: () -> Unit,
    onChanged: (index: Int, ScreenTextDefinition) -> Unit,
    onRemoved: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_text,
        accent = Accent.CREW,
        title = stringResource(Res.string.manager_type_texts),
    ) {
        IconActionButton(
            icon = Res.drawable.ic_add,
            label = Res.string.action_add_text,
            onClick = onAdded,
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (texts.isEmpty()) {
            Text(
                text = stringResource(Res.string.manager_type_texts_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        texts.forEachIndexed { index, text ->
            ScreenTextRow(
                text = text,
                onChange = { onChanged(index, it) },
                onRemove = { onRemoved(index) },
            )
        }
    }
}

@Composable
private fun ScreenTextRow(
    text: ScreenTextDefinition,
    onChange: (ScreenTextDefinition) -> Unit,
    onRemove: () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactTextField(
            modifier = Modifier.weight(1f),
            value = text.text,
            onValueChange = { onChange(text.copy(text = it)) },
            label = stringResource(Res.string.manager_type_text),
        )
        IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_remove, onClick = onRemove)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuButton(
            label = stringResource(text.position.title),
            options = ScreenPosition.entries,
            optionText = { stringResource(it.title) },
            onSelected = { onChange(text.copy(position = it)) },
        )
        NumberField(
            modifier = Modifier.width(72.dp),
            label = Res.string.manager_type_text_size,
            value = text.size,
            onValueChange = { if (it > 0f) onChange(text.copy(size = it)) },
        )
    }
    ColorField(
        modifier = Modifier.fillMaxWidth(),
        color = text.color,
        onColorChange = { onChange(text.copy(color = it)) },
    )
}
