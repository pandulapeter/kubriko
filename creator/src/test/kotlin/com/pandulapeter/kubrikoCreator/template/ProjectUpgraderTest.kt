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
    private val mainFile = File(directory, "src/desktopMain/kotlin/com/example/mygame/Main.kt")
    private val newMain = mainFile.readText()
    private val sceneLoaderFile = File(packageDirectory, "SceneLoader.kt")
    private val oldSceneLoader = TemplateRenderer.renderContent(checkNotNull(TemplateFiles.readResource("upgrades/SceneLoader-2.kt.template")), mapOf("PACKAGE" to "com.example.mygame"))

    @AfterTest
    fun tearDown() {
        parentDirectory.deleteRecursively()
    }

    /**
     * The project as the template made it before Manager types: `Game.kt` and `GameEntryPoint.kt` without
     * `KubrikoWiring`, the template's `SceneLoader.kt`, and no template version.
     */
    private fun makeItOld(game: String = OLD_GAME) {
        File(packageDirectory, "Game.kt").writeText(game)
        File(packageDirectory, "GameEntryPoint.kt").writeText(OLD_ENTRY_POINT)
        mainFile.writeText(OLD_MAIN)
        sceneLoaderFile.writeText(oldSceneLoader)
        settingsFile.writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "extra": true }""")
    }

    /**
     * The project as version 2 of the template made it: Manager types wired, but Scenes loaded by its own `SceneLoader`.
     */
    private fun makeItVersion2() {
        makeItOld(game = OLD_GAME.replace(SCENE_LOADER_LINE, "            *KubrikoWiring.createManagers().toTypedArray(),\n$SCENE_LOADER_LINE").replace(KUBRIKO_IMPORT, "$KUBRIKO_IMPORT\n$WIRING_IMPORT"))
        File(packageDirectory, "GameEntryPoint.kt").writeText(newEntryPoint)
        settingsFile.writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "templateVersion": 2 }""")
    }

    @Test
    fun aNewProjectIsUpToDate() {
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).check())
        assertContains(settingsFile.readText(), "\"templateVersion\": 5")
        assertFalse(sceneLoaderFile.exists())
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
        assertEquals(listOf("$PACKAGE_PATH/Game.kt", "$PACKAGE_PATH/GameEntryPoint.kt", MAIN_PATH, "$PACKAGE_PATH/SceneLoader.kt"), upgrade.changes.map { it.path })
        assertNull(upgrade.changes.last().after)
        assertEquals(emptyList(), upgrade.byHand)
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertEquals(newGame, File(packageDirectory, "Game.kt").readText())
        assertEquals(newEntryPoint, File(packageDirectory, "GameEntryPoint.kt").readText())
        assertEquals(newMain, mainFile.readText())
        assertFalse(sceneLoaderFile.exists())
        val settings = settingsFile.readText()
        assertContains(settings, "\"templateVersion\": 5")
        assertContains(settings, "\"extra\": true")
    }

    @Test
    fun upgradingAVersion2ProjectHandsTheFirstSceneToTheGeneratedManagers() {
        makeItVersion2()
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check())
        assertEquals(listOf("$PACKAGE_PATH/Game.kt", MAIN_PATH, "$PACKAGE_PATH/SceneLoader.kt"), upgrade.changes.map { it.path })
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertEquals(newGame, File(packageDirectory, "Game.kt").readText())
        assertFalse(sceneLoaderFile.exists())
        assertContains(settingsFile.readText(), "\"templateVersion\": 5")
    }

    @Test
    fun upgradingAVersion3ProjectLeavesThePluginManagersToTheGeneratedWiring() {
        val version3Game = newGame
            .replace(KUBRIKO_IMPORT_LINE, "$KUBRIKO_IMPORT_LINE\n$COLLISION_IMPORT\n$KUBRIKO_IMPORT")
            .replace(SERIALIZATION_LINE, "$SERIALIZATION_LINE\n            CollisionManager.newInstance(),\n            KeyboardInputManager.newInstance(),")
        File(packageDirectory, "Game.kt").writeText(version3Game)
        mainFile.writeText(OLD_MAIN)
        settingsFile.writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "templateVersion": 3 }""")
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check())
        assertEquals(listOf("$PACKAGE_PATH/Game.kt", MAIN_PATH), upgrade.changes.map { it.path })
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertEquals(newGame, File(packageDirectory, "Game.kt").readText())
        assertContains(settingsFile.readText(), "\"templateVersion\": 5")
    }

    @Test
    fun upgradingAVersion4ProjectLetsPlaySessionsTurnTheLogBlocksOn() {
        mainFile.writeText(OLD_MAIN)
        settingsFile.writeText("""{ "packageName": "com.example.mygame", "sourceDirectory": "src/commonMain/kotlin", "templateVersion": 4 }""")
        assertEquals(listOf(MAIN_PATH), assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check()).changes.map { it.path })
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertEquals(newMain, mainFile.readText())
        assertContains(newMain, "KubrikoWiring.isLoggingEnabled = System.getProperty(\"kubriko.creator.logging\").toBoolean()")
    }

    @Test
    fun aMainThatIsNotTheTemplatesIsLeftToTheUser() {
        assertNull(ProjectUpgrader.withLoggingTurnedOn("fun main() {\n    application { Game() }\n}\n", "import x"))
        assertNull(ProjectUpgrader.withLoggingTurnedOn("fun main() = application {\n    Game()\n}\n\nfun other() = Unit\n", "import x"))
    }

    @Test
    fun pluginManagersCreatedDifferentlyAreLeftToTheUserAndImportsStillInUseStay() {
        val configured = "package a\n\n$KUBRIKO_IMPORT\n\nval k = Kubriko.newInstance(\n    KeyboardInputManager.newInstance(isEnabled = false),\n)"
        assertNull(ProjectUpgrader.withoutPluginManagers(configured))
        val stillUsed = "package a\n\n$KUBRIKO_IMPORT\n\nval k = Kubriko.newInstance(\n    KeyboardInputManager.newInstance(),\n)\nval m = kubriko.get<KeyboardInputManager>()"
        assertEquals("package a\n\n$KUBRIKO_IMPORT\n\nval k = Kubriko.newInstance(\n)\nval m = kubriko.get<KeyboardInputManager>()", ProjectUpgrader.withoutPluginManagers(stillUsed))
    }

    @Test
    fun aSceneLoaderChangedByHandIsKept() {
        makeItVersion2()
        sceneLoaderFile.writeText(oldSceneLoader + "\n// Mine.\n")
        assertEquals(listOf("$PACKAGE_PATH/Game.kt", MAIN_PATH), assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check()).changes.map { it.path })
        assertEquals(TemplateUpgrade.UpToDate, ProjectUpgrader(directory).apply())
        assertContains(sceneLoaderFile.readText(), "// Mine.")
    }

    @Test
    fun filesThatChangedTooMuchAreLeftToTheUser() {
        makeItOld(game = OLD_GAME.replace("$SCENE_LOADER_LINE\n", ""))
        val upgrade = assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).apply())
        assertEquals(listOf("$PACKAGE_PATH/Game.kt"), upgrade.byHand)
        assertEquals(emptyList(), upgrade.changes, "SceneLoader.kt stays while Game.kt may still use it")
        assertEquals(newEntryPoint, File(packageDirectory, "GameEntryPoint.kt").readText())
        assertFalse("templateVersion" in settingsFile.readText())
    }

    @Test
    fun aSceneLoaderOutsideTheScenesFolderOrOnSeveralLinesIsLeftToTheUser() {
        listOf(
            "    SceneLoader(scenePath = \"files/levels/main.json\"),",
            "    SceneLoader(\n        scenePath = \"files/scenes/main.json\",\n    ),",
            "    SceneLoader(scenePath = path),",
        ).forEach { sceneLoader ->
            assertNull(ProjectUpgrader.withScenesLoadedByWiring("fun x() = Kubriko.newInstance(\n$sceneLoader\n)", "import c"), sceneLoader)
        }
    }

    @Test
    fun filesUpgradedByHandCountAsDone() {
        makeItOld()
        File(packageDirectory, "Game.kt").writeText(newGame)
        File(packageDirectory, "GameEntryPoint.kt").delete()
        assertEquals(listOf("$PACKAGE_PATH/GameEntryPoint.kt"), assertIs<TemplateUpgrade.Needed>(ProjectUpgrader(directory).check()).byHand)
        File(packageDirectory, "GameEntryPoint.kt").writeText(newEntryPoint)
        mainFile.writeText(newMain)
        sceneLoaderFile.delete()
        ProjectUpgrader(directory).recordVersionIfUpToDate()
        assertContains(settingsFile.readText(), "\"templateVersion\": 5")
    }

    @Test
    fun anEntryPointThatAlreadyHasCustomManagersIsLeftToTheUser() {
        assertNull(ProjectUpgrader.withManagersHandedToTheSceneEditor(OLD_ENTRY_POINT.replace("    override fun createSerializationManager()", "    override fun createCustomManagers() = listOf(Mine())\n\n    override fun createSerializationManager()"), "import x"))
    }

    @Test
    fun theImportGoesAfterTheLastOneOrUnderThePackageUnlessItIsThere() {
        val newInstance = "fun x() = Kubriko.newInstance(\n    *KubrikoWiring.createManagers(firstScene = \"level-1\").toTypedArray(),\n)"
        val sceneLoader = "fun x() = Kubriko.newInstance(\n    SceneLoader(scenePath = \"files/scenes/level-1.json\"),\n)"
        assertEquals("package a\n\nimport b\nimport c\n\n$newInstance", ProjectUpgrader.withScenesLoadedByWiring("package a\n\nimport b\n\n$sceneLoader", "import c"))
        assertEquals("package a\n\nimport c\n\n$newInstance", ProjectUpgrader.withScenesLoadedByWiring("package a\n\n$sceneLoader", "import c"))
        assertEquals("package a\n\nimport c\n\n$newInstance", ProjectUpgrader.withScenesLoadedByWiring("package a\n\nimport c\n\n$sceneLoader", "import c"))
    }

    companion object {
        private const val PACKAGE_PATH = "src/commonMain/kotlin/com/example/mygame"
        private const val MAIN_PATH = "src/desktopMain/kotlin/com/example/mygame/Main.kt"
        private const val SCENE_LOADER_LINE = "            SceneLoader(scenePath = \"files/scenes/main.json\"),"
        private const val KUBRIKO_IMPORT = "import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager"
        private const val WIRING_IMPORT = "import com.example.mygame.generated.KubrikoWiring"
        private const val KUBRIKO_IMPORT_LINE = "import com.pandulapeter.kubriko.KubrikoViewport"
        private const val COLLISION_IMPORT = "import com.pandulapeter.kubriko.collision.CollisionManager"
        private const val SERIALIZATION_LINE = "            GameSerialization.createSerializationManager(),"
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

        private val OLD_MAIN = """
            |package com.example.mygame
            |
            |import androidx.compose.ui.window.Window
            |import androidx.compose.ui.window.application
            |
            |fun main() = application {
            |    Window(
            |        title = "My Game",
            |        onCloseRequest = ::exitApplication,
            |    ) {
            |        Game()
            |    }
            |}
            |
        """.trimMargin()
    }
}
