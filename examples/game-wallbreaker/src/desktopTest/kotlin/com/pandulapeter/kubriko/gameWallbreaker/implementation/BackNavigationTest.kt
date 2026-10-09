/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation

import kotlin.test.Test
import kotlin.test.assertEquals

class BackNavigationTest {

    private val booleans = listOf(false, true)

    @Test
    fun pausesWhileRunningWhateverElseIsTrue() {
        for (isInfoDialogVisible in booleans) for (isGameStarted in booleans) for (isInFullscreenMode in booleans) {
            assertEquals(BackNavigationAction.PAUSE, backNavigationAction(true, isInfoDialogVisible, isGameStarted, isInFullscreenMode))
        }
    }

    @Test
    fun closesTheInfoDialogWhenPaused() {
        for (isGameStarted in booleans) for (isInFullscreenMode in booleans) {
            assertEquals(BackNavigationAction.CLOSE_INFO_DIALOG, backNavigationAction(false, true, isGameStarted, isInFullscreenMode))
        }
    }

    @Test
    fun resumesAStartedGameWhenNoDialogIsOpen() {
        for (isInFullscreenMode in booleans) {
            assertEquals(BackNavigationAction.RESUME, backNavigationAction(false, false, true, isInFullscreenMode))
        }
    }

    @Test
    fun exitsFullscreenOrTogglesTheCloseConfirmationBeforeTheGameStarts() {
        assertEquals(BackNavigationAction.EXIT_FULLSCREEN, backNavigationAction(false, false, false, true))
        assertEquals(BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION, backNavigationAction(false, false, false, false))
    }
}
