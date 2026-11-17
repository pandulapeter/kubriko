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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.LocalComposableOpener
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_create_composable
import kubriko.creator.generated.resources.action_open_composable
import kubriko.creator.generated.resources.action_remove_composable
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_open_in_new
import kubriko.creator.generated.resources.manager_type_composable
import kubriko.creator.generated.resources.manager_type_composable_empty
import org.jetbrains.compose.resources.stringResource

/**
 * The hand-written Composable a Manager type shows ([composable], by its full name), made by [onCreate] in a file of the
 * project's own, opened in the system's editor ([open]), and no longer shown after [onRemove], which keeps its file.
 */
@Composable
internal fun ComposableSection(
    composable: String?,
    onCreate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    open: ((String) -> Unit)? = LocalComposableOpener.current,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_code,
        accent = Accent.CODE,
        title = stringResource(Res.string.manager_type_composable),
    ) {
        if (composable == null) {
            IconActionButton(icon = Res.drawable.ic_add, label = Res.string.action_create_composable, onClick = onCreate)
        } else if (open != null) {
            IconActionButton(icon = Res.drawable.ic_open_in_new, label = Res.string.action_open_composable, onClick = { open(composable) })
        }
    }
    if (composable == null) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = stringResource(Res.string.manager_type_composable_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = composable,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_remove_composable, onClick = onRemove)
        }
    }
}
