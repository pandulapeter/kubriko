/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BreakoutProjectTest {
    private val configuration = CreatorConfiguration(kubrikoVersion = "9.9.9-creator", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = true)
    private val directory = BreakoutProject.create(testDirectory(), configuration, wrapperInstaller = { _, _ -> })

    @Test
    fun itIsAtTemplateVersion7WithNoFormatVersion() {
        assertEquals(
            "{\n    \"packageName\": \"com.example.breakout\",\n    \"sourceDirectory\": \"src/commonMain/kotlin\",\n    \"templateVersion\": 7\n}\n",
            File(directory, ".kubriko/project.json").readText(),
        )
        val game = File(directory, "src/commonMain/kotlin/com/example/breakout/Game.kt").readText()
        assertContains(game, "*KubrikoWiring.createManagers(firstScene = \"main\").toTypedArray(),")
        assertFalse("GameSettings" in game)
        assertContains(File(directory, "src/desktopMain/kotlin/com/example/breakout/Main.kt").readText(), "title = \"Breakout\",")
        assertFalse("game.name" in File(directory, "gradle.properties").readText())
    }

    @Test
    fun itHoldsTheBreakoutGameWithItsCodeGenerated() {
        listOf("actors/Ball", "actors/Paddle", "actors/Brick", "actors/GoldBrick", "managers/Game").forEach { definition ->
            assertTrue(File(directory, ".kubriko/$definition.json").isFile, definition)
            assertTrue(File(directory, "src/commonMain/kotlin/com/example/breakout/generated/${definition.substringAfter('/')}.kt").isFile, definition)
        }
        val scenes = File(directory, "src/commonMain/composeResources/files/scenes")
        assertEquals(42, File(scenes, "main.json").readLines().count { "\"typeId\"" in it })
        assertEquals("[]", File(scenes, "game-over.json").readText().trim())
        listOf("wall", "hit").forEach { sound ->
            val file = File(directory, "src/commonMain/composeResources/files/sounds/$sound.wav")
            assertEquals(AudioFileFormat.Type.WAVE, AudioSystem.getAudioFileFormat(file).type)
        }
    }

    @Test
    fun itsBuildUsesCreatorsKubrikoFromMavenLocal() {
        assertContains(File(directory, "gradle/libs.versions.toml").readText(), "kubriko = \"9.9.9-creator\"")
        assertContains(File(directory, "gradle/libs.versions.toml").readText(), "kotlin = \"2.4.21\"")
        assertContains(File(directory, "settings.gradle.kts").readText(), "mavenLocal {")
        assertContains(File(directory, "build.gradle.kts").readText(), "implementation(libs.kubriko.collision)")
    }

    @OptIn(EditorMutation::class)
    @Test
    fun everyTemplateUpgradeAppliesToItInOneGo() {
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check())
        assertEquals(emptyList(), upgrade.byHand)
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertContains(File(directory, ".kubriko/project.json").readText(), "\"templateVersion\": ${ProjectUpgrader.CURRENT_VERSION}")
        val properties = File(directory, "gradle.properties").readText()
        listOf("game.name=Breakout", "game.firstScene=main", "game.frameRate=60").forEach { assertContains(properties, it) }
        val game = File(directory, "src/commonMain/kotlin/com/example/breakout/Game.kt").readText()
        assertContains(game, "GameSettings.FIRST_SCENE")
        assertContains(game, "KubrikoWiring.runningInstances")
        assertContains(File(directory, "src/desktopMain/kotlin/com/example/breakout/Main.kt").readText(), "GameInfo.NAME")
    }

    @Test
    fun everythingIsCommitted() {
        val repository = assertNotNull(JGitRepository.open(directory))
        assertEquals(emptyList(), repository.status().changes)
        assertEquals(listOf("Breakout as Kubriko Creator last committed it."), repository.history(limit = 10).map { it.message.trim() })
    }
}
