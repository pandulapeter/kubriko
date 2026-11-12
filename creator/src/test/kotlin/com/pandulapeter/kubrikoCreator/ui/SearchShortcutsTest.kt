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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchShortcutsTest {

    @Test
    fun cmdOrCtrlFFocusesTheSidebarsFilter() = assertEquals(
        SearchShortcut.FILTER_SIDEBAR,
        searchShortcutOf(Key.F, isShortcutPressed = true, isShiftPressed = false, isAltPressed = false),
    )

    @Test
    fun cmdOrCtrlPOpensQuickOpen() = assertEquals(
        SearchShortcut.QUICK_OPEN,
        searchShortcutOf(Key.P, isShortcutPressed = true, isShiftPressed = false, isAltPressed = false),
    )

    @Test
    fun cmdOrCtrlShiftFOpensTheSearchPanelOnItsFieldLeavingTheUsagesItShowed() {
        assertEquals(SearchShortcut.FIND_IN_PROJECT, searchShortcutOf(Key.F, isShortcutPressed = true, isShiftPressed = true, isAltPressed = false))
        val uiState = WorkspaceUiState()
        uiState.findUsages(com.pandulapeter.kubrikoCreator.context.RelationSubject.Type("Ball"))
        uiState.findInProject()
        assertEquals(SidePanel.SEARCH, uiState.openPanel)
        assertEquals(null, uiState.usagesSubject)
        assertTrue(uiState.searchFieldFocus.isPending)
    }

    @Test
    fun aKeyWithoutCmdOrCtrlOrWithAltIsNoShortcut() {
        assertNull(searchShortcutOf(Key.F, isShortcutPressed = false, isShiftPressed = false, isAltPressed = false))
        assertNull(searchShortcutOf(Key.F, isShortcutPressed = true, isShiftPressed = false, isAltPressed = true))
        assertNull(searchShortcutOf(Key.Z, isShortcutPressed = true, isShiftPressed = false, isAltPressed = false))
    }

    @Test
    fun aFocusRequestIsPendingUntilItIsTaken() {
        val request = FocusRequest()
        request.request()
        assertTrue(request.isPending)
        request.consume()
        assertFalse(request.isPending)
    }

    @Test
    fun shiftPressedTwiceOpensQuickOpenAndTheFocusedElementStillSeesBothPresses() = uiTest(width = 200, height = 200) {
        val uiState = WorkspaceUiState()
        val seen = mutableListOf<KeyEventType>()
        showing {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Box(Modifier.fillMaxSize().searchShortcuts(uiState)) {
                Box(
                    Modifier
                        .size(50.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .testTag("focused")
                        .onKeyEvent { event -> false.also { if (event.key == Key.ShiftLeft) seen += event.type } },
                )
            }
        }
        onNodeWithTag("focused").performKeyInput {
            pressKey(Key.ShiftLeft)
            pressKey(Key.ShiftLeft)
        }
        assertTrue(uiState.isQuickOpenShown)
        assertEquals(listOf(KeyEventType.KeyDown, KeyEventType.KeyUp, KeyEventType.KeyDown, KeyEventType.KeyUp), seen)
    }

    @Test
    fun aClickBetweenTwoShiftsIsNoDoubleShift() = uiTest(width = 200, height = 200) {
        val uiState = WorkspaceUiState()
        showing {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Box(Modifier.fillMaxSize().searchShortcuts(uiState)) {
                Box(Modifier.size(50.dp).focusRequester(focusRequester).focusable().testTag("focused"))
            }
        }
        onNodeWithTag("focused").performKeyInput { pressKey(Key.ShiftLeft) }
        onNodeWithTag("focused").performMouseInput { click() }
        onNodeWithTag("focused").performKeyInput { pressKey(Key.ShiftLeft) }
        assertFalse(uiState.isQuickOpenShown)
    }
}
