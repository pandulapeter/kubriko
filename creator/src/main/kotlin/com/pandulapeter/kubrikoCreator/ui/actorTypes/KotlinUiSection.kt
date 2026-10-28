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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.KotlinUi
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_edit_kotlin_ui
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_open_in_new
import kubriko.creator.generated.resources.manager_type_kotlin_ui
import kubriko.creator.generated.resources.manager_type_kotlin_ui_empty
import org.jetbrains.compose.resources.stringResource

/**
 * A Manager type's Kotlin UI: the start of its code, which opens [KotlinUiDialog] to edit it.
 */
@Composable
internal fun KotlinUiSection(
    managerTypeName: String,
    code: String?,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    var isEditing by remember(managerTypeName) { mutableStateOf(false) }
    PanelHeader(
        icon = Res.drawable.ic_code,
        accent = Accent.CODE,
        title = stringResource(Res.string.manager_type_kotlin_ui),
    ) {
        IconActionButton(
            icon = Res.drawable.ic_open_in_new,
            label = Res.string.action_edit_kotlin_ui,
            onClick = { isEditing = true },
        )
    }
    val preview = remember(code) { kotlinUiPreview(code) }
    if (preview == null) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = stringResource(Res.string.manager_type_kotlin_ui_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                .clickable { isEditing = true }
                .padding(8.dp),
            text = preview,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
    if (isEditing) {
        KotlinUiDialog(
            managerTypeName = managerTypeName,
            code = code.orEmpty(),
            onCodeChange = onCodeChange,
            onDismiss = { isEditing = false },
        )
    }
}

/**
 * The first lines of the Composable's statements (leaving out the imports), or null if it has none.
 */
internal fun kotlinUiPreview(code: String?) = KotlinUi.of(code)?.statements?.let { lines ->
    (lines.take(PREVIEW_LINES) + if (lines.size > PREVIEW_LINES) listOf("…") else emptyList()).joinToString("\n")
}

private const val PREVIEW_LINES = 6
