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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebLocaleUpgradeTest {

    @Test
    fun theWebMainCallsTheGuardFirstAsTheTemplateDoes() {
        assertFalse(WebLocaleUpgrade.isDone(WEB_MAIN_11))
        val upgraded = checkNotNull(WebLocaleUpgrade.web(WEB_MAIN_11))
        assertTrue(WebLocaleUpgrade.isDone(upgraded))
        assertEquals(templateOf("targets/web/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), GameSizeUpgrade.change(upgraded))
        assertEquals(templateOf("templates/starter/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), GameSizeUpgrade.change(upgraded))
    }

    @Test
    fun aMainUnlikeTheTemplatesIsLeftToTheUser() {
        assertNull(WebLocaleUpgrade.web(WEB_MAIN_11.replace("fun main() {", "fun main() = run {")))
        assertNull(WebLocaleUpgrade.web(WEB_MAIN_11 + "\nfun main() {\n}\n"))
    }

    private fun templateOf(path: String) = TemplateRenderer.renderContent(checkNotNull(TemplateFiles.readResource("$path${TemplateFiles.TEMPLATE_SUFFIX}")), mapOf("PACKAGE" to PACKAGE))

    companion object {
        private const val PACKAGE = "com.example.mygame"

        /**
         * The web `Main.kt` as template version 11 made it.
         */
        val WEB_MAIN_11 = """
            |package com.example.mygame
            |
            |import androidx.compose.foundation.layout.fillMaxSize
            |import androidx.compose.ui.ExperimentalComposeUiApi
            |import androidx.compose.ui.Modifier
            |import androidx.compose.ui.window.ComposeViewport
            |import com.example.mygame.generated.KubrikoWiring
            |
            |@OptIn(ExperimentalComposeUiApi::class)
            |fun main() {
            |    KubrikoWiring.isLoggingEnabled = creatorParameter("kubriko.creator.logging").toBoolean()
            |    KubrikoWiring.firstSceneOverride = creatorParameter("kubriko.creator.scene")
            |    ComposeViewport {
            |        Game(
            |            modifier = Modifier.fillMaxSize(),
            |        )
            |    }
            |}
            |
            |/**
            | * A parameter of the page's address that Kubriko Creator sets when it plays the game, read only on a page served from
            | * this computer.
            | */
            |private fun creatorParameter(name: String): String? = js("['127.0.0.1', 'localhost', '[::1]'].includes(location.hostname) ? new URLSearchParams(location.search).get(name) : null")
            |
        """.trimMargin()
    }
}
