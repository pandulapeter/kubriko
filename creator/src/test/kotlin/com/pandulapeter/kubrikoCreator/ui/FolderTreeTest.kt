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

import kotlin.test.Test
import kotlin.test.assertEquals

class FolderTreeTest {
    private val items = listOf("Wall" to "", "Bat" to "enemies/flying", "Ball" to "", "Slime" to "enemies")

    @Test
    fun foldersComeFirstAtEveryLevelThenItemsByName() {
        assertEquals(
            listOf(
                FolderTreeRow.Folder("enemies", 0, isExpanded = true, isEmpty = false),
                FolderTreeRow.Folder("enemies/flying", 1, isExpanded = true, isEmpty = false),
                FolderTreeRow.Item("Bat", 2),
                FolderTreeRow.Item("Slime", 1),
                FolderTreeRow.Folder("props", 0, isExpanded = true, isEmpty = true),
                FolderTreeRow.Item("Ball", 0),
                FolderTreeRow.Item("Wall", 0),
            ),
            folderTreeRows(items, folders = setOf("props", "enemies"), collapsed = emptySet()),
        )
    }

    @Test
    fun aCollapsedFolderHidesWhatIsInIt() {
        assertEquals(
            listOf(FolderTreeRow.Folder("enemies", 0, isExpanded = false, isEmpty = false), FolderTreeRow.Item("Ball", 0), FolderTreeRow.Item("Wall", 0)),
            folderTreeRows(items, folders = emptySet(), collapsed = setOf("enemies")),
        )
    }
}
