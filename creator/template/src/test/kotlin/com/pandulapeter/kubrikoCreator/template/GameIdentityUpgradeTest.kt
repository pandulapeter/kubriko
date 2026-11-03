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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameIdentityUpgradeTest {

    @Test
    fun theWindowTitleComesFromGameInfoWhenItIsTheTemplatesText() {
        val main = "    Window(\n        title = \"My \\\"Game\\\"\",\n        onCloseRequest = ::exitApplication,\n    )"
        assertEquals("My \"Game\"", GameIdentityUpgrade.windowTitleOf(main))
        assertEquals("    Window(\n        title = GameInfo.NAME,\n        onCloseRequest = ::exitApplication,\n    )", GameIdentityUpgrade.main(main))
        val computed = "Window(\n    title = \"\$name\",\n)"
        assertTrue(GameIdentityUpgrade.isMainDone(computed), "a title that isn't plain text is the user's")
        assertEquals(computed, GameIdentityUpgrade.main(computed))
        assertNull(GameIdentityUpgrade.windowTitleOf(computed))
    }

    @Test
    fun thePageTitleIsFilledInByTheBuild() {
        val page = "<head>\n    <title>Tom &amp; Jerry</title>\n</head>"
        assertEquals("Tom & Jerry", GameIdentityUpgrade.pageTitleOf(page))
        val upgraded = GameIdentityUpgrade.page(page)
        assertEquals("<head>\n    <title>@GAME_NAME@</title>\n</head>", upgraded)
        assertTrue(GameIdentityUpgrade.isPageDone(upgraded))
        assertNull(GameIdentityUpgrade.pageTitleOf(upgraded))
        assertTrue(GameIdentityUpgrade.isPageDone("<head></head>"))
    }

    @Test
    fun theBuildFileGainsTheBlockOnlyWhenItHasTheTemplatesTargets() {
        val build = "kotlin {\n    wasmJs { }\n}\n\ncompose.desktop {\n}\n"
        val upgraded = checkNotNull(GameIdentityUpgrade.buildFile(build, "com.example.mygame"))
        assertTrue(upgraded.startsWith(build + "\nval gameName = providers.gradleProperty(\"game.name\")"))
        assertTrue("orElse(\"com.example.mygame\")" in upgraded && "package com.example.mygame" in upgraded)
        assertTrue(GameIdentityUpgrade.isBuildFileDone(upgraded))
        assertNull(GameIdentityUpgrade.buildFile("kotlin {\n    jvm()\n}\n", "com.example.mygame"))
    }

    @Test
    fun onlyMissingPropertiesAreAdded() {
        val properties = "org.gradle.caching=true\ngame.name=Kept\ngame.vendor=Me\n"
        val upgraded = GameIdentityUpgrade.properties(properties, "Ignored", "com.example.mygame")
        assertEquals(
            properties + "game.versionName=1.0.0\ngame.versionNumber=1\ngame.applicationId=com.example.mygame\ngame.description=\n",
            upgraded,
        )
        assertTrue(GameIdentityUpgrade.isPropertiesDone(upgraded))
    }
}
