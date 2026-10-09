/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation

/**
 * What a back press does in Blocky's Journey once loading is done.
 */
internal enum class BackNavigationAction {
    PAUSE,
    CLOSE_INFO_DIALOG,
    EXIT_FULLSCREEN,
    TOGGLE_CLOSE_CONFIRMATION,
}

internal fun backNavigationAction(
    isRunning: Boolean,
    isInfoDialogVisible: Boolean,
    isInFullscreenMode: Boolean,
) = when {
    isRunning -> BackNavigationAction.PAUSE
    isInfoDialogVisible -> BackNavigationAction.CLOSE_INFO_DIALOG
    isInFullscreenMode -> BackNavigationAction.EXIT_FULLSCREEN
    else -> BackNavigationAction.TOGGLE_CLOSE_CONFIRMATION
}
