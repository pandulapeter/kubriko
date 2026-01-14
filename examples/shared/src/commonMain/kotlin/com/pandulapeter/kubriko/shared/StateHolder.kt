/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

import androidx.compose.runtime.mutableStateOf
import com.pandulapeter.kubriko.Kubriko
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface StateHolder {

    /**
     * Used for the debug menu
     */
    val kubriko: Flow<Kubriko?>

    /**
     * Used for navigating back
     */
    val backNavigationIntent: Flow<Unit> get() = emptyFlow()

    /**
     * The music would stop after dispose() is called, but, due to the crossfade transition, that might be just a bit too late.
     */
    fun stopMusic() = Unit

    /**
     * Can be used to hook into back navigation. Returns if the event was consumed.
     */
    fun navigateBack(
        isInFullscreenMode: Boolean,
        onFullscreenModeToggled: () -> Unit,
    ): Boolean = false

    /**
     * Can be used to free up all resources related to the current demo.
     */
    fun dispose()

    companion object {
        val isInfoPanelVisible = mutableStateOf(true)
    }
}