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

class WebFontUpgradeTest {

    @Test
    fun theWebMainBecomesWhatTheTemplateMakesNow() {
        assertFalse(WebFontUpgrade.isDone(WEB_MAIN_11))
        val upgraded = WebFontUpgrade.web(WEB_MAIN_11, BUILD_FILE)
        assertEquals(templateOf("targets/web/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), upgraded)
        assertEquals(templateOf("templates/starter/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), upgraded)
        assertTrue(WebFontUpgrade.isDone(checkNotNull(upgraded)))
    }

    @Test
    fun aMainThatLoadsAFontOfItsOwnCountsAsDone() {
        assertTrue(WebFontUpgrade.isDone(WEB_MAIN_11.replace("    ComposeViewport {", "    ComposeViewport {\n        val resolver = LocalFontFamilyResolver.current")))
    }

    @Test
    fun theResourcesPackageIsTheOneTheBuildNames() {
        val upgraded = checkNotNull(WebFontUpgrade.web(WEB_MAIN_11, "compose.resources {\n    packageOfResClass=\"com.example.assets\"\n}\n"))
        assertTrue("import com.example.assets.Res\n" in upgraded && "import com.example.assets.public_sans_regular\n" in upgraded, upgraded)
    }

    @Test
    fun aMainUnlikeTheTemplatesOrABuildNamingNoResourcesPackageIsLeftToTheUser() {
        assertNull(WebFontUpgrade.web(WEB_MAIN_11.replace("    ComposeViewport {", "    ComposeViewport(viewportContainerId = \"game\") {"), BUILD_FILE))
        assertNull(WebFontUpgrade.web(WEB_MAIN_11.replace("        )\n    }\n}", "        )\n    }}"), BUILD_FILE))
        assertNull(WebFontUpgrade.web(WEB_MAIN_11.replace("            modifier = Modifier.fillMaxSize(),", "            modifier = Modifier.fillMaxSize(),\n            title = \"\"\"\n}\n\"\"\","), BUILD_FILE))
        assertNull(WebFontUpgrade.web(WEB_MAIN_11, "compose.resources {\n}\n"))
        assertNull(WebFontUpgrade.web(WEB_MAIN_11, buildFile = null))
    }

    @Test
    fun aMainImportingAnotherFontOrResIsLeftToTheUser() = listOf("import org.jetbrains.compose.resources.Font", "import com.example.other.Res", "import com.example.other.Typeface as Font").forEach { import ->
        assertNull(WebFontUpgrade.web(WEB_MAIN_11.replace("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\n$import\n"), BUILD_FILE), import)
    }

    @Test
    fun anImportThatIsThereAlreadyIsNotAddedAgain() {
        val withImports = WEB_MAIN_11.replace(
            "import androidx.compose.ui.Modifier\n",
            "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.text.font.FontFamily\nimport androidx.compose.ui.text.platform.Font\n",
        )
        assertEquals(templateOf("targets/web/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), WebFontUpgrade.web(withImports, BUILD_FILE))
    }

    @Test
    fun onlyAMainThatDoesntLoadAFontOfItsOwnNeedsTheFont() {
        assertTrue(WebFontUpgrade.needsFont(WEB_MAIN_11))
        assertTrue(WebFontUpgrade.needsFont(templateOf("targets/web/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt")))
        assertFalse(WebFontUpgrade.needsFont(WEB_MAIN_11.replace("    ComposeViewport {", "    ComposeViewport {\n        val resolver = LocalFontFamilyResolver.current")))
    }

    private fun templateOf(path: String) = TemplateRenderer.renderContent(checkNotNull(TemplateFiles.readResource("$path${TemplateFiles.TEMPLATE_SUFFIX}")), mapOf("PACKAGE" to PACKAGE))

    companion object {
        private const val PACKAGE = "com.example.mygame"
        private const val RESOURCES_PACKAGE = "$PACKAGE.resources"
        private const val BUILD_FILE = "compose.resources {\n    packageOfResClass = \"$RESOURCES_PACKAGE\"\n}\n"

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
