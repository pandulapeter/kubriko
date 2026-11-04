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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_folder_open
import org.jetbrains.compose.resources.stringResource

/**
 * The [rows] of the Kotlin files of the group [groupKey] ([kotlinTreeRows]): platform containers and package folders,
 * opened and closed in [tree] by a click, and the files.
 */
internal fun LazyListScope.kotlinTreeItems(
    groupKey: String,
    rows: List<KotlinTreeRow>,
    tree: SidebarTree,
) = items(rows, key = { row -> SidebarKeys.folder(groupKey, row.key) }) { row ->
    val key = SidebarKeys.folder(groupKey, row.key)
    when (row) {
        is KotlinTreeRow.Container -> SidebarFolderRow(
            modifier = Modifier.animateItem(),
            name = row.platform?.let { stringResource(it.title) } ?: row.sourceSet.orEmpty(),
            depth = row.depth,
            isExpanded = row.isExpanded,
            icon = row.platform?.icon ?: Res.drawable.ic_folder_open,
            onToggle = { tree.toggle(key) },
            subfolderActions = tree.subfolderActions(key),
        )
        is KotlinTreeRow.Package -> SidebarFolderRow(
            modifier = Modifier.animateItem(),
            name = row.name,
            depth = row.depth,
            isExpanded = row.isExpanded,
            onToggle = { tree.toggle(key) },
            subfolderActions = tree.subfolderActions(key),
        )
        is KotlinTreeRow.File -> SidebarItem(
            modifier = Modifier.animateItem(),
            icon = Res.drawable.ic_code,
            accent = Accent.CODE,
            text = row.resource.name,
            detail = row.detail,
            depth = row.depth,
            onClick = null,
        )
    }
}
