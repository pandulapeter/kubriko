/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateRendererTest {

    @Test
    fun placeholdersAreReplacedEverywhereTheyAppear() {
        assertEquals(
            "package com.example.game // com.example.game, My Game",
            TemplateRenderer.renderContent("package {{PACKAGE}} // {{PACKAGE}}, {{PROJECT_NAME}}", mapOf("PACKAGE" to "com.example.game", "PROJECT_NAME" to "My Game")),
        )
    }

    @Test
    fun textThatOnlyLooksLikeAPlaceholderIsLeftAlone() {
        val text = "val map = mapOf<String, Int>() // {{lowercase}} and { {SPACED} }"
        assertEquals(text, TemplateRenderer.renderContent(text, emptyMap()))
    }

    @Test
    fun anUnknownPlaceholderIsAnError() {
        assertFailsWith<IllegalArgumentException> { TemplateRenderer.renderContent("{{MISSING}}", emptyMap()) }
    }

    @Test
    fun thePackageTokenBecomesThePackagesFolders() {
        assertEquals("src/commonMain/kotlin/com/example/game/Game.kt", TemplateRenderer.renderPath("src/commonMain/kotlin/__PACKAGE__/Game.kt", "com.example.game"))
    }

    @Test
    fun dotFilesAreStoredWithoutTheirDot() {
        assertEquals(".gitignore", TemplateRenderer.renderPath("gitignore", "com.example.game"))
    }
}
