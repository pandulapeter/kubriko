/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.GeneratedCodeLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManagerComposablesTest {
    private val location = GeneratedCodeLocation("src/commonMain/kotlin", "com.example.game")

    @Test
    fun aComposableIsNamedInFullInAPackage() {
        assertTrue(ManagerComposables.isValid("com.example.game.ui.HudUi"))
        assertTrue(ManagerComposables.isValid("ui.Hud_2"))
        assertFalse(ManagerComposables.isValid("HudUi"))
        assertFalse(ManagerComposables.isValid("com.example..HudUi"))
        assertFalse(ManagerComposables.isValid("com.example.Hud Ui"))
        assertFalse(ManagerComposables.isValid("com.example.2Hud"))
    }

    @Test
    fun aNewComposableGoesInTheUiPackageBesideTheGeneratedOneTakingTheManager() {
        assertEquals("HudUi", ManagerComposables.defaultNameOf("Hud"))
        assertEquals("com.example.game.ui.HudUi", ManagerComposables.qualifiedNameOf(location, "HudUi"))
        assertEquals("src/commonMain/kotlin/com/example/game/ui/HudUi.kt", ManagerComposables.pathOf(location, "HudUi"))
        assertEquals(
            """
            package com.example.game.ui

            import androidx.compose.foundation.layout.WindowInsets
            import androidx.compose.runtime.Composable
            import com.example.game.generated.Hud

            /**
             * What the Manager type Hud shows on top of the game, after its texts. Its Properties are Compose state,
             * so what reads them here follows them.
             */
            @Composable
            fun HudUi(hudManager: Hud, windowInsets: WindowInsets) {
            }

            """.trimIndent(),
            ManagerComposables.textOf(location, "Hud", "HudUi"),
        )
    }
}
