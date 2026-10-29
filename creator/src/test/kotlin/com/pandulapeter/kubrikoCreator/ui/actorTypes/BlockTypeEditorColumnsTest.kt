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

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockTypeEditorColumnsTest {

    @Test
    fun aRoomyEditorKeepsTheUsualWidths() {
        assertEquals(BlockTypeEditorColumns(250.dp, 280.dp, 220.dp), BlockTypeEditorColumns.of(1_200.dp))
        assertEquals(BlockTypeEditorColumns(250.dp, 280.dp, 220.dp), BlockTypeEditorColumns.of(1_070.dp))
    }

    @Test
    fun aNarrowerEditorNarrowsTheColumnsEvenlyDownToTheirNarrowest() {
        assertEquals(BlockTypeEditorColumns(235.dp, 250.dp, 200.dp), BlockTypeEditorColumns.of(1_005.dp))
        assertEquals(BlockTypeEditorColumns(220.dp, 220.dp, 180.dp), BlockTypeEditorColumns.of(940.dp))
        assertEquals(BlockTypeEditorColumns(220.dp, 220.dp, 180.dp), BlockTypeEditorColumns.of(700.dp))
    }
}
