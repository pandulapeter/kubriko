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

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchShortcutsTest {

    @Test
    fun cmdOrCtrlFFocusesTheSidebarsFilter() = assertEquals(
        SearchShortcut.FILTER_SIDEBAR,
        searchShortcutOf(Key.F, isShortcutPressed = true, isShiftPressed = false, isAltPressed = false),
    )

    @Test
    fun aKeyWithoutCmdOrCtrlOrWithAltIsNoShortcut() {
        assertNull(searchShortcutOf(Key.F, isShortcutPressed = false, isShiftPressed = false, isAltPressed = false))
        assertNull(searchShortcutOf(Key.F, isShortcutPressed = true, isShiftPressed = false, isAltPressed = true))
        assertNull(searchShortcutOf(Key.Z, isShortcutPressed = true, isShiftPressed = false, isAltPressed = false))
    }

    @Test
    fun aShortcutFocusesTheSidebarsFilterAgainEveryTime() {
        val uiState = WorkspaceUiState()
        uiState.focusSidebarFilter()
        uiState.focusSidebarFilter()
        assertEquals(2, uiState.sidebarFilterFocusRequests)
    }
}
