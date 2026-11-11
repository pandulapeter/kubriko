/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.ui.components.NewNameField
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add
import kubriko.creator.generated.resources.action_rename
import kubriko.creator.generated.resources.ic_folder_open
import kubriko.creator.generated.resources.new_folder_hint

/**
 * A name being typed in [group]'s tree, at [depth], looking like the row it names: a folder's when [isFolder], an item's
 * otherwise. [onName] says why a name can't be used, or null once it is used, which ends the naming.
 */
@Composable
internal fun SidebarNameField(
    group: FolderGroup,
    isFolder: Boolean,
    depth: Int,
    onName: (String) -> Rejection?,
    onNamingChange: (SidebarNaming?) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
) = Box(modifier) {
    NewNameField(
        placeholder = if (isFolder) Res.string.new_folder_hint else group.newItemHint,
        initialName = initialName,
        indent = SIDEBAR_ROW_START + SIDEBAR_INDENT * depth,
        confirmLabel = if (initialName.isEmpty()) Res.string.action_add else Res.string.action_rename,
        icon = if (isFolder) Res.drawable.ic_folder_open else group.icon,
        iconTint = if (isFolder) Color.Unspecified else group.accent.color,
        onCreate = { name -> onName(name).also { if (it == null) onNamingChange(null) }?.toFieldError() },
        onCancel = { onNamingChange(null) },
    )
}
