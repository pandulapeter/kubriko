/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.key
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoSceneEditor
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoSceneEditor
import com.pandulapeter.kubriko.gameAnnoyedPenguins.AnnoyedPenguinsGameSceneEditor
import com.pandulapeter.kubriko.gameBlockysJourney.BlockysJourneyGameSceneEditor
import com.pandulapeter.kubriko.implementation.windowState

fun main() {
    System.setProperty("apple.awt.application.name", "Kubriko Showcase")
    application {
        windowState = rememberWindowState(
            size = DpSize(860.dp, 660.dp),
        )
        val fullscreenState = rememberDesktopFullscreenState()
        AnnoyedPenguinsGameSceneEditor(
            defaultSceneFolderPath = "../../examples/game-annoyed-penguins/src/commonMain/composeResources/files/scenes",
        )
        BlockysJourneyGameSceneEditor(
            defaultSceneFolderPath = "../../examples/game-blockys-journey/src/commonMain/composeResources/files/scenes",
        )
        PerformanceDemoSceneEditor(
            defaultSceneFolderPath = "../../examples/demo-performance/src/commonMain/composeResources/files/scenes",
        )
        PhysicsDemoSceneEditor(
            defaultSceneFolderPath = "../../examples/demo-physics/src/commonMain/composeResources/files/scenes",
        )

        if (isWindows) {
            key(fullscreenState.isInFullscreenMode) {
                KubrikoShowcaseWindow(
                    fullscreenState = fullscreenState,
                    undecorated = fullscreenState.isInFullscreenMode,
                    resizable = !fullscreenState.isInFullscreenMode,
                )
            }
        } else {
            KubrikoShowcaseWindow(
                fullscreenState = fullscreenState,
                undecorated = false,
                resizable = true,
            )
        }
    }
}
