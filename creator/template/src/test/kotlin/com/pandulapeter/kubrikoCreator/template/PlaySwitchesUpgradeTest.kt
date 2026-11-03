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
import kotlin.test.assertTrue

class PlaySwitchesUpgradeTest {

    @Test
    fun theWebMainBecomesWhatTheTemplateMakesNow() {
        assertFalse(PlaySwitchesUpgrade.isWebDone(OLD_WEB_MAIN))
        val upgraded = PlaySwitchesUpgrade.web(OLD_WEB_MAIN, WIRING_IMPORT)
        assertEquals(templateOf("targets/web/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), upgraded)
        assertEquals(templateOf("templates/starter/src/wasmJsMain/kotlin/__PACKAGE__/Main.kt"), upgraded)
        assertTrue(PlaySwitchesUpgrade.isWebDone(upgraded))
    }

    @Test
    fun theAndroidActivityBecomesWhatTheTemplateMakesNow() {
        assertFalse(PlaySwitchesUpgrade.isAndroidDone(OLD_ANDROID_ACTIVITY))
        val upgraded = PlaySwitchesUpgrade.android(OLD_ANDROID_ACTIVITY, WIRING_IMPORT)
        assertEquals(templateOf("targets/android/src/androidMain/kotlin/__PACKAGE__/MainActivity.kt"), upgraded)
        assertTrue(PlaySwitchesUpgrade.isAndroidDone(upgraded))
    }

    @Test
    fun theIosViewControllerBecomesWhatTheTemplateMakesNow() {
        assertFalse(PlaySwitchesUpgrade.isIosDone(OLD_IOS_VIEW_CONTROLLER))
        val upgraded = PlaySwitchesUpgrade.ios(OLD_IOS_VIEW_CONTROLLER, WIRING_IMPORT)
        assertEquals(templateOf("targets/ios/src/iosMain/kotlin/__PACKAGE__/MainViewController.kt"), upgraded)
        assertTrue(PlaySwitchesUpgrade.isIosDone(upgraded))
    }

    @Test
    fun filesUnlikeTheTemplatesAreLeftAsTheyAreAndCountAsDone() {
        val web = OLD_WEB_MAIN.replace("fun main() = ComposeViewport {", "fun main() = ComposeViewport(title = \"x\") {")
        assertTrue(PlaySwitchesUpgrade.isWebDone(web))
        assertEquals(web, PlaySwitchesUpgrade.web(web, WIRING_IMPORT))
        val webWithMore = "$OLD_WEB_MAIN\nfun other() = 1\n"
        assertTrue(PlaySwitchesUpgrade.isWebDone(webWithMore))
        val android = OLD_ANDROID_ACTIVITY.replace("        super.onCreate(savedInstanceState)\n", "")
        assertTrue(PlaySwitchesUpgrade.isAndroidDone(android))
        assertEquals(android, PlaySwitchesUpgrade.android(android, WIRING_IMPORT))
        val ios = OLD_IOS_VIEW_CONTROLLER.replace(": UIViewController = ", " = ")
        assertTrue(PlaySwitchesUpgrade.isIosDone(ios))
        assertEquals(ios, PlaySwitchesUpgrade.ios(ios, WIRING_IMPORT))
    }

    @Test
    fun anImportThatIsThereAlreadyIsNotAddedAgain() {
        val withImport = OLD_ANDROID_ACTIVITY.replace("import android.os.Bundle", "import android.content.pm.ApplicationInfo\nimport android.os.Bundle")
        assertEquals(templateOf("targets/android/src/androidMain/kotlin/__PACKAGE__/MainActivity.kt"), PlaySwitchesUpgrade.android(withImport, WIRING_IMPORT))
    }

    private fun templateOf(path: String) = TemplateRenderer.renderContent(checkNotNull(TemplateFiles.readResource("$path${TemplateFiles.TEMPLATE_SUFFIX}")), mapOf("PACKAGE" to PACKAGE))

    companion object {
        private const val PACKAGE = "com.example.mygame"
        private const val WIRING_IMPORT = "import com.example.mygame.generated.KubrikoWiring"
        val OLD_WEB_MAIN = """
            |package com.example.mygame
            |
            |import androidx.compose.foundation.layout.fillMaxSize
            |import androidx.compose.ui.ExperimentalComposeUiApi
            |import androidx.compose.ui.Modifier
            |import androidx.compose.ui.window.ComposeViewport
            |
            |@OptIn(ExperimentalComposeUiApi::class)
            |fun main() = ComposeViewport {
            |    Game(
            |        modifier = Modifier.fillMaxSize(),
            |    )
            |}
            |
        """.trimMargin()
        private val OLD_ANDROID_ACTIVITY = """
            |package com.example.mygame
            |
            |import android.os.Bundle
            |import androidx.activity.ComponentActivity
            |import androidx.activity.compose.setContent
            |import androidx.activity.enableEdgeToEdge
            |import androidx.compose.foundation.layout.fillMaxSize
            |import androidx.compose.ui.Modifier
            |
            |class MainActivity : ComponentActivity() {
            |    override fun onCreate(savedInstanceState: Bundle?) {
            |        enableEdgeToEdge()
            |        super.onCreate(savedInstanceState)
            |        setContent {
            |            Game(
            |                modifier = Modifier.fillMaxSize(),
            |            )
            |        }
            |    }
            |}
            |
        """.trimMargin()
        private val OLD_IOS_VIEW_CONTROLLER = """
            |package com.example.mygame
            |
            |import androidx.compose.ui.window.ComposeUIViewController
            |import platform.UIKit.UIViewController
            |
            |fun MainViewController(): UIViewController = ComposeUIViewController {
            |    Game()
            |}
            |
        """.trimMargin()
    }
}
