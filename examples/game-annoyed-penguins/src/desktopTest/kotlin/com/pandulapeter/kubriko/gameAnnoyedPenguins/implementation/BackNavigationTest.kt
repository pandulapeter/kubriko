/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation

import kotlin.test.Test
import kotlin.test.assertEquals

class BackNavigationTest {

    private val booleans = listOf(false, true)

    @Test
    fun pausesWhileRunningWhateverElseIsTrue() {
        for (isInfoDialogVisible in booleans) for (isLevelLoaded in booleans) for (isCloseConfirmationDialogVisible in booleans) for (isInFullscreenMode in booleans) {
            assertEquals(
                BackNavigationAction.PAUSE,
                backNavigationAction(true, isInfoDialogVisible, isLevelLoaded, isCloseConfirmationDialogVisible, isInFullscreenMode),
            )
        }
    }

    @Test
    fun closesTheInfoDialogWhenPaused() {
        for (isLevelLoaded in booleans) for (isCloseConfirmationDialogVisible in booleans) for (isInFullscreenMode in booleans) {
            assertEquals(
                BackNavigationAction.CLOSE_INFO_DIALOG,
                backNavigationAction(false, true, isLevelLoaded, isCloseConfirmationDialogVisible, isInFullscreenMode),
            )
        }
    }

    @Test
    fun resumesALoadedLevelWhenNoDialogIsOpen() {
        for (isInFullscreenMode in booleans) {
            assertEquals(
                BackNavigationAction.RESUME,
                backNavigationAction(false, false, true, false, isInFullscreenMode),
            )
        }
    }

    @Test
    fun doesNotResumeBehindTheCloseConfirmationDialog() {
        assertEquals(BackNavigationAction.EXIT_FULLSCREEN, backNavigationAction(false, false, true, true, true))
        assertEquals(BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION, backNavigationAction(false, false, true, true, false))
    }

    @Test
    fun exitsFullscreenOrTogglesTheCloseConfirmationWithoutALevel() {
        for (isCloseConfirmationDialogVisible in booleans) {
            assertEquals(BackNavigationAction.EXIT_FULLSCREEN, backNavigationAction(false, false, false, isCloseConfirmationDialogVisible, true))
            assertEquals(BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION, backNavigationAction(false, false, false, isCloseConfirmationDialogVisible, false))
        }
    }
}
