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

    private fun action(
        isRunning: Boolean = false,
        isInfoDialogVisible: Boolean = false,
        isLevelLoaded: Boolean = false,
        isCloseConfirmationDialogVisible: Boolean = false,
        isInFullscreenMode: Boolean = false,
    ) = backNavigationAction(
        isRunning = isRunning,
        isInfoDialogVisible = isInfoDialogVisible,
        isLevelLoaded = isLevelLoaded,
        isCloseConfirmationDialogVisible = isCloseConfirmationDialogVisible,
        isInFullscreenMode = isInFullscreenMode,
    )

    @Test
    fun pausesWhileRunningWhateverElseIsTrue() {
        for (info in booleans) for (loaded in booleans) for (confirmation in booleans) for (fullscreen in booleans) {
            assertEquals(
                BackNavigationAction.PAUSE,
                action(
                    isRunning = true,
                    isInfoDialogVisible = info,
                    isLevelLoaded = loaded,
                    isCloseConfirmationDialogVisible = confirmation,
                    isInFullscreenMode = fullscreen,
                ),
            )
        }
    }

    @Test
    fun closesTheInfoDialogWhenPaused() {
        for (loaded in booleans) for (confirmation in booleans) for (fullscreen in booleans) {
            assertEquals(
                BackNavigationAction.CLOSE_INFO_DIALOG,
                action(
                    isInfoDialogVisible = true,
                    isLevelLoaded = loaded,
                    isCloseConfirmationDialogVisible = confirmation,
                    isInFullscreenMode = fullscreen,
                ),
            )
        }
    }

    @Test
    fun resumesALoadedLevelWhenNoDialogIsOpen() {
        for (fullscreen in booleans) {
            assertEquals(BackNavigationAction.RESUME, action(isLevelLoaded = true, isInFullscreenMode = fullscreen))
        }
    }

    @Test
    fun doesNotResumeBehindTheCloseConfirmationDialog() {
        assertEquals(
            BackNavigationAction.EXIT_FULLSCREEN,
            action(isLevelLoaded = true, isCloseConfirmationDialogVisible = true, isInFullscreenMode = true),
        )
        assertEquals(
            BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION,
            action(isLevelLoaded = true, isCloseConfirmationDialogVisible = true),
        )
    }

    @Test
    fun exitsFullscreenOrTogglesTheCloseConfirmationWithoutALevel() {
        for (confirmation in booleans) {
            assertEquals(
                BackNavigationAction.EXIT_FULLSCREEN,
                action(isCloseConfirmationDialogVisible = confirmation, isInFullscreenMode = true),
            )
            assertEquals(
                BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION,
                action(isCloseConfirmationDialogVisible = confirmation),
            )
        }
    }
}
