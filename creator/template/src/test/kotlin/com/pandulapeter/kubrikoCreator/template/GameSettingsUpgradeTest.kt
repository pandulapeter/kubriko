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

class GameSettingsUpgradeTest {

    @Test
    fun gameStartsFromGameSettingsAndCreatesItsViewportManagerFromThem() {
        val game = """
            |package com.example.mygame
            |
            |import androidx.compose.runtime.remember
            |import com.pandulapeter.kubriko.Kubriko
            |import com.example.mygame.generated.KubrikoWiring
            |
            |fun game() = Kubriko.newInstance(
            |    GameSerialization.createSerializationManager(),
            |    *KubrikoWiring.createManagers(firstScene = "levels/1-1").toTypedArray(),
            |)
        """.trimMargin()
        assertEquals("levels/1-1", GameSettingsUpgrade.firstSceneOf(game))
        val upgraded = GameSettingsUpgrade.game(game)
        assertEquals(
            """
            |package com.example.mygame
            |
            |import androidx.compose.runtime.remember
            |import com.pandulapeter.kubriko.Kubriko
            |import com.pandulapeter.kubriko.manager.ViewportManager
            |import com.example.mygame.generated.KubrikoWiring
            |
            |fun game() = Kubriko.newInstance(
            |    GameSerialization.createSerializationManager(),
            |    ViewportManager.newInstance(
            |        aspectRatioMode = GameSettings.ASPECT_RATIO_MODE,
            |        initialTargetFrameRate = GameSettings.TARGET_FRAME_RATE,
            |    ),
            |    *KubrikoWiring.createManagers(firstScene = GameSettings.FIRST_SCENE).toTypedArray(),
            |)
            """.trimMargin(),
            upgraded,
        )
        assertTrue(GameSettingsUpgrade.isGameDone(checkNotNull(upgraded)))
        assertNull(GameSettingsUpgrade.firstSceneOf(upgraded))
    }

    @Test
    fun aGameThatDoesntLookLikeTheTemplatesIsLeftToTheUser() {
        val wiring = "    *KubrikoWiring.createManagers(firstScene = \"main\").toTypedArray(),"
        assertNull(GameSettingsUpgrade.game("fun x() = Kubriko.newInstance(\n    *KubrikoWiring.createManagers(firstScene = scene).toTypedArray(),\n)"))
        assertNull(GameSettingsUpgrade.game("fun x() = Kubriko.newInstance(\n$wiring\n$wiring\n)"), "two instances")
        assertNull(GameSettingsUpgrade.game("fun x() = Kubriko.newInstance(\n    ViewportManager.newInstance(),\n$wiring\n)"), "a ViewportManager of its own")
        val unnamable = "fun x() = Kubriko.newInstance(\n${wiring.replace("main", "Level 1")}\n)"
        assertNull(GameSettingsUpgrade.game(unnamable), "a first Scene gradle.properties can't hold")
        assertNull(GameSettingsUpgrade.firstSceneOf(unnamable))
    }

    @Test
    fun theFirstSceneOfAnOlderGameIsFoundInItsSceneLoader() {
        assertEquals("level-2", GameSettingsUpgrade.firstSceneOf("        SceneLoader(scenePath = \"files/scenes/level-2.json\"),"))
        assertNull(GameSettingsUpgrade.firstSceneOf("fun x() = Kubriko.newInstance()"))
    }

    @Test
    fun theBuildFileGainsTheBlockOnlyWhenItHasACommonSourceSet() {
        val build = "kotlin {\n    sourceSets {\n        commonMain.dependencies { }\n    }\n}\n"
        val upgraded = checkNotNull(GameSettingsUpgrade.buildFile(build, "com.example.mygame"))
        assertTrue(upgraded.startsWith(build + "\nval gameFirstScene = providers.gradleProperty(\"game.firstScene\").orElse(\"main\")\n"))
        assertTrue("package com.example.mygame" in upgraded && "{{" !in upgraded)
        assertTrue(GameSettingsUpgrade.isBuildFileDone(upgraded))
        assertEquals(upgraded, GameSettingsUpgrade.buildFile(upgraded, "com.example.mygame"))
        assertNull(GameSettingsUpgrade.buildFile("plugins {\n    kotlin(\"jvm\")\n}\n", "com.example.mygame"))
    }

    @Test
    fun onlyMissingPropertiesAreAddedStartingWithTheGamesFirstScene() {
        val properties = "game.name=My Game\ngame.frameRate=display\n"
        val upgraded = GameSettingsUpgrade.properties(properties, "levels/1-1")
        assertEquals(
            properties + "game.firstScene=levels/1-1\ngame.aspectRatio=dynamic\ngame.viewportWidth=1920\ngame.viewportHeight=1080\n",
            upgraded,
        )
        assertTrue(GameSettingsUpgrade.isPropertiesDone(upgraded))
    }
}
