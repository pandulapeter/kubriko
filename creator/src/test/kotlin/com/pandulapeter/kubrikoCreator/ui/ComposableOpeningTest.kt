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

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComposableOpeningTest {
    private val texts = mapOf(
        "src/commonMain/kotlin/game/Screens.kt" to "package com.example.game.ui\n\n@Composable\nfun HudUi(hudManager: Hud, windowInsets: WindowInsets) {}\n",
        "src/commonMain/kotlin/game/ui/HudUi.kt" to "package com.example.game.ui\n\nfun HudUi(hudManager: Hud, windowInsets: WindowInsets) {}\n",
        "src/commonMain/kotlin/other/HudUi.kt" to "package com.example.other\n\nfun HudUi() {}\n",
        "src/commonMain/kotlin/game/Menu.kt" to "package com.example.game.ui // Screens\n\nfun Menu() {}\n",
    )
    private val files = texts.keys.map { ProjectResource(ResourceGroup.KOTLIN, File(it), it) }

    private fun find(name: String) = composableFileOf(name, files) { texts[it.invariantSeparatorsPath] }?.invariantSeparatorsPath

    @Test
    fun theFileDeclaringTheComposableInItsPackageIsFoundTheOneNamedAfterItFirst() {
        assertEquals("src/commonMain/kotlin/game/ui/HudUi.kt", find("com.example.game.ui.HudUi"))
        assertEquals("src/commonMain/kotlin/game/Menu.kt", find("com.example.game.ui.Menu"))
        assertEquals("src/commonMain/kotlin/other/HudUi.kt", find("com.example.other.HudUi"))
        assertNull(find("com.example.game.ui.Missing"))
        assertNull(find("com.example.game.HudUi"))
    }
}
