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
 * The [rows] of the sidebar's Kotlin files ([kotlinTreeRows]): platform containers and package folders, opened and closed
 * by a click ([onToggle] gets the row's key), and the files.
 */
internal fun LazyListScope.kotlinTreeItems(
    rows: List<KotlinTreeRow>,
    onToggle: (key: String) -> Unit,
) = items(rows, key = { row -> "kotlin-${row.key}" }) { row ->
    when (row) {
        is KotlinTreeRow.Container -> SidebarFolderRow(
            modifier = Modifier.animateItem(),
            name = row.platform?.let { stringResource(it.title) } ?: row.sourceSet.orEmpty(),
            depth = row.depth,
            isExpanded = row.isExpanded,
            icon = row.platform?.icon ?: Res.drawable.ic_folder_open,
            onToggle = { onToggle(row.key) },
        )
        is KotlinTreeRow.Package -> SidebarFolderRow(
            modifier = Modifier.animateItem(),
            name = row.name,
            depth = row.depth,
            isExpanded = row.isExpanded,
            onToggle = { onToggle(row.key) },
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
