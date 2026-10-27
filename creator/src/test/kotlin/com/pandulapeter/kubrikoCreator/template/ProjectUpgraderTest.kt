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

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProjectUpgraderTest {
    private val parentDirectory = createTempDirectory().toFile()
    private val configuration = CreatorConfiguration(kubrikoVersion = "1.0.0", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = false)
    private val directory = ProjectGenerator(configuration, wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
        .create(NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = parentDirectory))
    private val packageDirectory = File(directory, "src/commonMain/kotlin/com/example/mygame")
    private val settingsFile = File(directory, ".kubriko/project.json")
    private val newGame = File(packageDirectory, "Game.kt").readText()
    private val newEntryPoint = File(packageDirectory, "GameEntryPoint.kt").readText()

    @AfterTest
    fun tearDown() {
        parentDirectory.deleteRecursively()
    }

    /**
     * The project as the template made it before Manager types: `Game.kt` and `GameEntryPoint.kt` without
     * `KubrikoWiring`, and no template version.
     */
    private fun makeItOld() {
        File(packageDirectory, "Game.kt").writeText(OLD_GAME)
        File(packageDirectory, "GameEntryPoint.kt").writeText(OLD_ENTRY_POINT)
        settingsFile.writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "extra": true }""")
    }

    @Test
    fun aNewProjectIsUpToDate() {
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).check())
        assertContains(settingsFile.readText(), "\"templateVersion\": 2")
    }

    @Test
    fun aProjectThatHasNotOptedInHasNothingToUpgrade() {
        settingsFile.delete()
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).check())
    }

    @Test
    fun upgradingAnOldProjectMakesItsFilesWhatTheTemplateMakesNow() {
        makeItOld()
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check())
        assertEquals(listOf("src/commonMain/kotlin/com/example/mygame/Game.kt", "src/commonMain/kotlin/com/example/mygame/GameEntryPoint.kt"), upgrade.changes.map { it.path })
        assertEquals(emptyList(), upgrade.byHand)
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertEquals(newGame, File(packageDirectory, "Game.kt").readText())
        assertEquals(newEntryPoint, File(packageDirectory, "GameEntryPoint.kt").readText())
        val settings = settingsFile.readText()
        assertContains(settings, "\"templateVersion\": 2")
        assertContains(settings, "\"extra\": true")
    }

    @Test
    fun filesThatChangedTooMuchAreLeftToTheUser() {
        makeItOld()
        File(packageDirectory, "Game.kt").writeText(OLD_GAME.replace("            SceneLoader(scenePath = \"files/scenes/main.json\"),\n", ""))
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).apply())
        assertEquals(listOf("src/commonMain/kotlin/com/example/mygame/Game.kt"), upgrade.byHand)
        assertEquals(newEntryPoint, File(packageDirectory, "GameEntryPoint.kt").readText())
        assertFalse("templateVersion" in settingsFile.readText())
    }

    @Test
    fun filesUpgradedByHandCountAsDone() {
        makeItOld()
        File(packageDirectory, "Game.kt").writeText(newGame.replace("SceneLoader(", "MyOwnLoader("))
        File(packageDirectory, "GameEntryPoint.kt").delete()
        assertEquals(listOf("src/commonMain/kotlin/com/example/mygame/GameEntryPoint.kt"), assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check()).byHand)
        File(packageDirectory, "GameEntryPoint.kt").writeText(newEntryPoint)
        ProjectUpgrader(directory).recordVersionIfUpToDate()
        assertContains(settingsFile.readText(), "\"templateVersion\": 2")
    }

    @Test
    fun anEntryPointThatAlreadyHasCustomManagersIsLeftToTheUser() {
        assertNull(ProjectUpgrader.withManagersHandedToTheSceneEditor(OLD_ENTRY_POINT.replace("    override fun createSerializationManager()", "    override fun createCustomManagers() = listOf(Mine())\n\n    override fun createSerializationManager()"), "import x"))
    }

    @Test
    fun theImportGoesAfterTheLastOneOrUnderThePackage() {
        assertEquals(
            "package a\n\nimport b\nimport c\n\nfun x() = Kubriko.newInstance(\n    *KubrikoWiring.createManagers().toTypedArray(),\n    SceneLoader(),\n)",
            ProjectUpgrader.withManagersCreated("package a\n\nimport b\n\nfun x() = Kubriko.newInstance(\n    SceneLoader(),\n)", "import c"),
        )
        assertEquals(
            "package a\n\nimport c\n\nfun x() = Kubriko.newInstance(\n    *KubrikoWiring.createManagers().toTypedArray(),\n    SceneLoader(),\n)",
            ProjectUpgrader.withManagersCreated("package a\n\nfun x() = Kubriko.newInstance(\n    SceneLoader(),\n)", "import c"),
        )
    }

    companion object {
        private val OLD_GAME = """
            |package com.example.mygame
            |
            |import androidx.compose.runtime.Composable
            |import androidx.compose.runtime.DisposableEffect
            |import androidx.compose.runtime.remember
            |import androidx.compose.ui.Modifier
            |import com.pandulapeter.kubriko.Kubriko
            |import com.pandulapeter.kubriko.KubrikoViewport
            |import com.pandulapeter.kubriko.collision.CollisionManager
            |import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
            |
            |/**
            | * The whole game: a viewport showing the first Scene.
            | */
            |@Composable
            |fun Game(
            |    modifier: Modifier = Modifier,
            |) {
            |    val kubriko = remember {
            |        Kubriko.newInstance(
            |            GameSerialization.createSerializationManager(),
            |            CollisionManager.newInstance(),
            |            KeyboardInputManager.newInstance(),
            |            SceneLoader(scenePath = "files/scenes/main.json"),
            |        )
            |    }
            |    DisposableEffect(kubriko) {
            |        onDispose { kubriko.dispose() }
            |    }
            |    KubrikoViewport(
            |        modifier = modifier,
            |        kubriko = kubriko,
            |    )
            |}
            |
        """.trimMargin()

        private val OLD_ENTRY_POINT = """
            |package com.example.mygame
            |
            |import com.pandulapeter.kubriko.creator.CreatorEntryPoint
            |
            |/**
            | * Tells Kubriko Creator which Actor types the Scene editor can place. Registered as a service in
            | * src/desktopMain/resources/META-INF/services.
            | */
            |class GameEntryPoint : CreatorEntryPoint {
            |
            |    override fun createSerializationManager() = GameSerialization.createSerializationManager()
            |}
            |
        """.trimMargin()
    }
}
