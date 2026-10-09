/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation

import kotlin.test.Test
import kotlin.test.assertEquals

class BackNavigationTest {

    private val booleans = listOf(false, true)

    private fun action(
        isRunning: Boolean = false,
        isGameOver: Boolean = false,
        isInfoDialogVisible: Boolean = false,
        isGameStarted: Boolean = false,
        isCloseConfirmationDialogVisible: Boolean = false,
        isInFullscreenMode: Boolean = false,
    ) = backNavigationAction(
        isRunning = isRunning,
        isGameOver = isGameOver,
        isInfoDialogVisible = isInfoDialogVisible,
        isGameStarted = isGameStarted,
        isCloseConfirmationDialogVisible = isCloseConfirmationDialogVisible,
        isInFullscreenMode = isInFullscreenMode,
    )

    @Test
    fun pausesWhileRunningWhateverElseIsTrue() {
        for (info in booleans) for (started in booleans) for (confirmation in booleans) for (fullscreen in booleans) {
            assertEquals(
                BackNavigationAction.PAUSE,
                action(
                    isRunning = true,
                    isInfoDialogVisible = info,
                    isGameStarted = started,
                    isCloseConfirmationDialogVisible = confirmation,
                    isInFullscreenMode = fullscreen,
                ),
            )
        }
    }

    @Test
    fun treatsARunningGameOverAsPaused() {
        assertEquals(
            BackNavigationAction.CLOSE_INFO_DIALOG,
            action(isRunning = true, isGameOver = true, isInfoDialogVisible = true, isGameStarted = true),
        )
        assertEquals(BackNavigationAction.RESUME, action(isRunning = true, isGameOver = true, isGameStarted = true))
        assertEquals(BackNavigationAction.EXIT_FULLSCREEN, action(isRunning = true, isGameOver = true, isInFullscreenMode = true))
        assertEquals(BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION, action(isRunning = true, isGameOver = true))
    }

    @Test
    fun closesTheInfoDialogWhenPaused() {
        for (gameOver in booleans) for (started in booleans) for (confirmation in booleans) for (fullscreen in booleans) {
            assertEquals(
                BackNavigationAction.CLOSE_INFO_DIALOG,
                action(
                    isGameOver = gameOver,
                    isInfoDialogVisible = true,
                    isGameStarted = started,
                    isCloseConfirmationDialogVisible = confirmation,
                    isInFullscreenMode = fullscreen,
                ),
            )
        }
    }

    @Test
    fun resumesAStartedGameWhenNoDialogIsOpen() {
        for (gameOver in booleans) for (fullscreen in booleans) {
            assertEquals(
                BackNavigationAction.RESUME,
                action(isGameOver = gameOver, isGameStarted = true, isInFullscreenMode = fullscreen),
            )
        }
    }

    @Test
    fun doesNotResumeBehindTheCloseConfirmationDialog() {
        assertEquals(
            BackNavigationAction.EXIT_FULLSCREEN,
            action(isGameStarted = true, isCloseConfirmationDialogVisible = true, isInFullscreenMode = true),
        )
        assertEquals(
            BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION,
            action(isGameStarted = true, isCloseConfirmationDialogVisible = true),
        )
    }

    @Test
    fun exitsFullscreenOrTogglesTheCloseConfirmationBeforeTheGameStarts() {
        for (gameOver in booleans) for (confirmation in booleans) {
            assertEquals(
                BackNavigationAction.EXIT_FULLSCREEN,
                action(isGameOver = gameOver, isCloseConfirmationDialogVisible = confirmation, isInFullscreenMode = true),
            )
            assertEquals(
                BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION,
                action(isGameOver = gameOver, isCloseConfirmationDialogVisible = confirmation),
            )
        }
    }
}
